plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "de.coldtea.smplr.alarm"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.coldtea.smplr.alarm"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
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

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(project(":smplralarm"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.test.junit)
    androidTestImplementation(libs.test.androidx.junit)

    // Timber
    implementation(libs.timber)

    // Koin
    implementation(libs.bundles.koin)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.runtime.livedata)

    // Adapter-delegates
    implementation(libs.adapterdelegates.kotlin.dsl)
}
