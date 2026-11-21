package dev.kotlforge.watch

import dev.kotlforge.core.BuildResult
import java.nio.file.Path

data class WatchCycleResult(
    val cycle: Int,
    val changedFiles: List<Path>,
    val buildResult: BuildResult,
    val deployedDevice: String?,
    val durationMillis: Long
)
