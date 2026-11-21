package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) =
    KotlForge()
        .subcommands(DoctorCommand(), BuildCommand(), DeployCommand(), WatchCommand())
        .main(args)

class KotlForge : CliktCommand(
    name = "kotlforge",
    help = "KotlForge — Kotlin-powered Android build tool"
) {
    override fun run() = Unit
}
