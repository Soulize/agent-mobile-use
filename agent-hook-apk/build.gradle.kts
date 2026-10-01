plugins {
    id("com.android.application")
}

val fixedKeystorePath = System.getenv("APK_SIGNING_KEYSTORE")
val fixedSigningPassword = System.getenv("APK_SIGNING_PASSWORD")

if (System.getenv("CI") == "true" &&
    (fixedKeystorePath.isNullOrBlank() || fixedSigningPassword.isNullOrBlank())) {
    error("CI release builds require APK_SIGNING_KEYSTORE and APK_SIGNING_PASSWORD")
}

android {
    // This module is Java-only. AGP 9 enables built-in Kotlin by default,
    // which otherwise adds kotlin-stdlib to the packaged APK.
    enableKotlin = false

    namespace = "com.agent.mobileuse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.agent.mobileuse"
        minSdk = 26
        targetSdk = 28
        versionCode = 800
        versionName = "0.8.0-alpha"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.directories += "src"
            res.directories += "res"
            assets.directories += "assets"
        }
    }

    signingConfigs {
        if (!fixedKeystorePath.isNullOrBlank() && !fixedSigningPassword.isNullOrBlank()) {
            create("fixed") {
                storeFile = file(fixedKeystorePath)
                storePassword = fixedSigningPassword
                keyAlias = "agentmobileuse"
                keyPassword = fixedSigningPassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("fixed")
                ?: signingConfigs.getByName("debug")
        }
    }

    lint {
        // This APK is sideloaded as part of a KernelSU/LSPosed module and is
        // not published through Google Play. Keep targetSdk 28 unchanged so
        // build-system modernization does not alter Android runtime behavior.
        disable += "ExpiredTargetSdkVersion"
        disable += "BlockedPrivateApi"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // LSPosed/Xposed provides these classes at runtime.
    compileOnly("de.robv.android.xposed:api:82")
}
