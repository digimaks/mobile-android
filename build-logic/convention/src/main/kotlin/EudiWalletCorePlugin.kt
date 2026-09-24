// SPDX-License-Identifier: EUPL-1.2

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.libs

class EudiWalletCorePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("kotlinx-serialization")
            }

            dependencies {
                add("api", libs.findLibrary("eudi.wallet.core").get())
            }
        }
    }
}