// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.resourceslogic.provider

import android.content.ContentResolver
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import lv.zzdats.resourceslogic.R
import java.util.Locale


interface ResourceProvider {
    fun provideContext(): Context
    fun provideContentResolver(): ContentResolver
    fun getString(@StringRes resId: Int): String
    fun getStringFromRaw(@RawRes resId: Int): String
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
    fun genericErrorMessage(): String
    fun genericNetworkErrorMessage(): String
    fun getLocale(): Locale
}

class ResourceProviderImpl(
    private val context: Context
) : ResourceProvider {

    override fun provideContext() = context

    override fun provideContentResolver(): ContentResolver = context.contentResolver

    override fun genericErrorMessage() =
        context.getString(R.string.common_error_description)

    override fun genericNetworkErrorMessage() =
        context.getString(R.string.common_network_error_message)


    override fun getString(@StringRes resId: Int): String =
        try {
            context.getString(resId)
        } catch (_: Exception) {
            ""
        }

    override fun getStringFromRaw(@RawRes resId: Int): String =
        try {
            context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }

    override fun getQuantityString(
        @PluralsRes resId: Int,
        quantity: Int,
        vararg formatArgs: Any
    ): String =
        try {
            context.resources.getQuantityString(resId, quantity, *formatArgs)
        } catch (_: Exception) {
            ""
        }

    override fun getString(resId: Int, vararg formatArgs: Any): String =
        try {
            context.getString(resId, *formatArgs)
        } catch (_: Exception) {
            ""
        }

    override fun getLocale(): Locale = Locale.getDefault()
}