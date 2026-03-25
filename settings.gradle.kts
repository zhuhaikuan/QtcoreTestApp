rootProject.name = "QtcoreTestApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }

//    includeBuild("build-logic")
}

dependencyResolutionManagement {
    val artifactoryUser="motomaven"
    val artifactoryUsPassword=""
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            url = uri("${rootDir}/local-maven")
        }
        maven { // 添加私有仓库
            url = uri("http://10.110.159.21:8081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        maven {
            url = uri("https://jogamp.org/deployment/maven/")
        }
        maven {
            url = uri("https://artifacts.mot.com/artifactory/gradle-dev-local")
            name = "motoUsRepo"
            credentials {
                username = artifactoryUser
                password = artifactoryUsPassword
            }
        }
        maven {
            url = uri("https://artifacts.mot.com/artifactory/gradle-dev-local")
            name = "motoUsRepoLocal"
            credentials {
                username = artifactoryUser
                password = artifactoryUsPassword
            }
        }
        maven { url = uri("https://artifactory.tc.lenovo.com/artifactory/gradle-virtual") }

    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")