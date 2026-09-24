// SPDX-License-Identifier: EUPL-1.2

import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import org.gradle.kotlin.dsl.getByType
import project.convention.logic.AppVersion
import project.convention.logic.config.LibraryModule
import java.util.Properties

plugins {
    id("project.android.application")
    id("project.android.version")
    id("project.android.application.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.play.publisher)
}

play {
    val credsPath = System.getenv("GPLAY_CREDENTIALS_JSON") ?: "${rootDir}/gplay-service-account.json"
    val playTrackName = providers.gradleProperty("playTrack").orNull ?: System.getenv("PLAY_TRACK") ?: "internal"
    serviceAccountCredentials.set(file(credsPath))

    defaultToAppBundles.set(true)
    track.set(playTrackName)
    releaseStatus.set(ReleaseStatus.COMPLETED)
    commit.set(true)
}

android {
    namespace = "lv.dativa.digimaks"
    compileSdk = 36

    packaging {
        resources {
            excludes += "META-INF/*"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    defaultConfig {
        applicationId = "lv.dativa.digimaks"
        minSdk = 26
        targetSdk = 36

        val appVersion = project.extensions.getByType<AppVersion>()
        versionCode = appVersion.code
        versionName = appVersion.name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val props = Properties()
        val file = rootProject.file("keystore.properties")
        if (file.exists()) props.load(file.inputStream())

        create("release") {
            storeFile = file(props.getProperty("storeFile") ?: "keystore.jks")
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true

            matchingFallbacks += listOf("production")
        }
        release {
            firebaseCrashlytics {
                nativeSymbolUploadEnabled = true
            }
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            matchingFallbacks += listOf("production")

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

tasks.register("createRelease") {
    group = "release"
    description = "Creates a new release build"

    dependsOn("clean")
    dependsOn("assembleRelease")
    dependsOn("bundleRelease")

    tasks.findByName("bundleRelease")?.mustRunAfter("clean")
    tasks.findByName("assembleRelease")?.mustRunAfter("clean")
}

tasks.withType<Test> {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))

    classDirectories.setFrom(
        files(
            fileTree("${project.layout.buildDirectory}/tmp/kotlin-classes/debug") {
                exclude(
                    "**/R.class",
                    "**/R$*.class",
                    "**/BuildConfig.*",
                    "**/Manifest*.*",
                    "**/*Test*.*",
                    "android/**/*.*"
                )
            }
        )
    )

    executionData.setFrom(files("${project.layout.buildDirectory}/jacoco/testDebugUnitTest.exec"))
}

dependencies {
    implementation(project(LibraryModule.AssemblyLogic.path))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
}
