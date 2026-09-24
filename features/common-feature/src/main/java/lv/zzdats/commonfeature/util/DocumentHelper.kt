// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.util

import android.util.Base64
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.NameSpace
import eu.europa.ec.eudi.wallet.document.format.DocumentClaim
import eu.europa.ec.eudi.wallet.document.format.MsoMdocData
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcClaim
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcData
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import lv.zzdats.businesslogic.extensions.decodeFromBase64
import lv.zzdats.businesslogic.extensions.encodeToBase64String
import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.businesslogic.util.safeLet
import lv.zzdats.businesslogic.util.toDateFormatted
import lv.zzdats.businesslogic.util.toLocalDate
import lv.zzdats.corelogic.extension.getLocalizedClaimName
import lv.zzdats.corelogic.extension.removeEmptyGroups
import lv.zzdats.corelogic.extension.sortRecursivelyBy
import lv.zzdats.corelogic.model.ClaimPath
import lv.zzdats.corelogic.model.DomainClaim
import lv.zzdats.resourceslogic.provider.ResourceProvider
import java.time.LocalDate
import lv.zzdats.resourceslogic.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun extractValueFromDocumentOrEmpty(
    document: IssuedDocument,
    key: String
): String {
    return document.data.claims
        .firstOrNull { it.identifier == key }
        ?.value
        ?.toString()
        ?: ""
}

/**
 * Converts any supported date representation (timestamp or date string) to dd.MM.yyyy. HH:mm format.
 */
fun convertAnyToFormattedDate(value: Any?): String? {
    val localDateTime = value.toLocalDateTimeOrNull() ?: return value?.toString()
    return localDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy."))
}

fun convertAnyToDateOnly(value: Any?): String? {
    val localDateTime = value.toLocalDateTimeOrNull() ?: return value?.toString()
    return localDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy."))
}

private fun Any?.toLocalDateTimeOrNull(): LocalDateTime? {
    when (this) {
        is Instant -> return this.atZone(ZoneId.systemDefault()).toLocalDateTime()
        is OffsetDateTime -> return this.toLocalDateTime()
        is ZonedDateTime -> return this.toLocalDateTime()
        is LocalDateTime -> return this
        is LocalDate -> return this.atStartOfDay()
    }

    val timestamp = when (this) {
        is Number -> this.toLong()
        is String -> this.toLongOrNull()
        else -> null
    }
    if (timestamp != null) {
        val instant = Instant.ofEpochSecond(timestamp)
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    val value = this as? String ?: return null
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null

    runCatching { return Instant.parse(trimmed).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    runCatching { return OffsetDateTime.parse(trimmed).toLocalDateTime() }
    runCatching { return ZonedDateTime.parse(trimmed).toLocalDateTime() }

    runCatching { return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    runCatching { return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) }
    runCatching { return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }
    runCatching { return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay() }

    val localDate: LocalDate? = trimmed.toLocalDate()
    if (localDate != null) {
        return localDate.atStartOfDay()
    }

    return null
}

fun extractFullNameFromDocumentOrEmpty(document: IssuedDocument): String {
    // Try standard and Diploma first name formats
    val firstName = listOf(
        DocumentJsonKeys.FIRST_NAME,      // "given_name"
        DocumentJsonKeys.DIPLOMA_FIRST_NAME    // "givenName"
    ).firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(document, key)
        if (value.isNotEmpty()) value else null
    } ?: ""

    // Try standard and Diploma last name formats
    val lastName = listOf(
        DocumentJsonKeys.LAST_NAME,       // "family_name"
        DocumentJsonKeys.DIPLOMA_LAST_NAME     // "familyName"
    ).firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(document, key)
        if (value.isNotEmpty()) value else null
    } ?: ""

    val fullName = when {
        firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
        firstName.isNotBlank() -> firstName
        lastName.isNotBlank() -> lastName
        else -> ""
    }
    return fullName
}

fun extractFirstNameFromDocumentOrEmpty(document: IssuedDocument): String {
    // Try standard and Diploma first name formats
    val firstName = listOf(
        DocumentJsonKeys.FIRST_NAME,      // "given_name"
        DocumentJsonKeys.DIPLOMA_FIRST_NAME    // "givenName"
    ).firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(document, key)
        if (value.isNotEmpty()) value else null
    } ?: ""

    return firstName
}

fun keyIsBase64(key: String): Boolean {
    val listOfBase64Keys = DocumentJsonKeys.BASE64_IMAGE_KEYS
    return listOfBase64Keys.contains(key)
}

private fun keyIsUserPseudonym(key: String): Boolean {
    return key == DocumentJsonKeys.USER_PSEUDONYM
}

private fun keyIsGender(key: String): Boolean {
    val listOfGenderKeys = DocumentJsonKeys.GENDER_KEYS
    return listOfGenderKeys.contains(key)
}

private fun getGenderValue(value: String, resourceProvider: ResourceProvider): String =
    when (value) {
        "0" -> {
            resourceProvider.getString(R.string.request_gender_unknown)
        }

        "1" -> {
            resourceProvider.getString(R.string.request_gender_male)
        }

        "2" -> {
            resourceProvider.getString(R.string.request_gender_female)
        }

        "9" -> {
            resourceProvider.getString(R.string.request_gender_not_applicable)
        }

        else -> {
            value
        }
    }

private val SD_JWT_TIMESTAMP_FIELDS = setOf("iat", "exp", "nbf")

private fun convertTimestampToDate(timestamp: Any?): String? {
    return try {
        val timestampLong = when (timestamp) {
            is Number -> timestamp.toLong()
            is String -> timestamp.toLongOrNull()
            else -> null
        } ?: return null

        val instant = Instant.ofEpochSecond(timestampLong)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (e: Exception) {
        null
    }
}

fun parseKeyValueUi(
    item: Any,
    groupIdentifier: String,
    groupIdentifierKey: String,
    keyIdentifier: String = "",
    resourceProvider: ResourceProvider,
    allItems: StringBuilder
) {
    when (item) {

        is Map<*, *> -> {
            item.forEach { (key, value) ->
                safeLet(key as? String, value) { key, value ->
                    parseKeyValueUi(
                        item = value,
                        groupIdentifier = groupIdentifier,
                        groupIdentifierKey = groupIdentifierKey,
                        keyIdentifier = key,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Collection<*> -> {
            item.forEach { value ->
                value?.let {
                    parseKeyValueUi(
                        item = it,
                        groupIdentifier = groupIdentifier,
                        groupIdentifierKey = groupIdentifierKey,
                        resourceProvider = resourceProvider,
                        allItems = allItems
                    )
                }
            }
        }

        is Boolean -> {
            allItems.append(
                if (item) {
                    "true"
                } else {
                    "false"
                }
            )
        }

        else -> {
            if (groupIdentifierKey in SD_JWT_TIMESTAMP_FIELDS) {
                val formattedDate = convertTimestampToDate(item)
                if (formattedDate != null) {
                    allItems.append(formattedDate)
                    return
                }
            }

            val date: String? = (item as? String)
            allItems.append(
                when {

                    keyIsGender(groupIdentifierKey) -> {
                        getGenderValue(item.toString(), resourceProvider)
                    }

                    keyIsUserPseudonym(groupIdentifierKey) -> {
                        item.toString().decodeFromBase64()
                    }

                    date != null && keyIdentifier.isEmpty() -> {
                        date
                    }

                    else -> {
                        val jsonString = item.toString()
                        if (keyIdentifier.isEmpty()) {
                            jsonString
                        } else {
                            val lineChange = if (allItems.isNotEmpty()) "\n" else ""
                            val value = jsonString ?: jsonString
                            "$lineChange$keyIdentifier: $value"
                        }
                    }
                }
            )
        }
    }
}

fun documentHasExpired(
    documentExpirationDate: String,
    currentDate: LocalDate = LocalDate.now(),
): Boolean {
    val localDateOfDocumentExpirationDate = documentExpirationDate.toLocalDate()

    return localDateOfDocumentExpirationDate?.let {
        currentDate.isAfter(it)
    } ?: false
}

fun extractDrivingPrivileges(drivingPrivileges: Any?): String {
    return try {
        @Suppress("UNCHECKED_CAST")
        (drivingPrivileges as? List<Map<String, Any>>)?.mapNotNull { category ->
            category["vehicle_category_code"] as? String
        }?.joinToString(", ") ?: ""
    } catch (_: Exception) {
        ""
    }
}

val IssuedDocument.docNamespace: NameSpace?
    get() = when (val data = this.data) {
        is MsoMdocData -> data.nameSpaces.keys.first()
        is SdJwtVcData -> null
    }

fun transformPathsToDomainClaims(
    pathsWithIntent: Map<ClaimPath, Boolean>,
    claims: List<DocumentClaim>,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider
): List<DomainClaim> {
    return pathsWithIntent.entries.fold<Map.Entry<ClaimPath, Boolean>, List<DomainClaim>>(
        initial = emptyList()
    ) { acc, (path, intentToRetain) ->
        insertPath(
            tree = acc,
            path = path,
            disclosurePath = path,
            claims = claims,
            resourceProvider = resourceProvider,
            intentToRetain = intentToRetain,
            uuidProvider = uuidProvider
        )
    }.removeEmptyGroups()
        .sortRecursivelyBy {
            it.displayTitle.lowercase()
        }
}

private fun insertPath(
    tree: List<DomainClaim>,
    path: ClaimPath,
    disclosurePath: ClaimPath,
    claims: List<DocumentClaim>,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    intentToRetain: Boolean
): List<DomainClaim> {
    if (path.value.isEmpty()) return tree

    val userLocale = resourceProvider.getLocale()

    val key = path.value.first()

    val existingNode = tree.find { it.key == key }

    val currentClaim: DocumentClaim? = claims.find { it.identifier == key }

    return if (path.value.size == 1) {
        // Leaf node (Primitive or Nested Structure)
        if (existingNode == null && currentClaim != null) {
            val accumulatedClaims: MutableList<DomainClaim> = mutableListOf()
            createKeyValue(
                item = currentClaim.value!!,
                groupKey = currentClaim.identifier,
                resourceProvider = resourceProvider,
                uuidProvider = uuidProvider,
                claimMetaData = currentClaim.issuerMetadata,
                disclosurePath = disclosurePath,
                allItems = accumulatedClaims,
                intentToRetain = intentToRetain
            )
            tree + accumulatedClaims
        } else {
            tree // Already exists or not available, return unchanged
        }
    } else {
        // Group node (Intermediate)
        val childClaims =
            (claims.find { key == it.identifier } as? SdJwtVcClaim)?.children ?: claims
        val updatedNode = if (existingNode is DomainClaim.Group) {
            // Update existing group by inserting the next path segment into its items
            existingNode.copy(
                items = insertPath(
                    tree = existingNode.items,
                    path = ClaimPath(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    resourceProvider = resourceProvider,
                    uuidProvider = uuidProvider,
                    intentToRetain = intentToRetain
                )
            )
        } else {
            // Create a new group and insert the next path segment
            DomainClaim.Group(
                key = currentClaim?.identifier ?: key,
                displayTitle = getReadableNameFromIdentifier(
                    claimMetaData = currentClaim?.issuerMetadata,
                    userLocale = userLocale,
                    fallback = currentClaim?.identifier ?: key
                ),
                path = ClaimPath(disclosurePath.value.take((disclosurePath.value.size - path.value.size) + 1)),
                items = insertPath(
                    tree = emptyList(),
                    path = ClaimPath(path.value.drop(1)),
                    disclosurePath = disclosurePath,
                    claims = childClaims,
                    resourceProvider = resourceProvider,
                    uuidProvider = uuidProvider,
                    intentToRetain = intentToRetain
                )
            )
        }

        tree.filter { it.key != key } + updatedNode // Replace or add the updated node
    }
}

fun getReadableNameFromIdentifier(
    claimMetaData: IssuerMetadata.Claim?,
    userLocale: Locale,
    fallback: String,
): String {
    return claimMetaData
        ?.display.getLocalizedClaimName(
            userLocale = userLocale,
            fallback = fallback
        )
}

fun createKeyValue(
    item: Any,
    groupKey: String,
    childKey: String = "",
    disclosurePath: ClaimPath,
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    claimMetaData: IssuerMetadata.Claim?,
    allItems: MutableList<DomainClaim>,
    intentToRetain: Boolean,
) {
    @OptIn(ExperimentalUuidApi::class)
    fun addFlatOrGroupedChildren(
        allItems: MutableList<DomainClaim>,
        children: List<DomainClaim>,
        groupKey: String,
        displayTitle: String,
        predicate: () -> Boolean
    ) {

        val groupIsAlreadyPresent = children
            .filterIsInstance<DomainClaim.Group>()
            .any { it.key == groupKey }

        if (predicate() && !groupIsAlreadyPresent) {
            allItems.add(
                DomainClaim.Group(
                    key = groupKey,
                    displayTitle = displayTitle,
                    path = ClaimPath(listOf(uuidProvider.provideUuid())),
                    items = children
                )
            )
        } else {
            allItems.addAll(children)
        }
    }

    when (item) {

        is Map<*, *> -> {

            val children: MutableList<DomainClaim> = mutableListOf()
            val childKeys: MutableList<String> = mutableListOf()

            item.forEach { (key, value) ->
                safeLet(key as? String, value) { key, value ->

                    val newGroupKey = if (value is Collection<*>) key else groupKey
                    val newChildKey = if (value is Collection<*>) "" else key

                    childKeys.add(newChildKey)

                    createKeyValue(
                        item = value,
                        groupKey = newGroupKey,
                        childKey = newChildKey,
                        disclosurePath = disclosurePath,
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider,
                        claimMetaData = null,
                        allItems = children,
                        intentToRetain = intentToRetain
                    )
                }
            }

            addFlatOrGroupedChildren(
                allItems = allItems,
                children = children,
                groupKey = groupKey,
                displayTitle = getReadableNameFromIdentifier(
                    claimMetaData = claimMetaData,
                    userLocale = resourceProvider.getLocale(),
                    fallback = groupKey
                )
            ) {
                childKeys.none { it.isEmpty() }
            }
        }

        is Collection<*> -> {

            val children: MutableList<DomainClaim> = mutableListOf()

            item.forEach { value ->
                value?.let {
                    createKeyValue(
                        item = it,
                        groupKey = groupKey,
                        disclosurePath = disclosurePath,
                        resourceProvider = resourceProvider,
                        uuidProvider = uuidProvider,
                        claimMetaData = claimMetaData,
                        allItems = children,
                        intentToRetain = intentToRetain
                    )
                }
            }

            addFlatOrGroupedChildren(
                allItems = allItems,
                children = children,
                groupKey = groupKey,
                displayTitle = getReadableNameFromIdentifier(
                    claimMetaData = claimMetaData,
                    userLocale = resourceProvider.getLocale(),
                    fallback = groupKey
                )
            ) {
                childKey.isEmpty()
            }
        }

        else -> {

            val base64Image = (item as? ByteArray)?.encodeToBase64String(Base64.URL_SAFE)

            val date: String? = (item as? String)?.toDateFormatted()
                ?: (item as? LocalDate)?.toDateFormatted()

            val formattedValue = when {
                base64Image != null -> base64Image
                keyIsGender(groupKey) -> getGenderValue(item.toString(), resourceProvider)
                keyIsUserPseudonym(groupKey) -> item.toString().decodeFromBase64()
                date != null -> date
                item is Boolean -> resourceProvider.getString(
                    if (item) R.string.document_details_boolean_item_true_readable_value
                    else R.string.document_details_boolean_item_false_readable_value
                )

                else -> item.toString()
            }

            allItems.add(
                DomainClaim.Primitive(
                    key = childKey.ifEmpty { groupKey },
                    displayTitle = childKey.ifEmpty {
                        getReadableNameFromIdentifier(
                            claimMetaData = claimMetaData,
                            userLocale = resourceProvider.getLocale(),
                            fallback = groupKey
                        )
                    },
                    path = disclosurePath,
                    isRequired = false,
                    value = formattedValue,
                    intentToRetain = intentToRetain
                )
            )
        }
    }
}
