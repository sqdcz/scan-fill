plugins {
    id("com.android.application")
}

android {
    namespace = "com.scanfill.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.scanfill.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            // R8 混淆会破坏 ML Kit / Paddle 内部反射与 JNI 映射，
            // 曾导致 "p5.h p5.g.a" NPE（识别必失败），个人分发无需混淆
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // 引擎一：ML Kit（离线，中英文）
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    // 引擎二：PaddleOCR（离线，PP-OCRv4）
    implementation("com.github.equationl.paddleocr4android:paddleocr4android:v1.2.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
