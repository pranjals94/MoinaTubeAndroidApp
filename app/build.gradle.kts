plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.test.moinatube"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.test.moinatube"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
            release {
                isMinifyEnabled = true // shrinks code (with ProGuard or R8). This property enables code shrinking, which removes unused code, classes, methods, and fields from your APK.
                isShrinkResources = true // This property removes unused resources (such as images, XML files, etc.) from your APK during the build process. It works in conjunction with isMinifyEnabled and helps reduce the final size of the APK by eliminating unnecessary resources that aren't being used in the app
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


    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {

    // At top of dependencies:
    implementation (platform("androidx.compose:compose-bom:2024.05.00"))

    implementation ("androidx.core:core-ktx:1.12.0")

    implementation ("com.google.android.material:material:1.11.0")

    implementation( "androidx.activity:activity-compose:1.8.2")
    implementation ("androidx.compose.material3:material3")

    implementation ("androidx.tv:tv-foundation:1.0.0-alpha12")
    implementation ("androidx.tv:tv-material:1.0.0-alpha10")
    implementation ("io.coil-kt:coil-compose:2.5.0") // For loading images

    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

}