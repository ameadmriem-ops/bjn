package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SearchRepository
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.AnswerStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.HowWeFoundItDetails
import com.example.data.model.SynthesizedAnswer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSearching: Boolean = false,
    val searchStep: String = "",
    val answer: SynthesizedAnswer? = null
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputQuery: String = "",
    val isSearching: Boolean = false,
    val historyList: List<SearchHistoryEntity> = emptyList(),
    val historySearchFilter: String = "",
    val language: String = "ar", // "ar", "en", "fr"
    val isDarkMode: Boolean = true, // Dark mode default as requested
    val maxSources: Int = 6,
    val strictTrustOnly: Boolean = false,
    val customApiUrl: String = "",
    val customApiKey: String = "",
    val toastMessage: String? = null,
    val showSettingsDialog: Boolean = false,
    val showSourcesInfoDialog: Boolean = false,
    val showAboutDialog: Boolean = false
) {
    val filteredHistory: List<SearchHistoryEntity>
        get() = if (historySearchFilter.isBlank()) {
            historyList
        } else {
            historyList.filter {
                it.query.contains(historySearchFilter, ignoreCase = true) ||
                        it.directAnswer.contains(historySearchFilter, ignoreCase = true)
            }
        }
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SearchRepository(application)
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getHistory().collectLatest { history ->
                _uiState.update { it.copy(historyList = history) }
            }
        }
    }

    fun onInputQueryChanged(newQuery: String) {
        _uiState.update { it.copy(inputQuery = newQuery) }
    }

    fun onHistorySearchFilterChanged(filter: String) {
        _uiState.update { it.copy(historySearchFilter = filter) }
    }

    fun startNewChat() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                inputQuery = "",
                isSearching = false
            )
        }
    }

    fun loadHistoryConversation(historyItem: SearchHistoryEntity) {
        val restoredAnswer = repository.restoreAnswerFromHistory(historyItem)
        val userMsg = ChatMessage(
            isUser = true,
            text = historyItem.query,
            timestamp = historyItem.timestamp - 1000
        )
        val assistantMsg = ChatMessage(
            isUser = false,
            text = restoredAnswer.directAnswer,
            timestamp = historyItem.timestamp,
            answer = restoredAnswer
        )
        _uiState.update {
            it.copy(
                messages = listOf(userMsg, assistantMsg),
                inputQuery = "",
                isSearching = false
            )
        }
    }

    fun sendMessage(promptText: String? = null) {
        val query = (promptText ?: _uiState.value.inputQuery).trim()
        if (query.isBlank() || _uiState.value.isSearching) return

        val userMessage = ChatMessage(
            isUser = true,
            text = query,
            timestamp = System.currentTimeMillis()
        )

        val placeholderMessage = ChatMessage(
            isUser = false,
            text = "",
            isSearching = true,
            searchStep = "",
            timestamp = System.currentTimeMillis() + 1
        )

        val updatedMessages = _uiState.value.messages + userMessage + placeholderMessage
        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputQuery = "",
                isSearching = true
            )
        }

        viewModelScope.launch {
            try {
                val answer = repository.executeSearch(
                    rawQuery = query,
                    maxSources = _uiState.value.maxSources,
                    strictTrustOnly = _uiState.value.strictTrustOnly
                )

                _uiState.update { state ->
                    val currentList = state.messages.toMutableList()
                    val lastIdx = currentList.indexOfLast { !it.isUser && it.isSearching }
                    val finalMsg = ChatMessage(
                        isUser = false,
                        text = answer.directAnswer,
                        isSearching = false,
                        answer = answer,
                        timestamp = System.currentTimeMillis()
                    )
                    if (lastIdx != -1) {
                        currentList[lastIdx] = finalMsg
                    } else {
                        currentList.add(finalMsg)
                    }
                    state.copy(messages = currentList, isSearching = false)
                }
            } catch (e: Exception) {
                val errorMsg = when (_uiState.value.language) {
                    "ar" -> "حدث خطأ غير متوقع أثناء معالجة السؤال."
                    "fr" -> "Une erreur inattendue s'est produite lors du traitement."
                    else -> "An unexpected error occurred while processing."
                }
                _uiState.update { state ->
                    val currentList = state.messages.toMutableList()
                    val lastIdx = currentList.indexOfLast { !it.isUser && it.isSearching }
                    val fallbackAnswer = SynthesizedAnswer(
                        query = query,
                        language = state.language,
                        status = AnswerStatus.ERROR,
                        directAnswer = errorMsg,
                        confidenceLevel = ConfidenceLevel.LOW,
                        confidencePercentage = 0,
                        sources = emptyList(),
                        howWeFoundIt = HowWeFoundItDetails(0, 0, 0, 0, 0, "Error", emptyList())
                    )
                    val finalMsg = ChatMessage(
                        isUser = false,
                        text = errorMsg,
                        isSearching = false,
                        answer = fallbackAnswer,
                        timestamp = System.currentTimeMillis()
                    )
                    if (lastIdx != -1) {
                        currentList[lastIdx] = finalMsg
                    } else {
                        currentList.add(finalMsg)
                    }
                    state.copy(messages = currentList, isSearching = false)
                }
            }
        }
    }

    fun onVoiceResult(spokenText: String) {
        if (spokenText.isNotBlank()) {
            sendMessage(spokenText)
        }
    }

    fun setLanguage(lang: String) {
        _uiState.update { it.copy(language = lang) }
    }

    fun setDarkMode(isDark: Boolean) {
        _uiState.update { it.copy(isDarkMode = isDark) }
    }

    fun setMaxSources(count: Int) {
        _uiState.update { it.copy(maxSources = count) }
    }

    fun setStrictTrust(strict: Boolean) {
        _uiState.update { it.copy(strictTrustOnly = strict) }
    }

    fun setCustomApi(url: String, key: String) {
        repository.customApiUrl = url
        repository.customApiKey = key
        _uiState.update { it.copy(customApiUrl = url, customApiKey = key) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update {
                it.copy(
                    toastMessage = if (it.language == "ar") "تم مسح المحادثات السابقة" else "Search history cleared"
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _uiState.update {
                it.copy(
                    toastMessage = if (it.language == "ar") "تم مسح الذاكرة المؤقتة" else "Cache cleared"
                )
            }
        }
    }

    fun showSettings(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun showSourcesInfo(show: Boolean) {
        _uiState.update { it.copy(showSourcesInfoDialog = show) }
    }

    fun showAbout(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
