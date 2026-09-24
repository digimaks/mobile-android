// SPDX-License-Identifier: EUPL-1.2

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.libs

class KtorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("implementation", libs.findLibrary("ktor-android").get())
                add("implementation", libs.findLibrary("ktor-logging").get())
                add("implementation", libs.findLibrary("ktor-client-content-negotiation").get())
                add("implementation", libs.findLibrary("ktor-serialization-kotlinx-json").get())
            }
        }
    }
}
