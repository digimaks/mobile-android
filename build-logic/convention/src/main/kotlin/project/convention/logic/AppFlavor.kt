// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.api.Project

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

enum class AppFlavor(
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
    val applicationNameSuffix: String? = null
) {
    Local(FlavorDimension.contentType, applicationIdSuffix = ".dev", applicationNameSuffix = "Local"),
    Demo(FlavorDimension.contentType, applicationIdSuffix = ".demo", applicationNameSuffix = "Demo"),
    Dev(FlavorDimension.contentType, applicationIdSuffix = ".dev", applicationNameSuffix = "Dev"),
    Staging(FlavorDimension.contentType, applicationIdSuffix = ".staging", applicationNameSuffix = "TV"),
    Prod(FlavorDimension.contentType),
}

fun Project.configureFlavors(
    commonExtension: ApplicationExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: AppFlavor) -> Unit = {}
) {

    val version = getProperty<String>(
        "VERSION_NAME",
        "version.properties"
    ).orEmpty()
    val appVersion = AppVersion.parse(version)

    commonExtension.apply {
        flavorDimensions += FlavorDimension.contentType.name
        productFlavors {
            AppFlavor.values().forEach {
                create(it.name.lowercase()) {
                    dimension = it.dimension.name
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        versionName = appVersion.name
                        if (it.applicationIdSuffix != null) {
                            applicationIdSuffix = it.applicationIdSuffix
                        }
                    }
                    manifestPlaceholders["appNameSuffix"] = it.applicationNameSuffix.orEmpty()
                    addConfigField(
                        "APP_VERSION",
                        appVersion.name
                    )
                    flavorConfigurationBlock(this, it)
                }
            }
        }
    }
}

fun Project.configureFlavors(
    commonExtension: LibraryExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: AppFlavor) -> Unit = {}
) {

    val version = getProperty<String>(
        "VERSION_NAME",
        "version.properties"
    ).orEmpty()
    val appVersion = AppVersion.parse(version)

    commonExtension.apply {
        flavorDimensions += FlavorDimension.contentType.name
        productFlavors {
            AppFlavor.values().forEach {
                create(it.name.lowercase()) {
                    dimension = it.dimension.name
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        versionName = appVersion.name
                        if (it.applicationIdSuffix != null) {
                            applicationIdSuffix = it.applicationIdSuffix
                        }
                    }
                    manifestPlaceholders["appNameSuffix"] = it.applicationNameSuffix.orEmpty()
                    addConfigField(
                        "APP_VERSION",
                        appVersion.name
                    )
                    flavorConfigurationBlock(this, it)
                }
            }
        }
    }
}
