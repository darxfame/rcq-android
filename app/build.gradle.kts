plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.rcq.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.rcq.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)

    // QR generation for the "my code" sheet (rcq://add/<uin>). Pure-Java
    // BitMatrix → Bitmap; no UI dependency.
    implementation(libs.zxing.core)

    // Networking (HTTP + WebSocket) and JSON — used by the API + WS
    // layers added in the next milestones.
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // X25519 + Ed25519 keygen via the BouncyCastle lightweight API.
    // JCA's "XDH"/"Ed25519" KeyPairGenerators only exist on API 33+,
    // and minSdk is 26 — BC's generators work everywhere and give raw
    // 32-byte key access, which is exactly the wire format the backend
    // expects (base64 of raw public keys, per rcq-spec 2.2).
    implementation(libs.bouncycastle)

    // Encrypted-at-rest storage for the per-account identity (UIN, JWT,
    // private keys) — the Android equivalent of the iOS Keychain.
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.security.crypto)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
