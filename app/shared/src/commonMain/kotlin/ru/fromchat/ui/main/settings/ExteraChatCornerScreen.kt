package ru.fromchat.ui.main.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.pr0gramm3r101.components.Category
import com.pr0gramm3r101.components.SwitchListItem
import com.pr0gramm3r101.utils.conditional
import org.jetbrains.compose.resources.stringResource
import ru.fromchat.Res
import ru.fromchat.config.Settings
import ru.fromchat.exterachat_hide_contacts
import ru.fromchat.exterachat_hide_contacts_d
import ru.fromchat.exterachat_hide_profile
import ru.fromchat.exterachat_hide_profile_d
import ru.fromchat.settings_category_exterachat
import ru.fromchat.ui.components.Text

/**
 * Reactive mirrors of [Settings.hideContacts] / [Settings.hideProfile] so the main bottom
 * navigation (composed outside the settings back stack) recomposes when a toggle changes
 * here — especially on desktop list–detail, where the list pane stays composed.
 */
var hideContactsUiState by mutableStateOf(runCatching { Settings.hideContacts }.getOrDefault(false))
var hideProfileUiState by mutableStateOf(runCatching { Settings.hideProfile }.getOrDefault(false))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExteraChatCornerScreen(onBack: () -> Unit) {
    val useCollapsing = settingsDetailUseCollapsingTopBar()
    val scrollBehavior = rememberSettingsCollapsingScrollBehavior()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = { Text(stringResource(Res.string.settings_category_exterachat)) },
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxWidth()
                .conditional(useCollapsing) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                }
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            Category(
                Modifier.padding(top = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                SwitchListItem(
                    headline = stringResource(Res.string.exterachat_hide_contacts),
                    supportingText = stringResource(Res.string.exterachat_hide_contacts_d),
                    checked = hideContactsUiState,
                    onCheckedChange = {
                        hideContactsUiState = it
                        Settings.hideContacts = it
                    },
                    divider = true,
                    leadingContent = {
                        Icon(Icons.Filled.Contacts, null)
                    }
                )
                SwitchListItem(
                    headline = stringResource(Res.string.exterachat_hide_profile),
                    supportingText = stringResource(Res.string.exterachat_hide_profile_d),
                    checked = hideProfileUiState,
                    onCheckedChange = {
                        hideProfileUiState = it
                        Settings.hideProfile = it
                    },
                    leadingContent = {
                        Icon(Icons.Filled.Person, null)
                    }
                )
            }
        }
    }
}