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
            java.setSrcDirs(listOf("src"))
            res.setSrcDirs(listOf("res"))
            assets.setSrcDirs(listOf("assets"))
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
        }
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
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("*.apk")
        rename { "agent_hook.apk" }
    }
    into(layout.buildDirectory)
}
