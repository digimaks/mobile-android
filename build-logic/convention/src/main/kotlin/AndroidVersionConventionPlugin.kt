// SPDX-License-Identifier: EUPL-1.2

import org.gradle.api.Plugin
import org.gradle.api.Project
import project.convention.logic.AppVersion
import java.util.Properties

class AndroidVersionConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val cliVersion = findProperty("VERSION_NAME")
                ?.toString()
                ?.takeUnless { it.equals("unspecified", ignoreCase = true) }

            val fileVersion = run {
                val f = rootProject.file("version.properties")
                if (f.exists()) {
                    Properties().apply { f.inputStream().use { load(it) } }
                        .getProperty("VERSION_NAME") ?: Properties()
                        .getProperty("appVersion")
                } else null
            }

            val versionStr = cliVersion ?: fileVersion ?: "1.0.0"
            val version = AppVersion.parse(versionStr)
            extensions.create("appVersion", AppVersion::class.java, version.major, version.minor, version.patch, version.build)
        }
    }
}