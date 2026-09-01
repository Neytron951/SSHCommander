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

// Overridable via -PappName=MyApp / -PappVersion=1.5.0 (used by build-windows.bat).
val appName: String = (project.findProperty("appName") as? String)?.takeIf { it.isNotBlank() } ?: "SSH Commander"
val appVersion: String = (project.findProperty("appVersion") as? String)?.takeIf { it.isNotBlank() } ?: "1.9.2"

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
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
            
            // Явно добавляем модули, которые jpackage не может определить автоматически
            modules("java.net.http", "java.scripting", "jdk.unsupported")

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                // Per-machine install to Program Files + Start Menu shortcut.
                perUserInstall = false
                menu = true
                menuGroup = appName
                // Stable UpgradeCode ensures that updating the app replaces the old entry
                // instead of creating a "bundled" duplicate in Add/Remove Programs.
                upgradeUuid = "8a9728d1-02e7-4faa-883f-29c2d985f6f8"
            }
        }
    }
}
