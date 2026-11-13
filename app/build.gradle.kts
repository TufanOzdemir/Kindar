plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Sürüm kodu her CI derlemesinde artar (VERSION_CODE ortam değişkeni). Android, aynı imzalı ve
// sürüm kodu daha yüksek bir APK'yı eski sürümün üzerine kurar; veriler korunur.
val ciVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
val appVersionCode = ciVersionCode ?: 1
val appVersionName = "0.1.$appVersionCode"

// Release imzası: anahtar yalnızca ortam değişkenlerinden gelir, repoda durmaz.
// CI bunları GitHub secret'larından doldurur (README > Release ve imza).
val releaseKeystore = System.getenv("KINDAR_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "com.tufanozdemir.kindar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tufanozdemir.kindar"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KINDAR_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KINDAR_KEY_ALIAS")
                keyPassword = System.getenv("KINDAR_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Debug sürümü ayrı bir uygulama olarak kurulur; release sürümüne ve verisine dokunmaz.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

ksp {
    arg("room.generateKotlin", "true")
}

// Her veritabanı sürümünün şeması repoda saklanır; şema değişikliği CI'da yakalanır ve
// bir Migration yazılmadan yayınlanamaz (bkz. KindarDatabase).
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
}
