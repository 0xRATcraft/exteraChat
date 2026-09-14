package ru.fromchat.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.fromchat.api.createPlatformHttpClient

enum class ExteraBadgeType {
    Developer,
    Supporter,
    Clown,
    Verified,
}

internal data class ExteraBadges(
    val developer: Boolean = false,
    val supporter: Boolean = false,
    val clown: Boolean = false,
    val verified: Boolean = false,
) {
    val asList: List<ExteraBadgeType>
        get() = buildList {
            if (developer) add(ExteraBadgeType.Developer)
            if (supporter) add(ExteraBadgeType.Supporter)
            if (clown) add(ExteraBadgeType.Clown)
            if (verified) add(ExteraBadgeType.Verified)
        }
}

internal object ExteraBadgeChecker {
    private val http: HttpClient by lazy { createPlatformHttpClient() }
    private val mutex = Mutex()
    private val cache = mutableMapOf<ExteraBadgeType, Set<Int>>()

    suspend fun hasBadge(userId: Int, type: ExteraBadgeType): Boolean =
        userId in idsFor(type)

    private suspend fun idsFor(type: ExteraBadgeType): Set<Int> = mutex.withLock {
        cache[type]?.let { return@withLock it }
        val fetched = fetchIds(type)
        if (fetched != null) {
            cache[type] = fetched
        }
        fetched.orEmpty()
    }

    private suspend fun fetchIds(type: ExteraBadgeType): Set<Int>? = runCatching {
        http.get(urlFor(type)).bodyAsText()
            .lineSequence()
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }.getOrNull()

    private fun urlFor(type: ExteraBadgeType): String = when (type) {
        ExteraBadgeType.Developer ->
            "https://gitverse.ru/api/repos/ratcraftik/data/raw/branch/master/exteraChatDev.txt"
        ExteraBadgeType.Supporter ->
            "https://gitverse.ru/api/repos/ratcraftik/data/raw/branch/master/exteraChatSupporter.txt"
        ExteraBadgeType.Clown ->
            "https://gitverse.ru/api/repos/ratcraftik/data/raw/branch/master/exteraChatSecretBadge.txt"
        ExteraBadgeType.Verified ->
            "https://gitverse.ru/api/repos/ratcraftik/data/raw/branch/master/exteraChatVerify.txt"
    }
}

@Composable
internal fun rememberExteraBadges(userId: Int?): ExteraBadges {
    var badges by remember(userId) { mutableStateOf(ExteraBadges()) }
    LaunchedEffect(userId) {
        badges = if (userId == null || userId <= 0) {
            ExteraBadges()
        } else {
            ExteraBadges(
                developer = ExteraBadgeChecker.hasBadge(userId, ExteraBadgeType.Developer),
                supporter = ExteraBadgeChecker.hasBadge(userId, ExteraBadgeType.Supporter),
                clown = ExteraBadgeChecker.hasBadge(userId, ExteraBadgeType.Clown),
                verified = ExteraBadgeChecker.hasBadge(userId, ExteraBadgeType.Verified),
            )
        }
    }
    return badges
}