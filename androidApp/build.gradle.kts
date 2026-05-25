plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "com.hflocal.android"; compileSdk = 35
    defaultConfig { applicationId = "com.hflocal.android"; minSdk = 28; targetSdk = 35; versionCode = 1; versionName = "1.0.0" }
    signingConfigs { create("release") { storeFile = file("${rootProject.projectDir}/mobileai-key.jks"); storePassword = "mobileai123"; keyAlias = "mobileai"; keyPassword = "mobileai123" } }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true; signingConfig = signingConfigs.getByName("release"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.android); implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx); implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose); implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3); implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose); implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto); implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.android); implementation(libs.koin.androidx.compose)
}
