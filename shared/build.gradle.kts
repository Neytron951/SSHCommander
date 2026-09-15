import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}


val properties = Properties()
val propertiesFile = project.rootProject.file("local.properties")
if (propertiesFile.exists()) {
    propertiesFile.inputStream().use { properties.load(it) }
}
val googleClientId = properties.getProperty("GOOGLE_CLIENT_ID") ?: ""
val googleClientSecret = properties.getProperty("GOOGLE_CLIENT_SECRET") ?: ""

kotlin {
    jvm("desktop") {
        compilerOptions {
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }

    // Register Android target for Kotlin Multiplatform (legacy androidTarget API)
    androidTarget()

    compilerOptions {
        // Disable K2 to avoid crashes with incompatible library metadata
        // useK2.set(false) // This is for older versions. In 2.0.x it's different
    }

    sourceSets {
        val commonMain = getByName("commonMain")
        commonMain.apply {
            // Создаем папку для сгенерированных файлов
            val generatedDir = layout.buildDirectory.dir("generated/secrets/commonMain/kotlin").get().asFile
            kotlin.srcDir(generatedDir)

            val generateSecretsTask = tasks.register("generateSecrets") {
                val outputFile = File(generatedDir, "com/neytron/sshcommander/Secrets.kt")
                inputs.property("googleClientId", googleClientId)
                inputs.property("googleClientSecret", googleClientSecret)
                outputs.file(outputFile)

                doLast {
                    outputFile.parentFile.mkdirs()
                    outputFile.writeText("""
                        package com.neytron.sshcommander

                        object Secrets {
                            const val GOOGLE_CLIENT_ID = "$googleClientId"
                            const val GOOGLE_CLIENT_SECRET = "$googleClientSecret"
                        }
                    """.trimIndent())
                }
            }

            tasks.matching {
                it.name.contains("compile", ignoreCase = true) ||
                it.name.contains("sourcesJar", ignoreCase = true) ||
                it.name.contains("metadata", ignoreCase = true)
            }.configureEach {
                dependsOn(generateSecretsTask)
            }

            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(libs.compose.lifecycle.viewmodel)
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)

                api(libs.ktor.client.core)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.ktor.client.logging)
                implementation(libs.zxing.core)
                implementation(libs.libadb)
            }
        }

        val androidMain = getByName("androidMain")
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.yandex.mobileads)
            implementation(libs.ktor.client.android)
            implementation(libs.play.services.auth)
            implementation(libs.libadb)
        }

        val desktopMain = getByName("desktopMain")
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(libs.ktor.client.java)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
        }

        val jvmMain = if (findByName("jvmMain") != null) getByName("jvmMain") else create("jvmMain")
        jvmMain.apply {
            dependsOn(commonMain)
            dependencies {
                api(libs.jsch)
                api(libs.bcprov)
                api(libs.dadb)
                implementation(libs.gson)
                implementation(libs.jmdns)
                implementation(libs.conscrypt)
                implementation("com.google.zxing:core:3.5.3")
            }
        }

        val desktopTest = getByName("desktopTest")
        desktopTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependsOn(jvmMain)
        desktopMain.dependsOn(jvmMain)
    }
}

// Configure Kotlin JVM compilation options for all Kotlin JVM compilations
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
    namespace = "com.neytron.sshcommander.shared"
    compileSdk = 37
    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
