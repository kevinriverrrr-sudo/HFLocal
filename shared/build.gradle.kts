plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sql.delight)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.sql.delight.runtime)
            implementation(libs.sql.delight.coroutines)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.compose.core)
            implementation(libs.coil.network.ktor)
        }
        getByName("androidMain").dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sql.delight.android)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }
        getByName("desktopMain").dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sql.delight.jvm)
        }
    }
}

android {
    namespace = "com.hflocal.shared"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("HFLocalDatabase") {
            packageName.set("com.hflocal.shared.data.local.db")
        }
    }
}
