// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.document_details.model

data class DocumentDetail(
    val identifier: String,
    val title: String,
    val value: String? = null,
    val base64Image: String? = null
) {
    val type: Type get() = if (!base64Image.isNullOrEmpty()) Type.IMAGE else Type.TEXT
    enum class Type { TEXT, IMAGE }

    fun toWebMap(): Map<String, Any?> = mapOf(
        "type" to (if (type == Type.IMAGE) "image" else "text"),
        "identifier" to identifier,
        "label" to title,
        "value" to value,
        "image" to base64Image
    )
}