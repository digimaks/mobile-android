// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic

import com.android.build.api.dsl.VariantDimension

@Suppress("IMPLICIT_CAST_TO_ANY")
inline fun <reified ValueT> VariantDimension.addConfigField(
    name: String,
    value: ValueT
) {
    val resolvedValue = when (value) {
        is String -> "\"$value\""
        else -> value
    }.toString()
    buildConfigField(ValueT::class.java.simpleName, name, resolvedValue)
}