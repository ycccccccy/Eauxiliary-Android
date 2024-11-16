plugins {
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false //  apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2") // or latest
        classpath("com.google.gms:google-services:4.4.0") // or latest
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22") // or latest
    }
}