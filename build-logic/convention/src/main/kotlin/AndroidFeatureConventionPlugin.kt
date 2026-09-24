// SPDX-License-Identifier: EUPL-1.2

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.config.LibraryModule

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("project.android.library")
                apply("project.android.library.compose")
            }
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            dependencies {
                add("implementation", project(LibraryModule.AnalyticsLogic.path))
                add("implementation", project(LibraryModule.UiLogic.path))
                add("implementation", project(LibraryModule.AuthLogic.path))
                add("implementation", project(LibraryModule.BusinessLogic.path))
                add("implementation", project(LibraryModule.CoreLogic.path))
                add("implementation", project(LibraryModule.ResourcesLogic.path))
                add("implementation", project(LibraryModule.WebBridge.path))
                add("implementation", project(LibraryModule.StorageLogic.path))
            }
        }
    }
}
