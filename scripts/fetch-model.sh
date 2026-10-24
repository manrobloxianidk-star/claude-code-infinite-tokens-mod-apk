#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODEL_DIR="$ROOT_DIR/app/src/main/assets/models"
MODEL_PATH="$MODEL_DIR/SmolLM2-135M-Instruct-Q4_K_M.gguf"
TEMP_PATH="$MODEL_PATH.partial"
MODEL_URL="https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/d255afaffd3441b95abca9b5cc4c819b93f66936/SmolLM2-135M-Instruct-Q4_K_M.gguf?download=true"
EXPECTED_BYTES=105454432
EXPECTED_SHA256="2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d"

mkdir -p "$MODEL_DIR"
trap 'rm -f "$TEMP_PATH"' EXIT

curl --fail --location --retry 4 --retry-all-errors --retry-delay 2 \
    --silent --show-error "$MODEL_URL" --output "$TEMP_PATH"

ACTUAL_BYTES="$(stat -c '%s' "$TEMP_PATH")"
if [[ "$ACTUAL_BYTES" != "$EXPECTED_BYTES" ]]; then
    echo "Model size mismatch: expected $EXPECTED_BYTES bytes, got $ACTUAL_BYTES" >&2
    exit 1
fi

ACTUAL_SHA256="$(sha256sum "$TEMP_PATH" | cut -d ' ' -f 1)"
if [[ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]]; then
    echo "Model SHA-256 mismatch: expected $EXPECTED_SHA256, got $ACTUAL_SHA256" >&2
    exit 1
fi

mv "$TEMP_PATH" "$MODEL_PATH"
trap - EXIT
echo "Verified SmolLM2-135M-Instruct-Q4_K_M.gguf ($ACTUAL_BYTES bytes, SHA-256 $ACTUAL_SHA256)"
