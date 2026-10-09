#include <jni.h>
#include <llama.h>
#include <atomic>
#include <chrono>
#include <codecvt>
#include <locale>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>
#include <stdexcept>

using Clock = std::chrono::steady_clock;
struct Engine {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    std::atomic<bool> cancelled{false};
    Clock::time_point deadline;
    ~Engine() { if (ctx) llama_free(ctx); if (model) llama_model_free(model); }
};
static std::once_flag initialized;
static void silent_log(ggml_log_level, const char *, void *) {}
static void init() {
    std::call_once(initialized, [] { llama_log_set(silent_log, nullptr); ggml_log_set(silent_log, nullptr); llama_backend_init(); });
}
// JNI's modified UTF-8 cannot represent emoji correctly. Convert real UTF-16/UTF-8.
static std::string from_java(JNIEnv * env, jstring value) {
    if (!value) return {};
    const jchar * chars = env->GetStringChars(value, nullptr);
    if (!chars) throw std::runtime_error("String allocation failed");
    std::u16string u(reinterpret_cast<const char16_t *>(chars), env->GetStringLength(value));
    env->ReleaseStringChars(value, chars);
    return std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>{}.to_bytes(u);
}
static jstring to_java(JNIEnv * env, const std::string & text) {
    try {
        auto u = std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>{}.from_bytes(text);
        return env->NewString(reinterpret_cast<const jchar *>(u.data()), static_cast<jsize>(u.size()));
    } catch (...) { return env->NewStringUTF("{\"text\":\"\",\"stop\":\"error\",\"error\":\"Invalid UTF-8 output\"}"); }
}
static std::string escaped(const std::string & s) {
    std::string result = "\"";
    const char hex[] = "0123456789abcdef";
    for (unsigned char c : s) {
        if (c == '"' || c == '\\') { result += '\\'; result += c; }
        else if (c < 32) { result += "\\u00"; result += hex[c >> 4]; result += hex[c & 15]; }
        else result += c;
    }
    return result + "\"";
}
static bool abort_decode(void * data) {
    auto * e = static_cast<Engine *>(data);
    return e->cancelled.load() || Clock::now() >= e->deadline;
}
static double ms_since(Clock::time_point start) { return std::chrono::duration<double, std::milli>(Clock::now() - start).count(); }
static std::string envelope(const std::string & text, int prompt, int generated, double prefill, double gen, const std::string & stop, const std::string & error = "") {
    std::ostringstream out;
    out << "{\"text\":" << escaped(text) << ",\"promptTokens\":" << prompt << ",\"genTokens\":" << generated
        << ",\"prefillMs\":" << prefill << ",\"genMs\":" << gen << ",\"stop\":" << escaped(stop) << ",\"error\":" << escaped(error) << "}";
    return out.str();
}
extern "C" JNIEXPORT void JNICALL Java_ph_salagtrace_llm_LlamaNative_nativeInit(JNIEnv *, jobject) { try { init(); } catch (...) {} }
extern "C" JNIEXPORT jlong JNICALL Java_ph_salagtrace_llm_LlamaNative_nativeLoad(JNIEnv * env, jobject, jstring path, jint n_ctx, jint threads) {
    try {
        init();
        auto e = std::make_unique<Engine>();
        auto params = llama_model_default_params();
        params.load_mode = LLAMA_LOAD_MODE_MMAP; params.n_gpu_layers = 0;
        e->model = llama_model_load_from_file(from_java(env, path).c_str(), params);
        if (!e->model) return 0;
        auto cp = llama_context_default_params();
        cp.n_ctx = n_ctx; cp.n_batch = 512; cp.n_ubatch = 128;
        cp.n_threads = threads; cp.n_threads_batch = threads;
        cp.no_perf = false;
        e->ctx = llama_init_from_model(e->model, cp);
        if (!e->ctx) return 0;
        llama_set_abort_callback(e->ctx, abort_decode, e.get());
        return reinterpret_cast<jlong>(e.release());
    } catch (...) { return 0; }
}
extern "C" JNIEXPORT jstring JNICALL Java_ph_salagtrace_llm_LlamaNative_nativeComplete(JNIEnv * env, jobject, jlong handle, jstring system, jstring user, jstring grammar, jint max_tokens, jfloat temperature, jint seed, jlong timeout_ms) {
    std::string output, stop = "max";
    int prompt_count = 0, generated = 0;
    double prefill_ms = 0, gen_ms = 0;
    try {
        auto * e = reinterpret_cast<Engine *>(handle);
        if (!e || !e->ctx) throw std::runtime_error("Model is not loaded");
        e->cancelled.store(false);
        e->deadline = Clock::now() + std::chrono::milliseconds(timeout_ms);
        llama_memory_clear(llama_get_memory(e->ctx), true);
        const auto * vocab = llama_model_get_vocab(e->model);
        std::string sys = from_java(env, system), usr = from_java(env, user);
        llama_chat_message chat[] = {{"system", sys.c_str()}, {"user", usr.c_str()}};
        const char * tmpl = llama_model_chat_template(e->model, nullptr);
        std::vector<char> formatted(2 * (sys.size() + usr.size()) + 1024);
        int n = llama_chat_apply_template(tmpl, chat, 2, true, formatted.data(), formatted.size());
        if (n < 0) {
            usr = sys + "\n\n" + usr;
            llama_chat_message single[] = {{"user", usr.c_str()}};
            n = llama_chat_apply_template(tmpl, single, 1, true, formatted.data(), formatted.size());
            if (n < 0) throw std::runtime_error("Unsupported model chat template");
            if (n > static_cast<int>(formatted.size())) {
                formatted.resize(n + 1);
                n = llama_chat_apply_template(tmpl, single, 1, true, formatted.data(), formatted.size());
            }
        } else if (n > static_cast<int>(formatted.size())) {
            formatted.resize(n + 1);
            n = llama_chat_apply_template(tmpl, chat, 2, true, formatted.data(), formatted.size());
        }
        int count = -llama_tokenize(vocab, formatted.data(), n, nullptr, 0, true, true);
        if (count <= 0 || count + max_tokens >= static_cast<int>(llama_n_ctx(e->ctx))) throw std::runtime_error("Message exceeds model context");
        std::vector<llama_token> tokens(count);
        prompt_count = llama_tokenize(vocab, formatted.data(), n, tokens.data(), count, true, true);
        if (prompt_count <= 0) throw std::runtime_error("Tokenization failed");
        const auto prefill_start = Clock::now();
        for (int offset = 0; offset < prompt_count; offset += 512) {
            int size = std::min(512, prompt_count - offset);
            if (llama_decode(e->ctx, llama_batch_get_one(tokens.data() + offset, size)) != 0) {
                stop = e->cancelled.load() ? "cancel" : (Clock::now() >= e->deadline ? "timeout" : "error");
                return to_java(env, envelope("", prompt_count, 0, ms_since(prefill_start), 0, stop, stop == "error" ? "Prefill failed" : ""));
            }
        }
        prefill_ms = ms_since(prefill_start);
        auto * chain = llama_sampler_chain_init(llama_sampler_chain_default_params());
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(chain, llama_sampler_free);
        std::string gbnf = from_java(env, grammar);
        auto * grammar_sampler = llama_sampler_init_grammar(vocab, gbnf.c_str(), "root");
        if (!grammar_sampler) throw std::runtime_error("Invalid output grammar");
        llama_sampler_chain_add(chain, grammar_sampler);
        llama_sampler_chain_add(chain, llama_sampler_init_top_k(40));
        llama_sampler_chain_add(chain, llama_sampler_init_top_p(0.9f, 1));
        llama_sampler_chain_add(chain, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(chain, llama_sampler_init_dist(static_cast<uint32_t>(seed)));
        const auto gen_start = Clock::now();
        for (int i = 0; i < max_tokens; ++i) {
            if (abort_decode(e)) { stop = e->cancelled.load() ? "cancel" : "timeout"; break; }
            llama_token token = llama_sampler_sample(chain, e->ctx, -1);
            if (llama_vocab_is_eog(vocab, token)) { stop = "eos"; break; }
            std::vector<char> piece(256);
            int size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false);
            if (size < 0) { piece.resize(-size); size = llama_token_to_piece(vocab, token, piece.data(), piece.size(), 0, false); }
            if (size < 0) throw std::runtime_error("Token conversion failed");
            output.append(piece.data(), size); ++generated;
            if (i + 1 < max_tokens && llama_decode(e->ctx, llama_batch_get_one(&token, 1)) != 0) {
                stop = e->cancelled.load() ? "cancel" : (Clock::now() >= e->deadline ? "timeout" : "error"); break;
            }
        }
        gen_ms = ms_since(gen_start);
        return to_java(env, envelope(output, prompt_count, generated, prefill_ms, gen_ms, stop, stop == "error" ? "Generation failed" : ""));
    } catch (const std::exception &) { return to_java(env, envelope("", prompt_count, generated, prefill_ms, gen_ms, "error", "Native inference failed")); }
    catch (...) { return to_java(env, envelope("", prompt_count, generated, prefill_ms, gen_ms, "error", "Native inference failed")); }
}
extern "C" JNIEXPORT void JNICALL Java_ph_salagtrace_llm_LlamaNative_nativeCancel(JNIEnv *, jobject, jlong h) { if (h) reinterpret_cast<Engine *>(h)->cancelled.store(true); }
extern "C" JNIEXPORT void JNICALL Java_ph_salagtrace_llm_LlamaNative_nativeFree(JNIEnv *, jobject, jlong h) { try { delete reinterpret_cast<Engine *>(h); } catch (...) {} }
extern "C" JNIEXPORT jstring JNICALL Java_ph_salagtrace_llm_LlamaNative_nativeModelInfo(JNIEnv * env, jobject, jlong h) {
    try {
        auto * e = reinterpret_cast<Engine *>(h);
        if (!e) return to_java(env, "{}");
        char desc[512] = {};
        llama_model_desc(e->model, desc, sizeof(desc));
        return to_java(env, "{\"description\":" + escaped(desc) + ",\"parameters\":" + std::to_string(llama_model_n_params(e->model)) + ",\"bytes\":" + std::to_string(llama_model_size(e->model)) + ",\"chatTemplate\":" + (llama_model_chat_template(e->model, nullptr) ? "true" : "false") + "}");
    } catch (...) { return to_java(env, "{}"); }
}
