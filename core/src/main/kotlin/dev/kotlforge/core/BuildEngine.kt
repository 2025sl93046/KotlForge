package dev.kotlforge.core

interface BuildEngine {
    fun build(request: BuildRequest): BuildResult
}
