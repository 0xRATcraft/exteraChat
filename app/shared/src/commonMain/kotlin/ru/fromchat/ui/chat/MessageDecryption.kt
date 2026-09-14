package ru.fromchat.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.fromchat.api.crypto.exteracrypt.ExteraCrypt
import ru.fromchat.config.Settings

private val decryptedCache = HashMap<String, String>()

private fun cacheKey(userKey: String, content: String): String = "$userKey\u001F$content"

private fun cacheResult(key: String, display: String) {
    synchronized(decryptedCache) {
        decryptedCache[key] = display
    }
}

private fun cachedResult(key: String): String? = synchronized(decryptedCache) { decryptedCache[key] }

@Composable
fun rememberDecryptedMessageContent(content: String): String {
    if (!ExteraCrypt.looksEncrypted(content)) return content

    val enabled = remember { Settings.encryptionEnabled }
    val userKey = remember { Settings.getEncryptionKey().trim() }
    if (!enabled || userKey.length < Settings.MIN_ENCRYPTION_KEY_LENGTH) return content

    cachedResult(cacheKey(userKey, content))?.let { return it }

    var display by remember(content) { mutableStateOf(content) }
    LaunchedEffect(content) {
        val decrypted = ExteraCrypt.tryDecrypt(content, userKey)
        val finalText = if (decrypted != null) "🔒 $decrypted" else content
        cacheResult(cacheKey(userKey, content), finalText)
        display = finalText
    }
    return display
}

/**
 * Synchronous best-effort lookup of the decrypted text for copy / edit handlers.
 * Returns the raw [content] when no cached decryption exists (the renderer warms the cache).
 */
fun syncDecryptedText(content: String): String {
    val userKey = if (Settings.encryptionEnabled) Settings.getEncryptionKey().trim() else ""
    if (userKey.isBlank()) return content
    return cachedResult(cacheKey(userKey, content))?.removePrefix("🔒 ") ?: content
}
