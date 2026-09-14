package ru.fromchat.api.crypto.exteracrypt

import ru.fromchat.api.crypto.backup.BackupCryptoPlatform

private const val GCM_IV_SIZE = 12

actual object ExteraCryptCrypto {
    actual suspend fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray {
        val (nonce, ciphertext) = BackupCryptoPlatform.aesGcmEncrypt(key, plaintext)
        return nonce + ciphertext
    }

    actual suspend fun decrypt(key: ByteArray, blob: ByteArray): ByteArray {
        require(blob.size > GCM_IV_SIZE) { "Blob too short" }
        return BackupCryptoPlatform.aesGcmDecrypt(
            key,
            blob.sliceArray(0 until GCM_IV_SIZE),
            blob.sliceArray(GCM_IV_SIZE until blob.size),
        )
    }
}