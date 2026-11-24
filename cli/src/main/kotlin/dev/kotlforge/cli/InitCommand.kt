package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import java.io.IOException

class InitCommand : CliktCommand(
    name = "init",
    help = "Scaffold a new minimal Android project"
) {
    private val projectName by option("--name", "-n", help = "Project name").required()
    private val packageName by option(
        "--package", "-pkg",
        help = "Application package name (default: com.example.<name>)"
    )
    private val minSdk by option("--min-sdk", help = "Minimum Android SDK version").int().default(DEFAULT_MIN_SDK)
    private val outputDir by option("--output", "-o", help = "Directory to create the project in").default(".")

    companion object {
        internal const val DEFAULT_MIN_SDK = 24
        internal const val DEFAULT_COMPILE_SDK = 35
        internal const val GRADLE_VERSION = "8.11"
        internal const val AGP_VERSION = "8.7.0"
        internal const val KOTLIN_VERSION = "2.0.21"
    }

    override fun run() {
        val resolvedPackage = packageName ?: "com.example.${projectName.lowercase().replace(Regex("[^a-z0-9]"), "")}"
        val projectDir = File(outputDir, projectName)

        if (projectDir.exists()) {
            echo("Error: directory already exists: ${projectDir.absolutePath}", err = true)
            throw ProgramResult(ExitCode.BAD_ARGS)
        }

        try {
            scaffoldProject(projectDir, resolvedPackage)
        } catch (e: IOException) {
            echo("Error: could not create project — ${e.message}", err = true)
            throw ProgramResult(ExitCode.BUILD_FAILED)
        }

        echo("Project '$projectName' created at: ${projectDir.absolutePath}")
        echo("  Package : $resolvedPackage")
        echo("  Min SDK : $minSdk")
        echo("")
        echo("Next steps:")
        echo("  cd ${projectDir.name}")
        echo("  gradle wrapper --gradle-version $GRADLE_VERSION")
        echo("  kotlforge build --project .")
    }

    private fun scaffoldProject(projectDir: File, pkg: String) {
        writeRootFiles(projectDir)
        writeGradleFiles(projectDir)
        writeAppModule(projectDir, pkg)
        writeFile(
            projectDir, ".gitignore",
            ".gradle/\nbuild/\nlocal.properties\n*.iml\n.idea/"
        )
    }

    private fun writeRootFiles(projectDir: File) {
        writeFile(
            projectDir, "settings.gradle.kts",
            """
            pluginManagement {
                repositories { google(); mavenCentral(); gradlePluginPortal() }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories { google(); mavenCentral() }
            }
            rootProject.name = "$projectName"
            include(":app")
            """.trimIndent()
        )
        writeFile(
            projectDir, "build.gradle.kts",
            "plugins {\n" +
                "    alias(libs.plugins.android.application) apply false\n" +
                "    alias(libs.plugins.kotlin.android) apply false\n" +
                "}"
        )
    }

    private fun writeGradleFiles(projectDir: File) {
        writeFile(
            projectDir, "gradle/libs.versions.toml",
            "[versions]\nagp = \"$AGP_VERSION\"\nkotlin = \"$KOTLIN_VERSION\"\n\n" +
                "[plugins]\nandroid-application = { id = \"com.android.application\", version.ref = \"agp\" }\n" +
                "kotlin-android = { id = \"org.jetbrains.kotlin.android\", version.ref = \"kotlin\" }"
        )
        writeFile(
            projectDir, "gradle/wrapper/gradle-wrapper.properties",
            "distributionBase=GRADLE_USER_HOME\ndistributionPath=wrapper/dists\n" +
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip\n" +
                "zipStoreBase=GRADLE_USER_HOME\nzipStorePath=wrapper/dists"
        )
    }

    private fun writeAppModule(projectDir: File, pkg: String) {
        writeFile(
            projectDir, "app/build.gradle.kts",
            """
            plugins {
                alias(libs.plugins.android.application)
                alias(libs.plugins.kotlin.android)
            }
            android {
                namespace = "$pkg"
                compileSdk = $DEFAULT_COMPILE_SDK
                defaultConfig {
                    applicationId = "$pkg"
                    minSdk = $minSdk
                    targetSdk = $DEFAULT_COMPILE_SDK
                    versionCode = 1
                    versionName = "1.0"
                }
                buildTypes { release { isMinifyEnabled = false } }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlinOptions { jvmTarget = "17" }
            }
            """.trimIndent()
        )
        val packagePath = pkg.replace('.', '/')
        writeFile(
            projectDir, "app/src/main/AndroidManifest.xml",
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:label="$projectName" android:theme="@style/Theme.AppCompat">
                    <activity android:name=".MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        )
        writeFile(
            projectDir, "app/src/main/kotlin/$packagePath/MainActivity.kt",
            "package $pkg\n\nimport android.app.Activity\nimport android.os.Bundle\n\n" +
                "class MainActivity : Activity() {\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n" +
                "    }\n" +
                "}"
        )
    }

    private fun writeFile(projectDir: File, relativePath: String, content: String) {
        val file = File(projectDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}
