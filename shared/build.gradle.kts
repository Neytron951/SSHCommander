import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Генерируем объект Secrets с ключами из local.properties
val properties = Properties()
val propertiesFile = project.rootProject.file("local.properties")
if (propertiesFile.exists()) {
    propertiesFile.inputStream().use { properties.load(it) }
}
val googleClientId = properties.getProperty("GOOGLE_CLIENT_ID") ?: ""
val googleClientSecret = properties.getProperty("GOOGLE_CLIENT_SECRET") ?: ""

kotlin {
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            // Создаем файл с константами "на лету"
            val generatedDir = File(project.buildDir, "generated/secrets/commonMain/kotlin")
            kotlin.srcDir(generatedDir)
            
            val task = tasks.register("generateSecrets") {
                val outputFile = File(generatedDir, "com/neytron/sshcommander/Secrets.kt")
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
            // Заставляем Kotlin ждать генерации файла
            tasks.matching { it.name.startsWith("compile") }.configureEach {
                dependsOn(task)
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
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.yandex.mobileads)
                implementation(libs.ktor.client.android)
                implementation(libs.play.services.auth)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.jna)
                implementation(libs.jna.platform)
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
            }
        }
        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(libs.jsch)
                api(libs.bcprov)
                implementation(libs.gson)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        androidMain.dependsOn(jvmMain)
        desktopMain.dependsOn(jvmMain)
    }
}

android {
    namespace = "com.neytron.sshcommander.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
