package com.nexus.ai

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.ai.ui.components.AppDrawer
import com.nexus.ai.ui.components.PuterWebView
import com.nexus.ai.ui.screens.ChatScreen
import com.nexus.ai.ui.screens.LoginScreen
import com.nexus.ai.ui.screens.SettingsScreen
import com.nexus.ai.ui.theme.NexusTheme
import com.nexus.ai.ui.viewmodel.ChatViewModel
import com.nexus.ai.ui.viewmodel.DrawerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NexusApp()
                }
            }
        }
    }
}

@Composable
fun NexusApp() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var isWebViewVisible by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    
    val chatViewModel: ChatViewModel = viewModel()
    val drawerViewModel: DrawerViewModel = viewModel()
    var currentScreen by remember { mutableStateOf("chat") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (chatViewModel.currentChatId == null) chatViewModel.startNewChat()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoggedIn) {
            LoginScreen(
                onLoginClick = { 
                    isWebViewVisible = true 
                    webView?.evaluateJavascript("loginPuter()", null) 
                }
            )
        } else {
            if (currentScreen == "settings") {
                SettingsScreen(onBackClick = { currentScreen = "chat" })
            } else {
                AppDrawer(
                    drawerState = drawerState,
                    viewModel = drawerViewModel,
                    onNavigateToSettings = { scope.launch { drawerState.close() }; currentScreen = "settings" },
                    onNewChatClick = { scope.launch { drawerState.close() }; chatViewModel.startNewChat() },
                    onChatSelect = { id -> chatViewModel.loadChat(id) }
                ) {
                    ChatScreen(
                        viewModel = chatViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSendMessage = { message, skill, sysPrompt ->
                            val safeMsg = message.replace("'", "\\'")
                            webView?.evaluateJavascript("askPuter('$safeMsg', 'Auto', '$sysPrompt')", null)
                        }
                    )
                }
            }
        }

        PuterWebView(
            isVisible = isWebViewVisible,
            onWebViewCreated = { webView = it },
            onPopupCreated = { popupWebView = it },
            onLoginSuccess = { 
                isWebViewVisible = false
                isLoggedIn = true 
            },
            onLoginError = { isWebViewVisible = false },
            onResponse = { response -> chatViewModel.addAiMessage(response) },
            onModelsLoaded = { },
            onImageGenerated = { url -> chatViewModel.addImageMessage(url) },
            onVideoAnalyzedCallback = { json -> chatViewModel.onVideoAnalyzedReceived(json) }
        )

        if (popupWebView != null) {
            Dialog(
                onDismissRequest = { 
                    popupWebView?.destroy()
                    popupWebView = null 
                }, 
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AndroidView(
                    factory = { popupWebView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
