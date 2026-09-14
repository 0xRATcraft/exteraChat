package ru.fromchat.api.crypto.exteracrypt

import com.pr0gramm3r101.utils.crypto.Base64
import korlibs.crypto.SHA256

/**
 * User-facing "short key" message encryption (exteracrypt).
 *
 * - Key derivation: SHA-256 of the user key -> 32-byte AES key.
 * - AES-256-GCM with a fresh random 12-byte nonce per message.
 * - Blob layout: nonce(12) || ciphertext || auth_tag(16).
 * - Transport: always "naked" — `exteracrypt:v1:` + Base64(blob).
 * - Zero-width transport from older builds is still supported on the decode path for back-compat.
 */
object ExteraCrypt {
    const val PREFIX = "exteracrypt:v1:"

    private const val GCM_IV_SIZE = 12
    private const val GCM_TAG_SIZE = 16

    private const val ZW_00 = '\u200B' // ZERO WIDTH SPACE
    private const val ZW_01 = '\u200C' // ZERO WIDTH NON-JOINER
    private const val ZW_10 = '\u200D' // ZERO WIDTH JOINER
    private const val ZW_11 = '\uFEFF' // ZERO WIDTH NO-BREAK SPACE

    private val ZERO_WIDTH_CHARS = setOf(ZW_00, ZW_01, ZW_10, ZW_11)

    /** Derives the 256-bit AES key from the short user-facing key. */
    fun deriveKey(key: String): ByteArray = SHA256.digest(key.encodeToByteArray()).bytes

    /** Returns true when [content] is an exteracrypt payload (naked marker or legacy zero-width chars). */
    fun looksEncrypted(content: String): Boolean =
        content.startsWith(PREFIX) || content.any { it in ZERO_WIDTH_CHARS }

    /** Encrypts [plaintext] with the user key into a naked `exteracrypt:v1:` payload. */
    suspend fun encrypt(plaintext: String, userKey: String): String {
        val blob = ExteraCryptCrypto.encrypt(deriveKey(userKey), plaintext.encodeToByteArray())
        return PREFIX + Base64.encode(blob)
    }

    /** Attempts to decrypt [content]. Returns the plaintext or null when it isn't exteracrypt / auth fails. */
    suspend fun tryDecrypt(content: String, userKey: String): String? {
        if (userKey.isBlank()) return null
        val blob = extractBlob(content) ?: return null
        return runCatching { ExteraCryptCrypto.decrypt(deriveKey(userKey), blob) }
            .getOrNull()
            ?.decodeToString()
    }

    private fun extractBlob(content: String): ByteArray? {
        if (content.startsWith(PREFIX)) {
            return try {
                Base64.decode(content.removePrefix(PREFIX).trim())
            } catch (_: Throwable) {
                null
            }
        }
        // Legacy zero-width transport (produced by older builds) — kept for back-compat.
        val bits = ArrayList<Int>(content.length * 2)
        for (c in content) {
            when (c) {
                ZW_00 -> { bits.add(0); bits.add(0) }
                ZW_01 -> { bits.add(0); bits.add(1) }
                ZW_10 -> { bits.add(1); bits.add(0) }
                ZW_11 -> { bits.add(1); bits.add(1) }
                else -> Unit
            }
        }
        if (bits.size < GCM_IV_SIZE + GCM_TAG_SIZE || bits.size % 8 != 0) return null
        val bytes = ByteArray(bits.size / 8)
        for (i in bytes.indices) {
            var value = 0
            for (j in 0 until 8) {
                value = (value shl 1) or bits[i * 8 + j]
            }
            bytes[i] = value.toByte()
        }
        return bytes
    }
}