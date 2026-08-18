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

// Overridable via -PappName=MyApp / -PappVersion=1.4.0 (used by build-windows.bat).
val appName: String = (project.findProperty("appName") as? String)?.takeIf { it.isNotBlank() } ?: "SSHCommander"
val appVersion: String = (project.findProperty("appVersion") as? String)?.takeIf { it.isNotBlank() } ?: "1.4.0"

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.neytron.sshcommander.MainKt"
        javaHome = jpackageJdk
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = appName
            packageVersion = appVersion
            description = "SSH/SFTP client for Windows"
            vendor = "Neytron"
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                // Per-machine install to Program Files + Start Menu shortcut so
                // Windows Search / Start can find the app after installation.
                perUserInstall = false
                menu = true
                menuGroup = appName
            }
        }
    }
}
