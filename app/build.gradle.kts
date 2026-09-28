plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.linetra.upishortcut"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.linetra.upishortcut"
        minSdk = 26 // pinned shortcuts need Android 8.0+
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // Upload key for Play (Play App Signing holds the real signing key). The keystore and its
    // passwords live outside the repo, in ~/.gradle/gradle.properties:
    //   upishortcut.storeFile=/path/to/upload-keystore.jks
    //   upishortcut.storePassword=...
    //   upishortcut.keyAlias=upload
    //   upishortcut.keyPassword=...
    // Without them, release builds are unsigned.
    val uploadStore = providers.gradleProperty("upishortcut.storeFile").orNull
    signingConfigs {
        if (uploadStore != null) {
            create("upload") {
                storeFile = file(uploadStore)
                storePassword = providers.gradleProperty("upishortcut.storePassword").get()
                keyAlias = providers.gradleProperty("upishortcut.keyAlias").get()
                keyPassword = providers.gradleProperty("upishortcut.keyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")

    implementation("androidx.glance:glance-appwidget:1.1.1")

    // Scanner UI and QR decoding are served by Google Play services (no camera permission, small APK).
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")

    testImplementation("junit:junit:4.13.2")
}
