package ru.fromchat.api.crypto.exteracrypt

/**
 * AES-256-GCM primitive used for exteracrypt "user key" encryption.
 *
 * [encrypt] returns a combined blob: `nonce(12) || ciphertext || auth_tag(16)`.
 * [decrypt] expects that exact combined layout and throws on auth failure.
 */
expect object ExteraCryptCrypto {
    suspend fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray

    suspend fun decrypt(key: ByteArray, blob: ByteArray): ByteArray
}