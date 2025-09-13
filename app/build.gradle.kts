plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hophacks2025app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.hophacks2025app"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        mlModelBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(project(":OpenCV"))
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    //try fix from joy 5:14p
    //implementation("org.tensorflow:tensorflow-lite:2.9.0")
    //implementation("org.tensorflow:tensorflow-lite-task-vision:0.3.1")
    //implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")
}