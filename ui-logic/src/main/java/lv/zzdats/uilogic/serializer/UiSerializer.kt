// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.serializer

import lv.zzdats.businesslogic.extensions.decodeFromBase64
import lv.zzdats.businesslogic.extensions.encodeToBase64

interface UiSerializer {
    fun <M : UiSerializable> toBase64(
        model: M,
        parser: UiSerializableParser
    ): String?

    fun <M : UiSerializable> fromBase64(
        payload: String?,
        model: Class<M>,
        parser: UiSerializableParser
    ): M?
}

class UiSerializerImpl : UiSerializer {

    override fun <M : UiSerializable> toBase64(
        model: M,
        parser: UiSerializableParser
    ): String? {
        return try {
            parser.provideParser().toJson(model).encodeToBase64()
        } catch (e: Exception) {
            null
        }
    }

    override fun <M : UiSerializable> fromBase64(
        payload: String?,
        model: Class<M>,
        parser: UiSerializableParser
    ): M? {
        return try {
            parser.provideParser().fromJson(
                payload?.decodeFromBase64(),
                model
            )
        } catch (e: Exception) {
            null
        }
    }
}