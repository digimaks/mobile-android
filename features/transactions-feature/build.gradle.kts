// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.zzdats.transactionsfeature"
}
dependencies {
    implementation(project(":storage-logic"))
}

moduleConfig {
    module = LibraryModule.TransactionsFeature
}