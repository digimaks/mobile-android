// SPDX-License-Identifier: EUPL-1.2

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin
import project.convention.logic.libs

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("project.android.library")
            }
            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            dependencies {
                add("api", kotlin("test"))
                add(
                    "api",
                    libs.findLibrary("androidx-test-orchestrator").get()
                )
                add("api", libs.findLibrary("androidx-test-rules").get())
                add("api", libs.findLibrary("androidx-test-runner").get())
                add("api", libs.findLibrary("androidx-work-testing").get())
                add("api", libs.findLibrary("kotlinx-coroutines-test").get())
                add("api", libs.findLibrary("turbine").get())
                add("api", libs.findLibrary("mockito-core").get())
                add("api", libs.findLibrary("mockito-kotlin").get())
                add("api", libs.findLibrary("robolectric").get())
            }
        }
    }
}
