package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import kotlin.system.exitProcess

// Exit codes: 0=success, 1=build failed, 2=bad args, 3=env failed
fun main(args: Array<String>) {
    val app = KotlForge().subcommands(DoctorCommand(), BuildCommand(), InspectCommand(), CleanCommand(), InitCommand(), DeployCommand(), WatchCommand())
    try {
        app.parse(args)
    } catch (e: ProgramResult) {
        exitProcess(e.statusCode)
    } catch (e: PrintHelpMessage) {
        println(e.context?.command?.getFormattedHelp(e) ?: "")
        exitProcess(if (e.error) ExitCode.BAD_ARGS else ExitCode.SUCCESS)
    } catch (e: UsageError) {
        System.err.println(e.message ?: "Invalid usage. Run with --help for usage information.")
        exitProcess(ExitCode.BAD_ARGS)
    } catch (e: CliktError) {
        val output = e.message ?: ""
        if (output.isNotEmpty()) {
            if (e.printError) System.err.println(output) else println(output)
        }
        exitProcess(e.statusCode)
    }
}

class KotlForge : CliktCommand(
    name = "kotlforge",
    help = "KotlForge — Kotlin-powered Android build tool"
) {
    override fun run() = Unit
}
