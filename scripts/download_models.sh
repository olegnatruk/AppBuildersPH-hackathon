#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p models
fetch() {
  local repo="$1" revision="$2" name="$3"
  if [[ ! -f "models/$name" ]]; then
    curl --fail --location --retry 3 --retry-all-errors --speed-time 60 --speed-limit 1024 --continue-at - "https://huggingface.co/$repo/resolve/$revision/$name" -o "models/$name.part"
    python3 - "models/$name.part" <<'PY'
import sys,os
p=sys.argv[1]
with open(p,'rb') as f:
    assert f.read(4)==b'GGUF', 'Not a GGUF file'
assert os.path.getsize(p)>100_000_000, 'Model is truncated'
PY
    mv "models/$name.part" "models/$name"
  fi
  shasum -a 256 "models/$name"
}
fetch Qwen/Qwen2.5-1.5B-Instruct-GGUF 91cad51170dc346986eccefdc2dd33a9da36ead9 qwen2.5-1.5b-instruct-q4_k_m.gguf
fetch Qwen/Qwen2.5-0.5B-Instruct-GGUF 9217f5db79a29953eb74d5343926648285ec7e67 qwen2.5-0.5b-instruct-q4_k_m.gguf
if [[ "${1:-}" == "--with-gemma" ]]; then
  fetch ggml-org/gemma-3-1b-it-GGUF f9c28bcd85737ffc5aef028638d3341d49869c27 gemma-3-1b-it-Q4_K_M.gguf
fi
