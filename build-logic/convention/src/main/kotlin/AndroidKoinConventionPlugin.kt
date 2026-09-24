// SPDX-License-Identifier: EUPL-1.2

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.libs

class AndroidKoinConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")

            when {
                pluginManager.hasPlugin("com.android.application") ->
                    configure<ApplicationExtension> { addVariantOptions(this.sourceSets) }

                pluginManager.hasPlugin("com.android.library") ->
                    configure<LibraryExtension> { addVariantOptions(this.sourceSets) }

                else -> {}
            }

            dependencies {
                add("implementation", libs.findLibrary("koin-android").get())
                add("implementation", libs.findLibrary("koin-annotations").get())
                add("implementation", libs.findLibrary("koin-compose").get())
                add("ksp", libs.findLibrary("koin-ksp").get())
            }
        }
    }

    private fun addVariantOptions(sourceSets: NamedDomainObjectContainer<out AndroidSourceSet>) {
        apply {
            sourceSets.all {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}