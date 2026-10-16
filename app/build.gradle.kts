plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.unisphere"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.unisphere"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
dd                ?: error("KEYSTORE_PATH non configurato")

            storeFile = file(keystorePath)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: error("KEYSTORE_PASSWORD non configurato")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: error("KEY_ALIAS non configurato")
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: error("KEY_PASSWORD non configurato")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.play.services.location)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.compose.v260) // Verifica che questa lib esista nel tuo libs.versions.toml

        implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
        implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation(libs.kotlinx.coroutines.play.services)

    // OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.20")


    // --- KTOR 3.0.0 (Unificato e ripulito) ---
    val ktorVersion = "3.0.0" // Versione più recente
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion") // <--- QUESTO RISOLVE L'ERRORE AUTH
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion") // Usa la stessa versione che hai per gli altri pacchetti ktor

    // --- HILT ---
    val hiltVersion = "2.55"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- DATASTORE & SECURITY ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}