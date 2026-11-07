package com.nexus.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    isEditing: Boolean,
    onCancelEdit: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onEditBack: () -> Unit,
    onEditForward: () -> Unit,
    onThinkClick: () -> Unit,
    onSkillClick: () -> Unit,
    selectedSkill: String,
    thinkingLevel: String,
    isVideoMode: Boolean,
    onVideoModeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isEditing) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Editing message", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEditBack, enabled = canGoBack, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back") }
                        IconButton(onClick = onEditForward, enabled = canGoForward, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.ChevronRight, contentDescription = "Forward") }
                        IconButton(onClick = onCancelEdit, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Cancel Edit") }
                    }
                }
            }
            TextField(
                value = text, onValueChange = onTextChange,
                placeholder = { Text("Type a message...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Think Button
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.clickable { onThinkClick() }) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = "Think", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (thinkingLevel == "Medium") "Think" else thinkingLevel, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Skill Button
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.clickable { onSkillClick() }) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = "Skill", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedSkill, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Video Mode Button
                    Surface(shape = RoundedCornerShape(16.dp), color = if (isVideoMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, modifier = Modifier.clickable { onVideoModeClick() }) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = "Video", modifier = Modifier.size(16.dp), tint = if (isVideoMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مخرج", fontSize = 12.sp, color = if (isVideoMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onAttachClick, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = onSend, modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))) { 
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary) 
                    }
                }
            }
        }
    }
}
