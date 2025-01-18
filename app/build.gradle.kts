import java.text.SimpleDateFormat
import java.util.Date


plugins {
    id("com.android.application")
    id("com.github.ben-manes.versions") version "0.48.0" // 最新版本
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" //  或者你的 Kotlin 版本

    kotlin("android") version "2.0.0"
}

android {
    signingConfigs {
        create("yc") {
            storeFile = file("C:\\Users\\yc\\Downloads\\sign.keystore")
            storePassword = "ycccccccy"
            keyAlias = "yc"
            keyPassword = "ycccccccy"
        }
    }

    namespace = "com.yc.eauxiliary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yc.eauxiliary"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "4.2.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val buildTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        resValue("string", "build_time", buildTime)
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.1"))
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2") // ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2") // LiveData
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // Lifecycle
    implementation("androidx.room:room-runtime:2.5.2") // Room 数据库
    implementation("androidx.room:room-ktx:2.5.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-analytics:21.6.1")
    implementation("androidx.compose.ui:ui-graphics-android:1.6.8")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.firebase:firebase-crashlytics-buildtools:3.0.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
