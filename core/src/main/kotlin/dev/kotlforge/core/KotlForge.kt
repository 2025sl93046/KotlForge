package dev.kotlforge.core

class KotlForge(private val engine: BuildEngine) {

    fun build(request: BuildRequest): BuildResult {
        val errors = validate(request)
        if (errors.isNotEmpty()) {
            return BuildResult(
                success = false,
                exitCode = 2,
                durationMillis = 0,
                diagnostics = errors.map {
                    Diagnostic(DiagnosticLevel.ERROR, it, "Fix the argument and try again")
                }
            )
        }
        return engine.build(request)
    }

    private fun validate(request: BuildRequest): List<String> {
        val errors = mutableListOf<String>()
        if (request.projectDir.isBlank()) errors.add("projectDir must not be blank")
        if (request.variant.isBlank()) errors.add("variant must not be blank")
        return errors
    }
}
