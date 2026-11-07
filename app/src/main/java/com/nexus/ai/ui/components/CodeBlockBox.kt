package com.nexus.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeBlockBox(codeText: String, language: String = "code") {
    val clipboardManager = LocalClipboardManager.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2D2D)).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(language, color = Color(0xFFA0A0A0), fontSize = 12.sp)
                Row(modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(codeText)) }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFA0A0A0), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = Color(0xFFA0A0A0), fontSize = 12.sp)
                }
            }
            Box(modifier = Modifier.padding(12.dp).horizontalScroll(rememberScrollState())) {
                Text(text = codeText, color = Color(0xFFE0E0E0), fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}
