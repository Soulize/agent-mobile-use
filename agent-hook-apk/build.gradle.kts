plugins {
    id("com.android.application")
}

android {
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
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.directories += "src"
            res.directories += "res"
            assets.directories += "assets"
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            // Keep the release variant non-debuggable while using the standard
            // debug keystore for test/installable CI artifacts. Production
            // signing can be introduced separately without changing bytecode.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint {
        // This APK is a sideloaded KernelSU/LSPosed companion, not a Play Store app.
        // Keep targetSdk 28 intentionally to preserve the legacy runtime behavior
        // while modernizing only the build toolchain.
        disable += "ExpiredTargetSdkVersion"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Framework classes are provided by LSPosed/Xposed at runtime.
    compileOnly("de.robv.android.xposed:api:82")
}

tasks.register<Copy>("packageHookApk") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
        rename { "agent_hook.apk" }
    }
    into(layout.buildDirectory.dir("dist"))
}
