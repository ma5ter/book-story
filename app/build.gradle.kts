plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.androidx.room)
    id("kotlin-parcelize")
}

android {
    namespace = "ua.acclorite.book_story"
    compileSdk = 37

    // Default configuration
    defaultConfig {
        applicationId = "ua.acclorite.book_story"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "1.8.0a"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            keyAlias = "mykey"
            storePassword = System.getenv("PASSWORD")
            keyPassword = System.getenv("PASSWORD")
        }
    }

    // Build types configuration
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = false

            proguardFiles("proguard-rules.pro")

            signingConfig = signingConfigs.getByName("release")
        }

        create("release-debug") {
            initWith(getByName("release"))
            applicationIdSuffix = ".release.debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
    }
}

// Room schema output (project-level, not inside android { })
room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    jvmToolchain(17)
}

// NOTE: The old `aboutLibraries { android { ... } }` block has been removed.
// The new `com.mikepenz.aboutlibraries.plugin.android` plugin handles task
// registration automatically. If you need prettyPrint / excludeFields /
// filterVariants, configure them via the plugin's new `collect { }` and
// `export { }` blocks (see docs: https://github.com/mikepenz/AboutLibraries).

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.android)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.android)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material)

    // All dependencies
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.accompanist.swiperefresh)

    // Dagger - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Datastore (Settings)
    implementation(libs.androidx.datastore.preferences)

    // Splash Screen API
    implementation(libs.androidx.core.splashscreen)

    // SAF
    implementation(libs.anggrayudi.storage)

    // PDF parser
    implementation(libs.pdfbox.android)

    // EPUB parser
    implementation(libs.jsoup)

    // Language Switcher
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appcompat.resources)

    // Coil
    implementation(libs.coil.compose)

    // AboutLibraries — runtime versions bumped to match plugin v15
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)

    // Drag & Drop
    implementation(libs.reorderable)

    // Scrollbar
    implementation(libs.lazycolumnscrollbar)

    // Markdown
    implementation(libs.commonmark)

    // OkHttp
    implementation(libs.okhttp)

    // SVG rasterization for AI image generation responses
    implementation("com.caverock:androidsvg:1.4")

    // Json
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
}