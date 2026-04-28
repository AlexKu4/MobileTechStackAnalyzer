import java.util.Properties

val isCI = System.getenv("CI") == "true"
val keystoreProperties = Properties().apply {
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.mobiletechstack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mobiletechstack"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "v1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    /*signingConfigs {
        create("release") {
            if (isCI) {
                storeFile = file("./keystore.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            } else {
                val keystoreProperties = Properties().apply {
                    val propsFile = rootProject.file("keystore.properties")
                    if (propsFile.exists()) {
                        propsFile.inputStream().use { load(it) }
                    }
                }
                storeFile = file(keystoreProperties.getProperty("KEYSTORE_PATH") ?: "")
                storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
                keyAlias = keystoreProperties.getProperty("KEY_ALIAS") ?: ""
                keyPassword = keystoreProperties.getProperty("KEY_PASSWORD") ?: ""
            }
        }
    }*/
    buildTypes {
        release {
            //signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.dexlib2)
    implementation(libs.timber)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.gson)
    kapt(libs.androidx.room.compiler)
}

kapt {
    correctErrorTypes = true
}