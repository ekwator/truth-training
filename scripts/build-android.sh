#!/usr/bin/env bash
set -euo pipefail

if [ -z "$NDK_HOME" ]; then
  echo "Please set NDK_HOME to your Android NDK path"
  exit 1
fi

TARGETS=("aarch64-linux-android" "x86_64-linux-android")

for TARGET in "${TARGETS[@]}"; do
  echo "Building truth_core library for $TARGET..."
  rustup target add "$TARGET" || true
  # Build only the library of the root package to avoid compiling binaries/workspace members
  cargo build -p truth_core --lib --release --target "$TARGET"
done

mkdir -p ./android-libs/arm64-v8a ./android-libs/x86_64
# The library name is derived from [lib] name = "truth_core" -> libtruth_core.so
cp target/aarch64-linux-android/release/libtruth_core.so ./android-libs/arm64-v8a/
cp target/x86_64-linux-android/release/libtruth_core.so ./android-libs/x86_64/

echo "✅ Build complete. Libraries in ./android-libs/"

