// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.util.securearea

import android.util.Base64
import android.util.Log
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.CborMap
import org.multipaz.cbor.Nint
import org.multipaz.cbor.Tstr
import org.multipaz.crypto.EcSignature
import org.multipaz.securearea.KeyInfo

object AttestationEncoder {

    fun getAttestationCborEncoded(keyInfo: KeyInfo, signature: EcSignature): String? {
        return try {
            val x5cArray = keyInfo.attestation.certChain?.toDataItem() ?: return null
            val secLevel = mapSecurityLevel(keyInfo) // "tee" | "strongbox" | "software"

            val wscdInfo = CborMap(mutableMapOf(
                Tstr("security_level") to Tstr(secLevel)
            ))

            val wscdTopLevelBytes = Cbor.encode(
                CborMap(mutableMapOf(Tstr("security_level") to Tstr(secLevel)))
            )

            val attStmtMap = CborMap(mutableMapOf(
                Tstr("alg") to Nint(6uL),
                Tstr("sig") to Bstr(signature.toCoseEncoded()),
                Tstr("x5c") to x5cArray,
                Tstr("wscd_info") to wscdInfo
            ))

            val cborMap = CborMap(mutableMapOf(
                Tstr("fmt") to Tstr("android-key"),
                Tstr("attStmt") to attStmtMap,
                Tstr("wscd") to Bstr(wscdTopLevelBytes)
            ))

            val cborBytes = Cbor.encode(cborMap)
            Base64.encodeToString(cborBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        } catch (e: Exception) {
            Log.e("Attestation", "Encoding failed", e)
            null
        }
    }

    /** Must return lowercase values backend maps: "tee" | "strongbox" | "software". */
    private fun mapSecurityLevel(keyInfo: KeyInfo): String {
        val strongBox = try {
            keyInfo.javaClass.methods.firstOrNull { it.name == "isStrongBoxBacked" && it.parameterCount == 0 }
                ?.invoke(keyInfo) as? Boolean ?: false
        } catch (_: Throwable) { false }

        val insideHw = try {
            keyInfo.javaClass.methods.firstOrNull { it.name == "isInsideSecureHardware" && it.parameterCount == 0 }
                ?.invoke(keyInfo) as? Boolean ?: false
        } catch (_: Throwable) { false }

        return when {
            strongBox -> "strongbox"
            insideHw  -> "tee"
            else      -> "software"
        }
    }
}