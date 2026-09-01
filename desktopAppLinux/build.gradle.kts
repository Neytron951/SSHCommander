import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// JBR from Android Studio has no jpackage, so point packaging to a full JDK 21.
val jpackageJdk: String = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
    vendor.set(JvmVendorSpec.ADOPTIUM)
}.get().metadata.installationPath.asFile.absolutePath

val appName: String = (project.findProperty("appName") as? String)?.takeIf { it.isNotBlank() } ?: "SSHCommander"
val appVersion: String = (project.findProperty("appVersion") as? String)?.takeIf { it.isNotBlank() } ?: "1.9.2"

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.neytron.sshcommander.MainKt"
        javaHome = jpackageJdk
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = appName
            packageVersion = appVersion
            description = "SSH/SFTP client for Linux"
            vendor = "Neytron"

            // Явно добавляем модули, которые jpackage не может определить автоматически
            modules("java.net.http", "java.scripting", "jdk.unsupported")

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                shortcut = true
                packageName = appName.lowercase()
                menuGroup = "Network"
            }
        }
    }
}
