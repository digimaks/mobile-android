// SPDX-License-Identifier: EUPL-1.2

import org.gradle.api.Plugin
import org.gradle.api.Project

class SonarPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.sonarqube")
            }
        }
    }
}