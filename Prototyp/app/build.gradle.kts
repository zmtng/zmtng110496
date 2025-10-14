plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.example.prototyp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.prototyp"
        minSdk = 24
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
        // Nutze JDK 11 in den Projekteinstellungen (File > Project Structure > SDK Location)
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.play.services.drive)
    kapt(libs.androidx.room.compiler)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.jsoup:jsoup:1.17.2")

    implementation("androidx.fragment:fragment-ktx:1.7.0")

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    // Material3/Compat je nach Setup:
    implementation("com.google.android.material:material:1.12.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit für Netzwerk-Anfragen
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

// Moshi für JSON-Verarbeitung (Parsing)
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
}
