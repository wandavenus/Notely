plugins {
    alias(libs.plugins.android.application)
    // AGP 9 sudah menyertakan dukungan Kotlin (built-in); plugin
    // org.jetbrains.kotlin.android tidak lagi dipakai.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.wndavenz.notely"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "dev.wndavenz.notely"
        minSdk = 29
        targetSdk = 37
        versionCode = 15
        versionName = "1.3.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // APK dipisah per ABI dan hanya arm64-v8a yang dihasilkan: ukuran unduh
    // per perangkat jauh lebih kecil.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    // Keystore rilis dibaca dari environment (di CI berasal dari GitHub
    // Secrets). Bila tidak ada, build rilis jatuh ke kunci debug agar build
    // lokal tetap bisa dipasang.
    val releaseKeystoreFile = System.getenv("NOTELY_KEYSTORE_FILE")
    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = System.getenv("NOTELY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NOTELY_KEY_ALIAS")
                keyPassword = System.getenv("NOTELY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 + penyusutan resource agar APK rilis sekecil mungkin.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (releaseKeystoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    // Skema database diekspor agar migrasi Room berikutnya aman.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.icons.lucide)

    testImplementation(libs.junit)
    // Tooling Compose hanya untuk pengembangan; tidak ikut ke APK rilis.
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
}
