package dev.kotlforge.watch

enum class DeployMode {
    /** Standard adb install -r on every change (default) */
    STANDARD,
    /** adb install --incremental — streams only changed APK blocks, falls back on Android < 11 */
    INCREMENTAL,
    /** JVMTI hot-swap via SwapAgent injected at watch start — no app restart, state preserved */
    LIGHTNING
}
