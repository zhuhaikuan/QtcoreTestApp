import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Copy
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildConfigHelper)
    alias(libs.plugins.dokka)
    id("dokka-convention")
    alias(libs.plugins.shadowJar)
}

// Configure Dokka using the simplified extension (buildSrc/dokka) over the gradle/dokka
dokkaModule {
    moduleName = "QTCore SDK"
    include("Module.md")
}

val sdkVersionCode = Versioning.getVersionCode(rootProject.projectDir, "Sdk")
val sdkVersionName = Versioning.getVersionName(rootProject.projectDir, "Sdk", includeGitSha = false)
val aarDir = layout.buildDirectory.dir("outputs/aar")
val libsDir = layout.buildDirectory.dir("artifacts")
val thirdPartyNoticePath = "src/windowsNativeMain/assets/ThirdPartyNotices.txt"

buildConfig {
    className("SdkBuildConfig")
    packageName("com.lenovo.quantum.sdk")

    buildConfigField("Int", "VERSION_CODE", sdkVersionCode.toString())
    buildConfigField("String", "VERSION_NAME", "\"$sdkVersionName\"")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    jvm("desktop")

    mingwX64("windowsNative") {
        binaries {
            sharedLib {
                baseName = "qiracore-sdk"

                // for windows apis
                linkerOpts("-lkernel32", "-ladvapi32")
            }
        }

        compilations.getByName("main") {
            cinterops {
                val kernel32 by creating {
                    // c interop definition file
                    defFile(project.file("src/windowsNativeMain/nativeInterop/cinterop/kernel32.def"))
                    packageName("platform.windows")
                }
            }
        }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(libs.jna)
                implementation(libs.jna.platform)
                implementation(libs.slf4j.api)
                implementation(libs.logback)
            }
        }

        val windowsNativeMain by getting {
            dependencies {
                implementation(libs.okio)
            }
        }

        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.serialization)
                implementation(libs.kotlin.serialization.core)
                implementation("io.ktor:ktor-client-core:3.1.3")
                implementation("io.ktor:ktor-client-cio:3.1.3")
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }
    }
}

android {
    namespace = "com.lenovo.quantum.sdk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    buildFeatures {
        aidl = true
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}


tasks.named<Jar>("desktopJar") {
    archiveFileName.set("qiracoresdk-desktop-$sdkVersionName.jar")
    destinationDirectory.set(libsDir)
    manifest {
        attributes(
            "Implementation-Title" to "QiraCore SDK",
            "Implementation-Version" to sdkVersionName
        )
    }
    outputs.file(archiveFile)
}

val sdkJarOut = tasks.named<Jar>("desktopJar").flatMap { it.archiveFile }
val sdkAarOut = libsDir.map { it.file("qiracoresdk-android-$sdkVersionName.aar") }

val renameAarFiles = tasks.register<Copy>("renameAarFiles") {
    dependsOn("bundleReleaseAar")
    from(aarDir.map { it.asFileTree.matching { include("*-release.aar") } })
    into(libsDir)
    rename(Pattern.compile(".*-release\\.aar"), "qiracoresdk-android-$sdkVersionName.aar")
    outputs.file(sdkAarOut)
}

val sdkDllOut = libsDir.map { it.file("qiracore-sdk-$sdkVersionName.dll") }

val copyDllToLibs = tasks.register<Copy>("copyDllToLibs") {
    dependsOn("linkReleaseSharedWindowsNative")
    from(layout.buildDirectory.dir("bin/windowsNative/releaseShared")) {
        include("*.dll")
    }
    into(libsDir)
    rename("qiracore_sdk.dll", "qiracore-sdk-$sdkVersionName.dll")
    outputs.file(sdkDllOut)
}

val sdkPackageDir = layout.buildDirectory.dir("artifacts/qiracore-sdk")

val prepareSdkPackage = tasks.register<Copy>("prepareSdkPackage") {
    dependsOn(tasks.named("desktopJar"), copyDllToLibs, renameAarFiles)
    into(sdkPackageDir)
    from(sdkDllOut)
    from(thirdPartyNoticePath)
}

val cleanupPcSdkPackage = tasks.register<Delete>("cleanupPcSdkPackage") {
    delete(sdkPackageDir)
}

val packagePcSdk = tasks.register<Zip>("packagePcSdk") {
    group = "distribution"
    description = "Packages the Windows Native PC SDK into a ZIP file with it's own DLL file"
    dependsOn(prepareSdkPackage)
    finalizedBy(cleanupPcSdkPackage)

    archiveFileName.set("qiracore-sdk-$sdkVersionName.zip")
    destinationDirectory.set(libsDir)
    from(sdkPackageDir)
}

tasks.register("buildAllArtifacts") {
    dependsOn(tasks.named("desktopJar"), renameAarFiles, copyDllToLibs, packagePcSdk)
}
// =================================================

configurations {
    create("dist") {
        isCanBeConsumed = true
        isCanBeResolved = false
    }
}

artifacts {
    add("dist", sdkJarOut) {
        builtBy(tasks.named("desktopJar"))
        type = "jar"
    }
    add("dist", sdkAarOut) {
        builtBy(renameAarFiles)
        type = "aar"
    }
    add("dist", sdkDllOut) {
        builtBy(copyDllToLibs)
        type = "dll"
    }
}

// Fix configuration cache issues with Kotlin Multiplatform metadata transformation
tasks.withType<org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyTransformationTask>().configureEach {
    notCompatibleWithConfigurationCache("Kotlin Multiplatform metadata transformation serialization issue")
}

rootProject.subprojects
    .filter { it != project }
    .forEach { consumer ->
        consumer.tasks.matching { it.name == "flattenJars" }.configureEach {
            dependsOn(":sdk:buildAllArtifacts")
            inputs.files(sdkJarOut, sdkAarOut, sdkDllOut)

            (this as? Copy)?.apply {
                from(sdkJarOut)
                from(sdkAarOut)
                from(sdkDllOut)
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
    }

dependencyLocking {
    lockAllConfigurations()
    lockMode = LockMode.STRICT
    lockFile = file("$projectDir/gradle.lockfile")
}