package com.nexus.ai.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nexus.ai.data.MessageEntity
import com.nexus.ai.data.PinnedMessageEntity
import com.nexus.ai.ui.components.*
import com.nexus.ai.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    
    // States
    var inputText by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editingMessageId by remember { mutableStateOf<String?>(null) }
    var editHistoryList by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentEditIndex by remember { mutableIntStateOf(-1) }
    
    var showModelSheet by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showThinkSheet by remember { mutableStateOf(false) }
    var showSkillSheet by remember { mutableStateOf(false) }
    var showPinsSheet by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinTargetMessageId by remember { mutableStateOf<String?>(null) }
    var pinNameInput by remember { mutableStateOf("") }
    
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var fullScreenImage by remember { mutableStateOf<Uri?>(null) }
    var isVideoMode by remember { mutableStateOf(false) }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status -> if (status != TextToSpeech.SUCCESS) { /* Error */ } }
    }
    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    val messages by viewModel.currentChatId?.let { id ->
        viewModel.getMessages(id).collectAsStateWithLifecycle(initialValue = emptyList())
    } ?: remember { mutableStateOf(emptyList()) }
    
    val pins by viewModel.currentChatId?.let { id ->
        viewModel.getPins(id).collectAsStateWithLifecycle(initialValue = emptyList())
    } ?: remember { mutableStateOf(emptyList()) }

    val isAtBottom by remember { derivedStateOf { 
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
        last == null || last.index == messages.size - 1 
    }}

    val photoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) selectedAttachmentUri = uri }
    val documentPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri -> if (uri != null) selectedAttachmentUri = uri }

    // مؤشر التحميل للفيديو
    if (viewModel.isGeneratingVideo) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("جاري المونتاج...", color = Color.White, modifier = Modifier.padding(top = 60.dp))
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
                    hasPins = pins.isNotEmpty(),
                    onPinClick = { showPinsSheet = true }
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
                            items(messages) { message ->
                                MessageItem(
                                    message = message,
                                    onImageClick = { fullScreenImage = it },
                                    onEdit = { 
                                        isEditing = true; editingMessageId = message.id; 
                                        editHistoryList = if (message.editHistory.isBlank()) listOf(message.text) else message.editHistory.split("||") + message.text
                                        currentEditIndex = editHistoryList.size - 1
                                        inputText = message.text 
                                    },
                                    onPin = { pinTargetMessageId = message.id; showPinDialog = true },
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
                                val currentMsg = messages.find { it.id == editingMessageId }
                                viewModel.editMessage(editingMessageId!!, currentMsg?.text ?: "", inputText, currentMsg?.editHistory ?: "")
                                isEditing = false; editingMessageId = null
                            } else {
                                viewModel.addUserMessage(inputText, selectedAttachmentUri?.toString())
                                onSendMessage(inputText, viewModel.currentSkill, "")
                                inputText = ""; selectedAttachmentUri = null
                            }
                        }
                    },
                    onAttachClick = { showAttachSheet = true },
                    isEditing = isEditing,
                    onCancelEdit = { isEditing = false; inputText = "" },
                    canGoBack = currentEditIndex > 0,
                    canGoForward = currentEditIndex < editHistoryList.size - 1,
                    onEditBack = { if (currentEditIndex > 0) { currentEditIndex--; inputText = editHistoryList[currentEditIndex] } },
                    onEditForward = { if (currentEditIndex < editHistoryList.size - 1) { currentEditIndex++; inputText = editHistoryList[currentEditIndex] } },
                    onThinkClick = { showThinkSheet = true },
                    onSkillClick = { showSkillSheet = true },
                    selectedSkill = viewModel.currentSkill,
                    thinkingLevel = viewModel.currentThinkingLevel,
                    isVideoMode = isVideoMode,
                    onVideoModeClick = { isVideoMode = !isVideoMode }
                )
            }
        }

        // زر النزول للأسفل
        if (!isAtBottom) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 16.dp).size(40.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)
            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll") }
        }
    }

    // --- النوافذ المنبثقة (Sheets & Dialogs) ---
    if (showModelSheet) {
        ModalBottomSheet(onDismissRequest = { showModelSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("اختر النموذج", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                listOf("Auto", "Claude 3.5 Sonnet", "GPT-4o", "DeepSeek V3").forEach { model ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.currentModel = model; showModelSheet = false }.padding(12.dp)) {
                        Text(model, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    if (showThinkSheet) {
        ModalBottomSheet(onDismissRequest = { showThinkSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("مستوى التفكير", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                listOf("Low", "Medium", "High").forEach { level ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.currentThinkingLevel = level; showThinkSheet = false }.padding(12.dp)) {
                        RadioButton(selected = viewModel.currentThinkingLevel == level, onClick = {})
                        Text(level, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showSkillSheet) {
        ModalBottomSheet(onDismissRequest = { showSkillSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("المهارة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                listOf("General", "Developer", "Writer", "Translator", "Analyst").forEach { skill ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.currentSkill = skill; showSkillSheet = false }.padding(12.dp)) {
                        RadioButton(selected = viewModel.currentSkill == skill, onClick = {})
                        Text(skill, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showPinsSheet) {
        ModalBottomSheet(onDismissRequest = { showPinsSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("الرسائل المثبتة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                pins.forEach { pin ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { 
                        val index = messages.indexOfFirst { it.id == pin.messageId }
                        if (index != -1) scope.launch { listState.animateScrollToItem(index) }
                        showPinsSheet = false
                    }.padding(12.dp)) {
                        Icon(Icons.Default.PushPin, contentDescription = null)
                        Text(pin.pinName, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showPinDialog && pinTargetMessageId != null) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("تثبيت الرسالة") },
            text = { OutlinedTextField(value = pinNameInput, onValueChange = { pinNameInput = it }, placeholder = { Text("اسم التثبيت...") }) },
            confirmButton = { TextButton(onClick = { viewModel.addPin(viewModel.currentChatId ?: "", pinTargetMessageId!!, pinNameInput); showPinDialog = false; pinNameInput = "" }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("إلغاء") } }
        )
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

@Composable
fun MessageItem(message: MessageEntity, onImageClick: (Uri) -> Unit, onEdit: () -> Unit, onPin: () -> Unit, onCopy: () -> Unit, onTTS: () -> Unit, onRegenerate: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
        if (message.isUser) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.attachmentUri != null) {
                        AsyncImage(model = message.attachmentUri, contentDescription = "Img", modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp)).clickable { onImageClick(Uri.parse(message.attachmentUri)) }, contentScale = ContentScale.Crop)
                    }
                    SelectionContainer {
                        Text(
                            text = message.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp,
                            modifier = Modifier.combinedClickable(onClick = { onEdit() }, onLongClick = { onPin() })
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (message.isVideo && message.videoUri != null) {
                    VideoPlayer(videoUri = Uri.parse(message.videoUri))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { /* Download */ }) { Icon(Icons.Default.Download, contentDescription = "Download") }
                        IconButton(onClick = { onEdit() }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { /* Like */ }) { Icon(Icons.Default.ThumbUp, contentDescription = "Like") }
                    }
                } else if (message.isImage) {
                    AsyncImage(model = message.text, contentDescription = "Gen Img", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onImageClick(Uri.parse(message.text)) }, contentScale = ContentScale.FillWidth)
                } else {
                    RichText(text = message.text, onCopy = onCopy)
                    // أزرار التفاعل
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
