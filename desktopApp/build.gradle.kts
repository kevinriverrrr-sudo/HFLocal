import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.sql.delight.jvm)
                implementation(libs.coil.compose)
                implementation(libs.coil.compose.core)
                implementation(libs.coil.network.ktor)
            }
        }
    }
}

// For native packaging (Linux/Windows)
compose.desktop {
    application {
        mainClass = "com.hflocal.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.AppImage
            )
            packageName = "HFLocal"
            packageVersion = "1.0.0"
            description = "HF Local - Run AI models locally"
            vendor = "HFLocal"

            linux {
                menuGroup = "Development"
                rpmLicenseType = "Apache-2.0"
            }

            windows {
                menuGroup = "Development"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            }
        }

        buildTypes.release {
            proguard {
                isEnabled.set(false)
            }
        }
    }
}
