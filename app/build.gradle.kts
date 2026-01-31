import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.saiesh.tele"
    compileSdk {
        version = release(36)
    }

    val properties = Properties().apply {
        val localProperties = rootProject.file("local.properties")
        if (localProperties.exists()) {
            localProperties.inputStream().use { stream ->
                load(stream)
            }
        }
    }

    val releaseStoreFile = properties.getProperty("RELEASE_STORE_FILE")
        ?: System.getenv("RELEASE_STORE_FILE")
        ?: ""
    val releaseStorePassword = properties.getProperty("RELEASE_STORE_PASSWORD")
        ?: System.getenv("RELEASE_STORE_PASSWORD")
        ?: ""
    val releaseKeyAlias = properties.getProperty("RELEASE_KEY_ALIAS")
        ?: System.getenv("RELEASE_KEY_ALIAS")
        ?: ""
    val releaseKeyPassword = properties.getProperty("RELEASE_KEY_PASSWORD")
        ?: System.getenv("RELEASE_KEY_PASSWORD")
        ?: ""

    signingConfigs {
        create("release") {
            if (releaseStoreFile.isNotBlank()) {
                storeFile = file(releaseStoreFile)
            }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    defaultConfig {
        applicationId = "com.saiesh.tele"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "0.0.2"

        ndk {
            abiFilters += listOf("armeabi-v7a")
        }

        val apiId = properties.getProperty("TELEGRAM_API_ID") ?: ""
        val apiHash = properties.getProperty("TELEGRAM_API_HASH") ?: ""
        buildConfigField("String", "TELEGRAM_API_ID", "\"$apiId\"")
        buildConfigField("String", "TELEGRAM_API_HASH", "\"$apiHash\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
    }

    androidComponents {
        onVariants(selector().all()) { variant ->
            variant.outputs.forEach { output ->
                output.outputFileName.set("Tele-${'$'}{variant.versionName}.apk")
            }
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.leanback)
    implementation(libs.glide)
}