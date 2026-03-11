plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    // TODO: id("com.google.dagger.hilt.android") figure out hilt stuff later
    id("com.google.gms.google-services")  // Firebase
}

android {
    namespace = "com.adhithimaran.cultivate"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.adhithimaran.cultivate"
        minSdk = 26
        targetSdk = 36
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // don't need to manually match version numbers with compose libraries
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

//    TODO: Hilt (Dagger Hilt) Figure Out Later
//    implementation("com.google.dagger:hilt-android:2.56")
//    ksp("com.google.dagger:hilt-compiler:2.56")
//    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")  // adds Flow support
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines (likely already included, but just in case)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Coil Compose
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase BOM (controls all Firebase versions from one place)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    // Then add individual Firebase libs WITHOUT version numbers:
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Google Sign in
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}