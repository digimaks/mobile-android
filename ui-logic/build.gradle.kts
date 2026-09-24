// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    id("project.android.library.compose")
}

android {
    namespace = "lv.zzdats.uilogic"
}

moduleConfig {
    module = LibraryModule.UiLogic
}

dependencies {
    implementation(project(LibraryModule.ResourcesLogic.path))
    implementation(project(LibraryModule.BusinessLogic.path))
    implementation(project(LibraryModule.AnalyticsLogic.path))
    implementation(project(LibraryModule.CoreLogic.path))

    implementation(libs.gson)
}
