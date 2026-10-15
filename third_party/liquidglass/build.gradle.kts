plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.liquidglass"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    compileOnly("androidx.appcompat:appcompat:1.7.0")
    compileOnly("com.google.android.material:material:1.12.0")
    compileOnly("androidx.viewpager2:viewpager2:1.1.0")
}
