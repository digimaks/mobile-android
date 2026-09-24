// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.zzdats.startupfeature"
}
dependencies {
    implementation(project(LibraryModule.CommonFeature.path))
    implementation(project(LibraryModule.NetworkLogic.path))
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.google.play.app.update)
    implementation(libs.kotlinx.coroutines.play.services)
}

moduleConfig {
    module = LibraryModule.StartupFeature
}
