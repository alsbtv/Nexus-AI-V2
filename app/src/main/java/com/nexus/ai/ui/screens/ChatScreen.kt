package com.nexus.ai.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nexus.ai.data.MessageEntity
import com.nexus.ai.ui.components.*
import com.nexus.ai.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onMenuClick: () -> Unit,
    onSendMessage: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var inputText by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    
    var showAttachSheet by remember { mutableStateOf(false) }
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var fullScreenImage by remember { mutableStateOf<Uri?>(null) }
    var isVideoMode by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) { tts = TextToSpeech(context) { } }
    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    val currentChatId = viewModel.currentChatId
    val messagesState = if (currentChatId != null) {
        viewModel.getMessages(currentChatId).collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList<MessageEntity>()) }
    }
    val messages by messagesState

    val isAtBottom by remember { derivedStateOf { 
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
        last == null || last.index == messages.size - 1 
    }}

    val photoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) selectedAttachmentUri = uri }
    val documentPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri -> if (uri != null) selectedAttachmentUri = uri }

    if (viewModel.isGeneratingVideo) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                NexusTopBar(
                    chatTitle = if (messages.isEmpty()) "Nexus AI" else "محادثة",
                    modelName = viewModel.currentModel,
                    onMenuClick = onMenuClick,
                    onModelClick = { },
                    onNewChatClick = { viewModel.startNewChat() },
                    hasPins = false,
                    onPinClick = {}
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).imePadding()) {
                Box(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("كيف أساعدك اليوم؟", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                MessageItem(
                                    message = message,
                                    onImageClick = { fullScreenImage = it },
                                    onCopy = { clipboardManager.setText(AnnotatedString(message.text)) },
                                    onTTS = { tts?.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, null) }
                                )
                            }
                        }
                    }
                }
                NexusInputBar(
                    text = inputText, onTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() || selectedAttachmentUri != null) {
                            viewModel.addUserMessage(inputText, selectedAttachmentUri?.toString())
                            onSendMessage(inputText, viewModel.currentSkill, "")
                            inputText = ""; selectedAttachmentUri = null
                        }
                    },
                    onAttachClick = { showAttachSheet = true },
                    isEditing = false, onCancelEdit = {}, canGoBack = false, canGoForward = false, onEditBack = {}, onEditForward = {},
                    onThinkClick = { }, onSkillClick = { },
                    selectedSkill = viewModel.currentSkill, thinkingLevel = viewModel.currentThinkingLevel,
                    isVideoMode = isVideoMode, onVideoModeClick = { isVideoMode = !isVideoMode }
                )
            }
        }

        if (!isAtBottom) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp).size(40.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)
            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll") }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("المرفقات", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text("Camera", modifier = Modifier.clickable { showAttachSheet = false })
                    Text("Photo", modifier = Modifier.clickable { showAttachSheet = false; photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                }
            }
        }
    }

    if (fullScreenImage != null) {
        FullScreenImageViewer(imageUri = fullScreenImage!!, onDismiss = { fullScreenImage = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(message: MessageEntity, onImageClick: (Uri) -> Unit, onCopy: () -> Unit, onTTS: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
        if (message.isUser) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SelectionContainer { Text(text = message.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp) }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                SelectionContainer { Text(text = message.text, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, lineHeight = 24.sp) }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp).clickable { onCopy() }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(20.dp).clickable { onTTS() }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
