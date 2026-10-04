package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.screens.ChatScreen
import com.example.ui.theme.TrustedSearchTheme
import com.example.viewmodel.SearchViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }

            // Voice Search Speech Recognizer Launcher
            val speechLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = matches?.firstOrNull() ?: ""
                    if (spokenText.isNotBlank()) {
                        viewModel.onVoiceResult(spokenText)
                    }
                }
            }

            fun launchVoiceSearch() {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        val langTag = when (uiState.language) {
                            "ar" -> "ar-SA"
                            "fr" -> "fr-FR"
                            else -> "en-US"
                        }
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                        putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            if (uiState.language == "ar") "تحدث بسؤالك..." else "Speak your question..."
                        )
                    }
                    speechLauncher.launch(intent)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        if (uiState.language == "ar") "البحث الصوتي غير متوفر على هذا الجهاز" else "Voice recognition not available on this device",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            LaunchedEffect(uiState.toastMessage) {
                uiState.toastMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearToast()
                }
            }

            val layoutDirection = if (uiState.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                TrustedSearchTheme(darkTheme = uiState.isDarkMode) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { _ ->
                        ChatScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onVoiceClick = { launchVoiceSearch() }
                        )
                    }
                }
            }
        }
    }
}
