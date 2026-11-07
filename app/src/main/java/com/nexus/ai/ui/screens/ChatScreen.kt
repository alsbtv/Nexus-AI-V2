package com.nexus.ai.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
    
    var showModelSheet by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showThinkSheet by remember { mutableStateOf(false) }
    var showSkillSheet by remember { mutableStateOf(false) }
    
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
            Text("جاري المعالجة...", color = Color.White, modifier = Modifier.padding(top = 60.dp))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                NexusTopBar(
                    chatTitle = if (messages.isEmpty()) "Nexus AI" else "محادثة",
                    modelName = viewModel.currentModel,
                    onMenuClick = onMenuClick,
                    onModelClick = { showModelSheet = true },
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
                        EmptyState(onSuggestionClick = { inputText = it })
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
                                    onEdit = { isEditing = true; editingMessageId = message.id; inputText = message.text },
                                    onPin = { },
                                    onCopy = { clipboardManager.setText(AnnotatedString(message.text)) },
                                    onTTS = { tts?.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, null) },
                                    onRegenerate = { onSendMessage(message.text, viewModel.currentSkill, "") }
                                )
                            }
                        }
                    }
                }
                NexusInputBar(
                    text = inputText, onTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank() || selectedAttachmentUri != null) {
                            if (isVideoMode) {
                                viewModel.generateMontage(inputText)
                            } else if (isEditing && editingMessageId != null) {
                                isEditing = false; editingMessageId = null
                            } else {
                                viewModel.addUserMessage(inputText, selectedAttachmentUri?.toString())
                                onSendMessage(inputText, viewModel.currentSkill, "")
                                inputText = ""; selectedAttachmentUri = null
                            }
                        }
                    },
                    onAttachClick = { showAttachSheet = true },
                    isEditing = isEditing, onCancelEdit = { isEditing = false; inputText = "" },
                    canGoBack = false, canGoForward = false, onEditBack = {}, onEditForward = {},
                    onThinkClick = { showThinkSheet = true }, onSkillClick = { showSkillSheet = true },
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
            AttachmentSheet(
                onPhotoClick = { showAttachSheet = false; photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onDocumentClick = { showAttachSheet = false; documentPickerLauncher.launch("*/*") },
                onCameraClick = { showAttachSheet = false; photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
        }
    }

    if (fullScreenImage != null) {
        FullScreenImageViewer(imageUri = fullScreenImage!!, onDismiss = { fullScreenImage = null })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(message: MessageEntity, onImageClick: (Uri) -> Unit, onEdit: () -> Unit, onPin: () -> Unit, onCopy: () -> Unit, onTTS: () -> Unit, onRegenerate: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
        if (message.isUser) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.attachmentUri != null) {
                        AsyncImage(model = message.attachmentUri, contentDescription = "Img", modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)).clickable { onImageClick(Uri.parse(message.attachmentUri)) }, contentScale = ContentScale.Crop)
                    }
                    SelectionContainer { Text(text = message.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp) }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (message.isVideo && message.videoUri != null) {
                    VideoPlayer(videoUri = Uri.parse(message.videoUri))
                } else if (message.isImage) {
                    AsyncImage(model = message.text, contentDescription = "Gen Img", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onImageClick(Uri.parse(message.text)) }, contentScale = ContentScale.FillWidth)
                } else {
                    RichText(text = message.text, onCopy = onCopy)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp).clickable { onCopy() }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.ThumbUp, contentDescription = "Like", modifier = Modifier.size(20.dp).clickable { }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.ThumbDown, contentDescription = "Dislike", modifier = Modifier.size(20.dp).clickable { }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak", modifier = Modifier.size(20.dp).clickable { onTTS() }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(20.dp).clickable { onRegenerate() }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun RichText(text: String, onCopy: () -> Unit) {
    val parts = text.split("```")
    SelectionContainer {
        Column {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 0) {
                    if (part.isNotBlank()) Text(part.trim(), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, lineHeight = 24.sp)
                } else {
                    val lines = part.trim().lines()
                    val lang = if (lines.isNotEmpty() && lines.first().length < 15) lines.first() else "code"
                    val code = if (lines.isNotEmpty() && lines.first().length < 15) lines.drop(1).joinToString("\n") else part.trim()
                    CodeBlockBox(codeText = code, language = lang)
                }
            }
        }
    }
}

@Composable
fun EmptyState(onSuggestionClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("كيف أساعدك اليوم؟", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))
        listOf("بناء تطبيق", "إصلاح كود", "توليد صورة", "مونتاج فيديو").forEach { suggestion ->
            Text(suggestion, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().clickable { onSuggestionClick(suggestion) }.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
