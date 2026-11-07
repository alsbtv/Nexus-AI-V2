package com.nexus.ai

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.ai.ui.components.AppDrawer
import com.nexus.ai.ui.components.PuterWebView
import com.nexus.ai.ui.screens.ChatScreen
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
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("NexusPrefs", Context.MODE_PRIVATE)
    
    var isLoggedIn by remember { mutableStateOf(prefs.getBoolean("is_logged_in", false)) }
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
        if (currentScreen == "settings") {
            SettingsScreen(
                isLoggedIn = isLoggedIn,
                onLoginClick = { 
                    isWebViewVisible = true 
                    webView?.evaluateJavascript("loginPuter()", null) 
                },
                onClearChats = { chatViewModel.clearAllChats() },
                onBackClick = { currentScreen = "chat" }
            )
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

        PuterWebView(
            isVisible = isWebViewVisible,
            onWebViewCreated = { webView = it },
            onPopupCreated = { popupWebView = it },
            onLoginSuccess = { 
                isWebViewVisible = false
                isLoggedIn = true 
                prefs.edit().putBoolean("is_logged_in", true).apply()
            },
            onLoginError = { isWebViewVisible = false },
            onResponse = { response -> chatViewModel.addAiMessage(response) },
            onModelsLoaded = { },
            onImageGenerated = { url -> chatViewModel.addImageMessage(url) },
            onVideoAnalyzedCallback = { json -> chatViewModel.onVideoAnalyzedReceived(json) }
        )

        if (isWebViewVisible) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                IconButton(
                    onClick = { isWebViewVisible = false; popupWebView?.destroy(); popupWebView = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        if (popupWebView != null) {
            Dialog(
                onDismissRequest = { popupWebView?.destroy(); popupWebView = null }, 
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    AndroidView(factory = { popupWebView!! }, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = { popupWebView?.destroy(); popupWebView = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}
