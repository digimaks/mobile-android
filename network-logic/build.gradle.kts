// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
}

android {
    namespace = "lv.zzdats.networklogic"
}

moduleConfig {
    module = LibraryModule.NetworkLogic
}

dependencies {
    implementation(project(LibraryModule.BusinessLogic.path))
    implementation(project(LibraryModule.AnalyticsLogic.path))

    api(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.mockwebserver)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
}
