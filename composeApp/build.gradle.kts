import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildConfigHelper)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

kotlin {
    buildConfig {
        // Lenovo ID
        buildConfigField(
            type = "String",
            name = "LENOVOID_REALM_KEY",
            value = localProperties.getProperty("LENOVOID_REALM_KEY") ?: "not found in local.properties"
        )
        buildConfigField(
            type = "String",
            name = "LENOVOID_PUBLIC_KEY",
            value = localProperties.getProperty("LENOVOID_PUBLIC_KEY") ?: "not found in local.properties"
        )
        buildConfigField(
            type = "String",
            name = "LENOVOID_CLIENT_ID",
            value = localProperties.getProperty("LENOVOID_CLIENT_ID") ?: "not found in local.properties"
        )
        buildConfigField(
            type = "String",
            name = "LENOVOID_REALM_ID",
            value = localProperties.getProperty("LENOVOID_REALM_ID") ?: "not found in local.properties"
        )
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    jvm("desktop")
    
    sourceSets {
//        configurations.all {
//            if (name.contains("android", ignoreCase = true)) {
//                exclude(group = "com.lenovo.quantum.sdk", module = "logback-classic")
//            }
//        }

        androidMain.dependencies {
//            implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.moto.motoaccountsdk)
            implementation(libs.gson)
            implementation(libs.androidx.browser)
            implementation(libs.security.crypto)

            implementation(files("./src/libs/qiracoresdk-android-1.1.2.aar"))

            implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.org.json)
            implementation(libs.kotlin.serialization)
            implementation(libs.coil.compose)
            implementation(libs.coil.network)
            implementation(libs.compose.multiplatform.media.player)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)

            compileOnly(files("./src/libs/qiracoresdk-desktop-1.1.2.jar"))
            implementation("androidx.compose.material:material:1.9.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("com.google.code.gson:gson:2.13.2")
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(files("./src/libs/qiracoresdk-desktop-1.1.2.jar"))
        }
    }
}

android {
    namespace = "com.lenovo.quantum.test"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.lenovo.quantum.test"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile =
                file("../config/build/common2.keystore")
            storePassword = "motorola"
            keyPassword = "motorola"
            keyAlias = "common2"
        }

        create("release") {
            storeFile = file("../testRelease")
            storePassword = "123456"
            keyPassword = "123456"
            keyAlias = "key0"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            val keystoreFile = file("../testRelease")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.lenovo.quantum.test.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.lenovo.quantum.test"
            packageVersion = "1.0.0"
        }
    }
}
