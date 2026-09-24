// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.model

import com.google.gson.annotations.SerializedName

/**
 * @param randomString used for biometric validation
 * @param encryptedString encrypted with biometric cipher and base64 form
 * @param ivString used in biometric cipher in base64 form
 */

data class BiometricAuth(
    @SerializedName("random") val randomString: String,
    @SerializedName("encrypted") val encryptedString: String,
    @SerializedName("iv") val ivString: String,
)