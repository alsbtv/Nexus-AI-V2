package com.nexus.ai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onClearChats: () -> Unit,
    onBackClick: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("الإعدادات", fontWeight = FontWeight.Medium) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            
            Text("الحساب", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { if (!isLoggedIn) onLoginClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isLoggedIn) Icons.Default.CheckCircle else Icons.Default.Person, contentDescription = null, tint = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(if (isLoggedIn) "تم تسجيل الدخول عبر Puter" else "تسجيل الدخول عبر Puter", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Text("البيانات", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { showClearDialog = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("مسح جميع المحادثات", modifier = Modifier.weight(1f), color = Color.Red)
                }
            }
            
            Text("حول التطبيق", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().clickable {}.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null); Spacer(modifier = Modifier.width(16.dp)); Text("الإصدار 2.0", modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("مسح المحادثات") },
            text = { Text("هل أنت متأكد من مسح جميع المحادثات والرسائل؟ لا يمكن التراجع عن هذا الإجراء. (لن يتم مسح حسابك).") },
            confirmButton = {
                TextButton(onClick = { 
                    onClearChats() 
                    showClearDialog = false 
                }) { Text("مسح", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
