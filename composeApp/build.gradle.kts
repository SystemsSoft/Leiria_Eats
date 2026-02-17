plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    // Plugin de Serialização (Garante o JSON)
    kotlin("plugin.serialization") version "1.9.23"
}

kotlin {

    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("androidx.compose.ui:ui")

            implementation("io.ktor:ktor-client-okhttp:2.3.7")
            implementation("io.insert-koin:koin-android:3.5.3")

            implementation("com.google.android.gms:play-services-location:21.0.1")
        }

        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.7")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(compose.materialIconsExtended)

            implementation("media.kamel:kamel-image:0.9.4")

            implementation(libs.androidx.datastore.preferences.core)

            // Koin Core (Injeção de Dependência)
            implementation("io.insert-koin:koin-core:3.5.3")
            implementation("io.insert-koin:koin-compose:1.1.2")
            implementation("io.insert-koin:koin-compose-viewmodel:4.0.0")

            // Ktor Core (Rede)
            implementation("io.ktor:ktor-client-core:2.3.7")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

            // Serialização JSON
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            
            // Data e Hora (Multiplatform)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.leria.eats.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.leria.eats.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}