plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.ksp.android)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.aditya1875.pokeverse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aditya1875.pokeverse"
        minSdk = 25
        targetSdk = 36

        versionCode = 44
        versionName = "1.4.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(
                System.getenv("RELEASE_STORE_FILE")
                    ?: project.findProperty("RELEASE_STORE_FILE") as String?
                    ?: ""
            )

            storePassword =
                System.getenv("RELEASE_STORE_PASSWORD")
                    ?: project.findProperty("RELEASE_STORE_PASSWORD") as String?

            keyAlias =
                System.getenv("RELEASE_KEY_ALIAS")
                    ?: project.findProperty("RELEASE_KEY_ALIAS") as String?

            keyPassword =
                System.getenv("RELEASE_KEY_PASSWORD")
                    ?: project.findProperty("RELEASE_KEY_PASSWORD") as String?
        }
    }


    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("play") {
            dimension = "distribution"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
            buildConfigField("boolean", "USE_FIREBASE", "true")
        }
        create("foss") {
            dimension = "distribution"
//            applicationIdSuffix = ".foss"
//            versionNameSuffix = "-foss"
            isDefault = true
            buildConfigField("boolean", "USE_FIREBASE", "false")
        }
    }

    sourceSets {
        getByName("play") {
            java.srcDir("src/play/java")
            res.srcDir("src/play/res")
            manifest.srcFile("src/play/AndroidManifest.xml")
        }
        getByName("foss") {
            java.srcDir("src/foss/java")
            res.srcDir("src/foss/res")
            manifest.srcFile("src/foss/AndroidManifest.xml")
        }
    }
}

if (gradle.startParameter.taskNames.any { it.lowercase().contains("play") }) {
    apply(plugin = "com.google.gms.google-services")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.bottombar)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    ksp(libs.androidx.room.compiler)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.foundation)

    // Koin Core
    implementation(libs.koin.core)
    implementation(libs.material3.window.size.class1)

    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)

    // Koin for Android
    implementation(libs.koin.android)

    // Koin for Jetpack Compose
    implementation(libs.koin.androidx.compose)

    implementation(libs.androidx.core.splashscreen)

    // haze
    implementation(libs.haze.jetpack.compose)

    implementation(libs.ui)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.accompanist.placeholder.material)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.navigation.animation.v0340)

    add("playImplementation", libs.firebase.analytics)
    add("playImplementation", platform(libs.firebase.bom))
    add("playImplementation", libs.firebase.messaging)
//    add("playImplementation", libs.androidx.core.ktx)
//    add("playImplementation",libs.firebase.common.ktx)

    // Glance Widget
    implementation (libs.androidx.glance.appwidget)
    implementation (libs.androidx.work.runtime.ktx)
    implementation(libs.colorpicker.compose)

    implementation(libs.androidx.material.icons.core)
    // AboutLibraries
    implementation(libs.aboutlibraries.compose.m3)

    implementation(libs.liquid)
    // ui tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(kotlin("test"))
}

// Disable baseline profiles to make builds reproducible
tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}
