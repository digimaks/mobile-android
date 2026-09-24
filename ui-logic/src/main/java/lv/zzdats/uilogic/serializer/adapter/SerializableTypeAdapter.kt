// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.serializer.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

interface SerializableAdapterType

class SerializableTypeAdapter<T : Any> : JsonSerializer<T>, JsonDeserializer<T>,
    SerializableAdapterType {

    private val CLASSNAME = "CLASSNAME"
    private val DATA = "DATA"

    @Throws(JsonParseException::class, RuntimeException::class)
    override fun serialize(
        src: T?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src != null && context != null) {
            return JsonObject().apply {
                addProperty(CLASSNAME, src::class.java.name)
                add(DATA, context.serialize(src))
            }
        }
        throw RuntimeException("SerializableTypeAdapter:: Failed to serialize: ${src?.javaClass?.name}")
    }

    @Throws(JsonParseException::class, RuntimeException::class)
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): T {
        val jsonObject = json?.asJsonObject
        val className = jsonObject?.get(CLASSNAME)?.asString
        val clazz = getObjectClass(className)
        return context?.deserialize(jsonObject?.get(DATA), clazz)
            ?: throw RuntimeException("SerializableTypeAdapter:: Failed to deserialize: $className")
    }

    @Throws(JsonParseException::class)
    private fun getObjectClass(className: String?): Class<*> {
        try {
            return Class.forName(className.toString())
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }
    }
}
