// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.util.crypto

import org.multipaz.crypto.EcSignature

/**
 * Encode ECDSA signature (r,s) to ASN.1 DER.
 */
fun EcSignature.toAsn1Der(): ByteArray {
    fun encodeLength(len: Int): ByteArray =
        if (len < 0x80) byteArrayOf(len.toByte())
        else {
            // long-form length
            val bytes = buildList<Byte> {
                var v = len
                val stack = ArrayDeque<Byte>()
                while (v > 0) {
                    stack.addFirst((v and 0xFF).toByte())
                    v = v ushr 8
                }
                addAll(stack)
            }.toByteArray()
            byteArrayOf((0x80 or bytes.size).toByte()) + bytes
        }

    fun trimLeadingZeros(b: ByteArray): ByteArray {
        var i = 0
        while (i < b.size - 1 && b[i] == 0.toByte()) i++
        return b.copyOfRange(i, b.size)
    }

    fun encodeInteger(x: ByteArray): ByteArray {
        var v = trimLeadingZeros(x)
        if (v.isEmpty()) v = byteArrayOf(0)
        // If highest bit set, prepend 0x00 to keep it positive
        if ((v[0].toInt() and 0x80) != 0) v = byteArrayOf(0) + v
        val len = encodeLength(v.size)
        return byteArrayOf(0x02) + len + v
    }

    val rEnc = encodeInteger(r)
    val sEnc = encodeInteger(s)
    val seqContent = rEnc + sEnc
    val seqLen = encodeLength(seqContent.size)
    return byteArrayOf(0x30) + seqLen + seqContent
}