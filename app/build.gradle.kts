plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "adu.nttu.englishai"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "adu.nttu.englishai"

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
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets")
            }
        }
    }
}
dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    implementation("com.google.code.gson:gson:2.11.0")

    implementation("androidx.recyclerview:recyclerview:1.4.0")

    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Firebase AI Logic
    implementation("com.google.firebase:firebase-ai")

    // App Check cho lúc chạy máy ảo/debug
    implementation("com.google.firebase:firebase-appcheck-debug")

    // Firebase AI Logic Java cần Guava
    implementation("com.google.guava:guava:31.0.1-android")

    // Dùng cho phản hồi dạng streaming
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}