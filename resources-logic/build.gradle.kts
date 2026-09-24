// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.library")
    id("project.android.library.compose")
}

android {
    namespace = "lv.zzdats.resourceslogic"

    // Prevent accidentally shipping the entire Vue workspace (node_modules, source, lockfiles, sourcemaps)
    // as Android assets. Only the built `dist/` output should be packaged.
    androidResources {
        // This is a colon-separated pattern string used by AAPT.
        // Keep existing defaults and append project-specific exclusions.
        ignoreAssetsPattern = (ignoreAssetsPattern ?: "") + ":node_modules:src:package.json:pnpm-lock.yaml:vite.config.mjs:.npmrc:*.map"
    }
}

moduleConfig {
    module = LibraryModule.ResourcesLogic
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material3.windowSizeClass)
    api(libs.material)
    api(libs.androidx.core.splashscreen)
}