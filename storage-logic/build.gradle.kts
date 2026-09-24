// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    id("project.androidx.room")
}

android {
    namespace = "lv.zzdats.storagelogic"
}


moduleConfig {
    module = LibraryModule.StorageLogic
}

dependencies {
    implementation(project(LibraryModule.BusinessLogic.path))
}