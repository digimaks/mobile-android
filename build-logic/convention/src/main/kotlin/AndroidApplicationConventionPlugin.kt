// SPDX-License-Identifier: EUPL-1.2

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import project.convention.logic.configureKotlinAndroid
import org.gradle.kotlin.dsl.getByType
import project.convention.logic.AppVersion

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("project.android.version")
                apply("project.android.application.flavors")
                apply("project.android.koin")
                apply("project.android.lint")
                apply("project.sonar")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 36
            }
            val appVersion = extensions.getByType<AppVersion>()
            extensions.getByType<ApplicationExtension>().apply {
                defaultConfig {
                    versionCode = appVersion.code
                    versionName = appVersion.name
                }
            }
        }
    }
}
