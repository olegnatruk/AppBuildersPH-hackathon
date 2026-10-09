package ph.salagtrace.llm

object LlamaNative {
    init { System.loadLibrary("salag_llm") }
    external fun nativeInit()
    external fun nativeLoad(path: String, nCtx: Int, nThreads: Int): Long
    external fun nativeComplete(handle: Long, systemPrompt: String, userPrompt: String, grammar: String, maxTokens: Int, temperature: Float, seed: Int, timeoutMs: Long): String
    external fun nativeCancel(handle: Long)
    external fun nativeFree(handle: Long)
    external fun nativeModelInfo(handle: Long): String
}
