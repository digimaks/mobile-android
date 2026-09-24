// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.util

object DocumentJsonKeys {
    const val FIRST_NAME = "given_name"
    const val LAST_NAME = "family_name"
    const val PORTRAIT = "portrait"
    const val SIGNATURE = "signature_usual_mark"
    const val EXPIRY_DATE = "expiry_date"
    const val ISSUANCE_DATE = "issuance_date"
    const val ISSUE_DATE = "issue_date"
    const val USER_PSEUDONYM = "user_pseudonym"
    const val ISSUING_AUTHORITY = "issuing_authority"
    const val ISSUER_COUNTRY = "issuing_country"
    const val DRIVING_PRIVILEGES = "driving_privileges"

    const val PID_ID_NUMBER = "personal_administrative_number"

    const val MDL_ID_NUMBER = "document_number"

    const val DIPLOMA_ISSUANCE_DATE = "issued"
    const val DIPLOMA_ISSUER_COUNTRY = "awardingBody_countryCode"
    const val DIPLOMA_ACHIEVEMENT = "title"
    const val DIPLOMA_THEMATIC_AREA = "thematicArea"
    const val DIPLOMA_FIRST_NAME = "givenName"
    const val DIPLOMA_LAST_NAME = "familyName"
    const val DIPLOMA_ID_NUMBER = "nationalID"

    const val SIGNING_ISSUANCE_DATE = "issuedOn"
    const val SIGNING_EXPIRY_DATE = "expiresOn"
    const val SIGNING_NAME = "cn"

    const val SEB_IBAN = "sub"

    const val JWT_EXPIRTY_DATE = "exp"
    const val JWT_ISSUED_DATE = "iat"

    private const val GENDER = "gender"
    private const val SEX = "sex"

    val GENDER_KEYS: List<String> = listOf(GENDER, SEX)
    val BASE64_IMAGE_KEYS: List<String> = listOf(PORTRAIT, SIGNATURE)
}