plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI passes GITHUB_RUN_NUMBER so every build has a higher versionCode (required to install updates).
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// Optional release key, provided by CI from repository secrets. Without it the APK is debug-signed.
val releaseKeystore: String? = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.rezatabrizii.cryptotracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.rezatabrizii.cryptotracker"
        minSdk = 30
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")

    implementation("androidx.wear.tiles:tiles:1.4.0")
    implementation("androidx.wear.protolayout:protolayout:1.2.0")
    implementation("androidx.wear.protolayout:protolayout-material:1.2.0")
    implementation("androidx.concurrent:concurrent-futures:1.2.0")

    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json is a stub in local unit tests; use the real implementation.
    testImplementation("org.json:json:20240303")
}
