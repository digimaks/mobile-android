// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    id("project.wallet.core")
}

android {
    namespace = "lv.zzdats.corelogic"
}

moduleConfig {
    module = LibraryModule.CoreLogic
}

dependencies {
    implementation(project(LibraryModule.ResourcesLogic.path))
    api(project(LibraryModule.BusinessLogic.path))
    implementation(project(LibraryModule.AuthLogic.path))
    implementation(project(LibraryModule.StorageLogic.path))
    implementation(project(LibraryModule.NetworkLogic.path))
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.biometric)
    implementation(libs.ktor.android)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.eudi.wallet.core)
    implementation(libs.webkit)
}
