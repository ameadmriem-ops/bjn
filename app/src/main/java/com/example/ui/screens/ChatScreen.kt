package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AboutAppDialog
import com.example.ui.components.AssistantMessageBubble
import com.example.ui.components.ChatBottomComposer
import com.example.ui.components.ChatSidebar
import com.example.ui.components.ChatWelcomeCenter
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SourcesInfoDialog
import com.example.ui.components.UserMessageBubble
import com.example.viewmodel.ChatUiState
import com.example.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    viewModel: SearchViewModel,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll to latest message when messages change
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChatSidebar(
                    language = uiState.language,
                    historyList = uiState.filteredHistory,
                    searchFilter = uiState.historySearchFilter,
                    onSearchFilterChanged = { viewModel.onHistorySearchFilterChanged(it) },
                    onNewChatClick = { viewModel.startNewChat() },
                    onHistoryItemClick = { item -> viewModel.loadHistoryConversation(item) },
                    onDeleteHistoryItem = { id -> viewModel.deleteHistoryItem(id) },
                    onOpenSettings = { viewModel.showSettings(true) },
                    onOpenSourcesInfo = { viewModel.showSourcesInfo(true) },
                    onOpenAbout = { viewModel.showAbout(true) },
                    onCloseSidebar = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Trusted Search",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.language == "ar") "0% ذكاء اصطناعي • مصادر موثقة" else "0% AI • Verified Sources",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("top_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Sidebar"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.startNewChat() },
                            modifier = Modifier.testTag("top_new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.showSettings(true) },
                            modifier = Modifier.testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                ChatBottomComposer(
                    inputQuery = uiState.inputQuery,
                    language = uiState.language,
                    isSearching = uiState.isSearching,
                    onQueryChanged = { viewModel.onInputQueryChanged(it) },
                    onSend = { viewModel.sendMessage(it) },
                    onVoiceClick = onVoiceClick,
                    onNewChatClick = { viewModel.startNewChat() },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.messages.isEmpty()) {
                    ChatWelcomeCenter(
                        language = uiState.language,
                        onSuggestionClick = { suggestion ->
                            viewModel.onInputQueryChanged(suggestion)
                            viewModel.sendMessage(suggestion)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            if (msg.isUser) {
                                UserMessageBubble(message = msg)
                            } else {
                                AssistantMessageBubble(message = msg, language = uiState.language)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            language = uiState.language,
            isDarkMode = uiState.isDarkMode,
            maxSources = uiState.maxSources,
            strictTrustOnly = uiState.strictTrustOnly,
            customApiUrl = uiState.customApiUrl,
            customApiKey = uiState.customApiKey,
            onLanguageChange = { viewModel.setLanguage(it) },
            onDarkModeToggle = { viewModel.setDarkMode(it) },
            onMaxSourcesChange = { viewModel.setMaxSources(it) },
            onStrictTrustChange = { viewModel.setStrictTrust(it) },
            onCustomApiSave = { url, key -> viewModel.setCustomApi(url, key) },
            onClearHistory = { viewModel.clearAllHistory() },
            onClearCache = { viewModel.clearCache() },
            onDismiss = { viewModel.showSettings(false) }
        )
    }

    if (uiState.showSourcesInfoDialog) {
        SourcesInfoDialog(
            language = uiState.language,
            onDismiss = { viewModel.showSourcesInfo(false) }
        )
    }

    if (uiState.showAboutDialog) {
        AboutAppDialog(
            language = uiState.language,
            onDismiss = { viewModel.showAbout(false) }
        )
    }
}
