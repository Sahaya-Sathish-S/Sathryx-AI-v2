plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sathryx.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sathryx.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
}
