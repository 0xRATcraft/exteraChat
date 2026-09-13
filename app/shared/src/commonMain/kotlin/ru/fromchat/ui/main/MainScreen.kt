package ru.fromchat.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.pr0gramm3r101.utils.WindowWidthSizeClass
import com.pr0gramm3r101.utils.currentWindowAdaptiveInfo
import com.pr0gramm3r101.utils.widthSizeClass
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ru.fromchat.Res
import ru.fromchat.api.ApiClient
import ru.fromchat.chats
import ru.fromchat.contacts
import ru.fromchat.desktop.DesktopMenuCommand
import ru.fromchat.desktop.DesktopMenuCommands
import ru.fromchat.profile
import ru.fromchat.settings
import ru.fromchat.ui.LocalNavController
import ru.fromchat.ui.chat.rememberChatSurfaceContainerHazeStyle
import ru.fromchat.ui.chat.panels.dm.navigateToDmChat
import ru.fromchat.ui.components.BackHandler
import ru.fromchat.ui.components.FromChatSnackbarHost
import ru.fromchat.ui.components.LocalPaneHazeState
import ru.fromchat.ui.components.Text
import ru.fromchat.ui.extraStatusBars
import ru.fromchat.ui.main.chats.ChatContextMenuOverlayController
import ru.fromchat.ui.main.chats.ChatContextMenuOverlayHost
import ru.fromchat.ui.main.chats.ChatsSearchScreen
import ru.fromchat.ui.main.chats.ChatsTab
import ru.fromchat.ui.main.settings.SettingsTab
import ru.fromchat.ui.main.settings.hideContactsUiState
import ru.fromchat.ui.main.settings.hideProfileUiState
import ru.fromchat.ui.profile.ProfileScreen

const val MAIN_PAGE_CHATS = 0
const val MAIN_PAGE_CONTACTS = 1
const val MAIN_PAGE_SETTINGS = 2
const val MAIN_PAGE_PROFILE = 3

private const val PAGE_CHATS = MAIN_PAGE_CHATS
private const val PAGE_CONTACTS = MAIN_PAGE_CONTACTS
private const val PAGE_SETTINGS = MAIN_PAGE_SETTINGS
private const val PAGE_PROFILE = MAIN_PAGE_PROFILE

/**
 * Opens the signed-in user's profile in the list–detail pane (large screens).
 *
 * File-private (not a local fun inside [MainScreen]) so desktop JVM incremental runs do not
 * chase missing nested classes like `MainScreenKt$MainScreen$openOwnProfileInDetailPane$1`.
 */
private fun openOwnProfileInDetailPane(navController: NavController) {
    val userId = ApiClient.user?.id?.takeIf { it > 0 } ?: return
    navController.navigateReplacingMainDetail(route = "profile/$userId")
}

/**
 * Switches the main hub pager page. On large screens, Profile is a button: selects Settings
 * in the list and opens own profile in the detail pane (Profile is never the active tab).
 *
 * Kept file-private (not nested in [MainScreen]) for the same desktop JVM class-loading reason
 * as [openOwnProfileInDetailPane] / ChatRowAvatar gesture helpers.
 *
 * Skips pager animation and detail navigation when the destination is already showing.
 */
private fun selectMainPage(
    page: Int,
    widthClass: WindowWidthSizeClass,
    visiblePages: List<Int>,
    scope: CoroutineScope,
    pagerState: PagerState,
    settingsDetailNavController: NavController,
) {
    when {
        page == PAGE_PROFILE && widthClass != WindowWidthSizeClass.COMPACT -> {
            val settingsIndex = visiblePages.indexOf(PAGE_SETTINGS)
            val alreadyOnSettingsList = settingsIndex >= 0 && pagerState.currentPage == settingsIndex
            val ownUserId = ApiClient.user?.id?.takeIf { it > 0 }
            val alreadyShowingOwnProfile =
                ownUserId != null &&
                    settingsDetailNavController.isCurrentMainDetailRoute("profile/$ownUserId")
            if (alreadyOnSettingsList && alreadyShowingOwnProfile) return
            if (!alreadyOnSettingsList && settingsIndex >= 0) {
                scope.launch { pagerState.animateScrollToPage(settingsIndex) }
            }
            if (!alreadyShowingOwnProfile) {
                openOwnProfileInDetailPane(settingsDetailNavController)
            }
        }
        else -> {
            val pageIndex = visiblePages.indexOf(page)
            if (pageIndex < 0) return
            // Desktop list–detail: do not pop the chat (or settings) stack — each tab
            // keeps its own detail host, so switching tabs must not wipe the other tab.
            if (pagerState.currentPage == pageIndex) return
            scope.launch { pagerState.animateScrollToPage(pageIndex) }
        }
    }
}

/**
 * @param embeddedInListDetail When true, this screen is the left pane of a list–detail layout.
 * Nav chrome stays inside this pane (bottom bar), never spanning the window.
 * @param initialPage Pager page when showing the full MainScreen.
 * @param forceSettingsTab When true, scrolls the pager to settings (settings/profile detail open).
 * Does not snap back when cleared — avoids fighting user tab changes.
 * @param onPageChanged Invoked when the pager settles on a page (for list–detail empty panes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    snackbarHostState: SnackbarHostState? = null,
    embeddedInListDetail: Boolean = false,
    initialPage: Int = PAGE_CHATS,
    forceSettingsTab: Boolean = false,
    onPageChanged: (Int) -> Unit = {},
) {
    val effectiveSnackbarHostState = snackbarHostState ?: remember { SnackbarHostState() }
    val navController = LocalNavController.current
    val settingsDetailNavController =
        LocalDesktopSettingsNavController.current ?: navController

    // Bottom-nav pages derive from the "Уголок exteraChat" toggles. Keep the pager in sync
    // with the visible page order so hidden tabs are gone, not just unselected.
    val visiblePages = remember(hideContactsUiState, hideProfileUiState) {
        buildList {
            add(PAGE_CHATS)
            if (!hideContactsUiState) add(PAGE_CONTACTS)
            add(PAGE_SETTINGS)
            if (!hideProfileUiState) add(PAGE_PROFILE)
        }
    }
    val initialPageId = visiblePages.firstOrNull { it == initialPage } ?: PAGE_CHATS
    // Key on the corner-screen toggles: when a tab gets hidden/reshown the PagerState is
    // recreated on the correct page. Without this, rememberPagerState's saveable saver restores
    // the old pager index after popping the settings back stack, which then points to a
    // different (or missing) tab once the visible-page list shrinks.
    val pagerState = key(hideContactsUiState to hideProfileUiState) {
        rememberPagerState(
            initialPage = visiblePages.indexOf(initialPageId),
            pageCount = { visiblePages.size },
        )
    }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val navBarHazeState = rememberHazeState()
    val navBarHazeStyle = rememberChatSurfaceContainerHazeStyle()
    val paneHazeState = LocalPaneHazeState.current
    val contextMenuHazeState = paneHazeState ?: rememberHazeState()
    val chatContextMenuOverlay = remember { ChatContextMenuOverlayController() }
    var chatListSelectionRequestId by remember { mutableStateOf(0L) }
    var paneSearchOpen by remember { mutableStateOf(false) }

    fun openConversationSearch() {
        if (embeddedInListDetail) {
            paneSearchOpen = true
        } else {
            navController.navigate("search/conversations") {
                launchSingleTop = true
            }
        }
    }

    val widthClass = currentWindowAdaptiveInfo().widthSizeClass

    LaunchedEffect(forceSettingsTab, visiblePages) {
        if (forceSettingsTab) {
            val index = visiblePages.indexOf(PAGE_SETTINGS)
            if (index >= 0 && pagerState.currentPage != index) {
                pagerState.scrollToPage(index)
            }
        }
    }

    LaunchedEffect(visiblePages) {
        DesktopMenuCommands.commands.collect { command ->
            when (command) {
                DesktopMenuCommand.NewChat,
                DesktopMenuCommand.SearchConversations,
                -> {
                    if (pagerState.currentPage != visiblePages.indexOf(PAGE_CHATS)) {
                        pagerState.scrollToPage(visiblePages.indexOf(PAGE_CHATS))
                    }
                    openConversationSearch()
                }
                DesktopMenuCommand.EnterChatListSelection -> {
                    if (paneSearchOpen) {
                        paneSearchOpen = false
                    }
                    if (navController.currentBackStackEntry?.destination?.route ==
                        "search/conversations"
                    ) {
                        navController.popBackStack()
                    }
                    if (pagerState.currentPage != visiblePages.indexOf(PAGE_CHATS)) {
                        pagerState.scrollToPage(visiblePages.indexOf(PAGE_CHATS))
                    }
                    chatListSelectionRequestId += 1
                }
                DesktopMenuCommand.OpenAbout -> {
                    if (pagerState.currentPage != visiblePages.indexOf(PAGE_SETTINGS)) {
                        pagerState.scrollToPage(visiblePages.indexOf(PAGE_SETTINGS))
                    }
                }
            }
        }
    }

    val selectedPageIndex = pagerState.currentPage
    val selectedPageId = visiblePages.getOrNull(selectedPageIndex) ?: PAGE_CHATS

    LaunchedEffect(selectedPageId) {
        onPageChanged(selectedPageId)
    }
    val isChatsPage = selectedPageId == PAGE_CHATS
    val chatMenuBlurProgress = chatContextMenuOverlay.blurProgress

    // List–detail shell already pads/consumes safeDrawing ∪ extraStatusBars; do not re-apply top.
    val statusBarTop = if (embeddedInListDetail) {
        0.dp
    } else {
        with(density) {
            WindowInsets.extraStatusBars.getTop(this).toDp()
        }
    }
    var bottomChromeHeightDp by remember { mutableStateOf(0.dp) }

    val mainChromeInsets = remember(statusBarTop, bottomChromeHeightDp) {
        MainChromeInsets(
            top = statusBarTop,
            bottom = bottomChromeHeightDp,
        )
    }

    val chatsLabel = stringResource(Res.string.chats)
    val contactsLabel = stringResource(Res.string.contacts)
    val settingsLabel = stringResource(Res.string.settings)
    val profileLabel = stringResource(Res.string.profile)

    BoxWithConstraints(
        Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(contextMenuHazeState),
        ) {
            Scaffold(
                snackbarHost = {
                    FromChatSnackbarHost(
                        hostState = effectiveSnackbarHostState,
                        modifier = Modifier.padding(bottom = mainChromeInsets.bottom),
                    )
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                CompositionLocalProvider(LocalMainChromeInsets provides mainChromeInsets) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(navBarHazeState),
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = widthClass == WindowWidthSizeClass.COMPACT,
                        ) { page ->
                            when (val pageId = visiblePages.getOrNull(page) ?: PAGE_CHATS) {
                                PAGE_CHATS -> ChatsTab(
                                    isVisible = isChatsPage,
                                    onOpenSearch = { openConversationSearch() },
                                    chatContextMenuOverlay = chatContextMenuOverlay,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    enterSelectionRequestId = chatListSelectionRequestId,
                                )
                                PAGE_CONTACTS -> ContactsTab()
                                PAGE_SETTINGS -> SettingsTab()
                                PAGE_PROFILE -> {
                                    ProfileScreen(
                                        userId = ApiClient.user?.id,
                                        onBack = {},
                                        onChat = { _ -> },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .mainPagerBottomInset(),
                                        onOpenSettings = {
                                            val settingsIndex =
                                                visiblePages.indexOf(PAGE_SETTINGS)
                                            if (settingsIndex >= 0) {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(settingsIndex)
                                                }
                                            }
                                        },
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                    .imePadding()
                    .onSizeChanged { size ->
                        val measured = with(density) { size.height.toDp() }
                        if (measured != bottomChromeHeightDp) {
                            bottomChromeHeightDp = measured
                        }
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .hazeBlur(input = HazeInput.Backdrop(navBarHazeState), style = navBarHazeStyle),
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                ) {
                    visiblePages.forEach { pageId ->
                        when (pageId) {
                            PAGE_CHATS -> NavigationBarItem(
                                selected = selectedPageId == PAGE_CHATS,
                                onClick = {
                                    selectMainPage(
                                        page = PAGE_CHATS,
                                        widthClass = widthClass,
                                        visiblePages = visiblePages,
                                        scope = scope,
                                        pagerState = pagerState,
                                        settingsDetailNavController = settingsDetailNavController,
                                    )
                                },
                                label = { Text(chatsLabel) },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                    )
                                },
                            )
                            PAGE_CONTACTS -> NavigationBarItem(
                                selected = selectedPageId == PAGE_CONTACTS,
                                onClick = {
                                    selectMainPage(
                                        page = PAGE_CONTACTS,
                                        widthClass = widthClass,
                                        visiblePages = visiblePages,
                                        scope = scope,
                                        pagerState = pagerState,
                                        settingsDetailNavController = settingsDetailNavController,
                                    )
                                },
                                label = { Text(contactsLabel) },
                                icon = {
                                    Icon(Icons.Filled.Contacts, contentDescription = null)
                                },
                            )
                            PAGE_SETTINGS -> NavigationBarItem(
                                selected = selectedPageId == PAGE_SETTINGS,
                                onClick = {
                                    selectMainPage(
                                        page = PAGE_SETTINGS,
                                        widthClass = widthClass,
                                        visiblePages = visiblePages,
                                        scope = scope,
                                        pagerState = pagerState,
                                        settingsDetailNavController = settingsDetailNavController,
                                    )
                                },
                                label = { Text(settingsLabel) },
                                icon = {
                                    Icon(Icons.Filled.Settings, contentDescription = null)
                                },
                            )
                            PAGE_PROFILE -> NavigationBarItem(
                                // Two-pane: Profile is a shortcut button — never selected;
                                // Settings stays highlighted while own profile is open in the
                                // detail pane.
                                selected = widthClass == WindowWidthSizeClass.COMPACT &&
                                    selectedPageId == PAGE_PROFILE,
                                onClick = {
                                    selectMainPage(
                                        page = PAGE_PROFILE,
                                        widthClass = widthClass,
                                        visiblePages = visiblePages,
                                        scope = scope,
                                        pagerState = pagerState,
                                        settingsDetailNavController = settingsDetailNavController,
                                    )
                                },
                                label = { Text(profileLabel) },
                                icon = {
                                    Icon(Icons.Filled.Person, contentDescription = null)
                                },
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }

        if (chatMenuBlurProgress > 0f) {
            ChatContextMenuBlurLayer(
                hazeState = contextMenuHazeState,
                blurProgress = chatMenuBlurProgress,
                modifier = Modifier.zIndex(2f),
            )
        }

        ChatContextMenuOverlayHost(
            controller = chatContextMenuOverlay,
            screenWidthPx = constraints.maxWidth,
            screenHeightPx = constraints.maxHeight,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(3f),
        )

        if (embeddedInListDetail) {
            BackHandler(paneSearchOpen) { paneSearchOpen = false }
            AnimatedVisibility(
                visible = paneSearchOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f),
            ) {
                val chatsNav = LocalDesktopChatsNavController.current ?: navController
                ChatsSearchScreen(
                    onBack = { paneSearchOpen = false },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this,
                    onOpenProfile = { userId: Int ->
                        if (userId == 0) return@ChatsSearchScreen
                        paneSearchOpen = false
                        chatsNav.navigateReplacingMainDetail("profile/$userId")
                    },
                    onOpenConversation = { userId: Int ->
                        if (userId == 0) return@ChatsSearchScreen
                        paneSearchOpen = false
                        chatsNav.navigateToDmChat(userId)
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatContextMenuBlurLayer(
    hazeState: HazeState,
    blurProgress: Float,
    modifier: Modifier = Modifier,
) {
    val progress = blurProgress.coerceIn(0f, 1f)
    val blurRadius = 12.dp * progress
    if (blurRadius <= 0.dp) return

    // Opaque enough background so rounded chrome corners don't stay razor-sharp while
    // interiors look frosted (Transparent + no tint left original AA edges unblurred).
    // In-tree under the overlay (zIndex). Never use a Popup here: platform popups stack above
    // the sharp row/menu, blur them, and intercept dismiss taps.
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val tint = HazeColorEffect.tint(surface.copy(alpha = 0.4f * progress))
    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeBlur(
                input = HazeInput.Backdrop(hazeState),
                style = HazeBlurStyle {
                    blurRadius(blurRadius)
                    backgroundColor(surface)
                    colorEffects(listOf(tint))
                    noiseFactor(0f)
                    fallbackColorEffect(tint)
                },
            ),
    )
}
