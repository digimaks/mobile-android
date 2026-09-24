// SPDX-License-Identifier: EUPL-1.2

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            mavenContent { snapshotsOnly() }
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "Digimaks"
include(":app")
include(":core-logic")
include(":auth-logic")
include(":web-bridge")
include(":resources-logic")
include(":business-logic")
include(":analytics-logic")
include(":assembly-logic")
include(":features:startup-feature")
include(":ui-logic")
include(":features:common-feature")
include(":features:issuance-feature")
include(":features:dashboard-feature")
include(":features:web-feature")
include(":network-logic")
include(":features:presentation-feature")
include(":storage-logic")
include(":features:transactions-feature")
include(":features:sign-feature")
include(":features:proximity-feature")
