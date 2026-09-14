package ru.fromchat.api.crypto.exteracrypt

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GCM_IV_SIZE = 12

@OptIn(DelicateCryptographyApi::class)
actual object ExteraCryptCrypto {
    private val provider get() = CryptographyProvider.Default
    private val aesGcm get() = provider.get(AES.GCM)

    actual suspend fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            val nonce = CryptographyRandom.nextBytes(GCM_IV_SIZE)
            val ciphertext = aesGcm
                .keyDecoder()
                .decodeFromByteArray(AES.Key.Format.RAW, key)
                .cipher(tagSize = 128.bits)
                .encryptWithIv(nonce, plaintext)
            nonce + ciphertext
        }

    actual suspend fun decrypt(key: ByteArray, blob: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            require(blob.size > GCM_IV_SIZE) { "Blob too short" }
            aesGcm
                .keyDecoder()
                .decodeFromByteArray(AES.Key.Format.RAW, key)
                .cipher(tagSize = 128.bits)
                .decryptWithIv(
                    blob.sliceArray(0 until GCM_IV_SIZE),
                    blob.sliceArray(GCM_IV_SIZE until blob.size),
                )
        }
}