# Building Truth Core for Android

## Prerequisites
- Rust + cargo
- Android NDK (r25+)
- Set `NDK_HOME` in your shell environment

## Build steps
```bash
chmod +x scripts/build-android.sh
./scripts/build-android.sh
```

Output .so files will appear in:

android-libs/arm64-v8a/libtruthcore.so
android-libs/x86_64/libtruthcore.so

These can be copied into the Android client's:

truth-android-client/app/src/main/jniLibs/

