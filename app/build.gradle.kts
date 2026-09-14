import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Lê a chave da Comic Vine de local.properties (que está no .gitignore).
val comicVineApiKey: String = run {
    val f = rootProject.file("local.properties")
    val key = if (f.exists()) {
        Properties().apply { f.inputStream().use { load(it) } }
            .getProperty("COMIC_VINE_API_KEY") ?: ""
    } else ""
    if (key.isBlank()) {
        println("⚠️  COMIC_VINE_API_KEY não encontrada em local.properties — o Akinator não vai conseguir buscar personagens.")
    }
    key
}

android {
    namespace = "com.ovigia.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ovigia.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COMIC_VINE_API_KEY", "\"$comicVineApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Rede (Comic Vine)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Imagens
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Arquitetura (MVVM)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}