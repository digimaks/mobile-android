// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.zzdats.proximityfeature"
}
dependencies {
    implementation(project(LibraryModule.IssuanceFeature.path))
}

moduleConfig {
    module = LibraryModule.ProximityFeature
}