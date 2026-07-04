plugins {
    id("com.android.library")
}

android {
    namespace = "com.liskovsoft.sharedutils"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
