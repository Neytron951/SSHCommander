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
            packageName = "SSHCommander"
            packageVersion = "1.3.0"
            description = "SSH/SFTP client for Windows"
            vendor = "Neytron"
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}
