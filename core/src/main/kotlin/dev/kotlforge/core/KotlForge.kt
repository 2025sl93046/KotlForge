package dev.kotlforge.core

class KotlForge(
    private val engine: BuildEngine,
    private val configLoader: ConfigLoader = ConfigLoader
) {

    fun build(request: BuildRequest): BuildResult {
        val resolved = applyConfigDefaults(request)
        val errors = validate(resolved)
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
        return engine.build(resolved)
    }

    private fun applyConfigDefaults(request: BuildRequest): BuildRequest {
        val root = ProjectRootFinder.find(request.resolvedProjectDir()) ?: return request
        val config = configLoader.load(root)
        return request.copy(
            variant = if (request.variant == "debug") config.defaultVariant else request.variant,
            tasks = request.tasks.ifEmpty { config.defaultTasks }
        )
    }

    private fun validate(request: BuildRequest): List<String> {
        val errors = mutableListOf<String>()
        if (request.projectDir.isBlank()) errors.add("projectDir must not be blank")
        if (request.variant.isBlank()) errors.add("variant must not be blank")
        return errors
    }
}
