plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.hoangquanghuy.papervault"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hoangquanghuy.papervault"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    val roomVersion = "2.6.1"
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.biometric:biometric:1.1.0")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
}