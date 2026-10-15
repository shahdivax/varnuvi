import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

val releasePropertiesFile = rootProject.file("key.properties")
val releaseProperties = Properties().apply {
    if (releasePropertiesFile.exists()) releasePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "dev.voiceagent.app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "dev.voiceagent.app"
        minSdk = 33
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (releasePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseProperties.getProperty("storeFile")))
                storePassword = requireNotNull(releaseProperties.getProperty("storePassword"))
                keyAlias = requireNotNull(releaseProperties.getProperty("keyAlias"))
                keyPassword = requireNotNull(releaseProperties.getProperty("keyPassword"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Without private signing properties, builds stay unsigned.
            // Never silently distribute a release signed with a debug key.
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

flutter {
    source = "../.."
}
