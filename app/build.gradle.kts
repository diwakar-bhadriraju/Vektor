plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Removed: alias(libs.plugins.kotlin.compose) // This plugin is for Jetpack Compose
}

android {
    namespace = "com.vektor.offgrid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vektor.offgrid"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    // Removed: buildFeatures { // This block is for Jetpack Compose
    // Removed:     compose = true
    // Removed: }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.appcompat) // Keep for AppCompat compatibility
    implementation(libs.androidx.constraintlayout) // Keep for ConstraintLayout

    // This is the correct and sufficient Material Components for Views library
    implementation(libs.material) // This resolves to com.google.android.material:material:1.12.0 toml

    implementation(libs.play.services.location) // Keep for location services
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jetbrains.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}