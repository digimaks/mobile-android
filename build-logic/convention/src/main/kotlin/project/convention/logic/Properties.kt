// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic

import org.gradle.api.Project
import java.util.Properties

@Suppress("UNCHECKED_CAST")
fun <T> Project.getProperty(key: String, fileName: String = "local.properties"): T? {
    return try {
        val properties = Properties().apply {
            load(rootProject.file(fileName).reader())
        }
        properties[key] as? T
    } catch (_: Exception) {
        null
    }
}