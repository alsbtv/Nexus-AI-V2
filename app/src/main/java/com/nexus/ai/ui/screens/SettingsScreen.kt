package com.nexus.ai.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.ai.data.GitHubUpdateManager
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("NexusPrefs", Context.MODE_PRIVATE) }
    val updateManager = remember { GitHubUpdateManager() }

    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf(prefs.getString("github_token", "") ?: "") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateRelease by remember { mutableStateOf<JSONObject?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Profile",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {}.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Account settings", modifier = Modifier.weight(1f))
                    }
                }
            }

            Text(
                "Developer & Updates",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showTokenDialog = true }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Link GitHub Token")
                            val hasToken = !prefs.getString("github_token", null).isNullOrBlank()
                            Text(
                                if (hasToken) "Token saved" else "Not linked",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isChecking) {
                            isChecking = true
                            scope.launch {
                                val release = updateManager.checkLatestRelease()
                                isChecking = false
                                if (release == null) {
                                    Toast.makeText(context, "تعذر التحقق من التحديثات", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val tag = release.optString("tag_name", "")
                                val currentVersion = try {
                                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                                } catch (e: Exception) { "" }
                                val normalizedTag = tag.removePrefix("v").trim()
                                if (normalizedTag.isNotBlank() && normalizedTag != currentVersion) {
                                    updateRelease = release
                                    showUpdateDialog = true
                                } else {
                                    Toast.makeText(context, "أنت على أحدث إصدار ($currentVersion)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (isChecking) "Checking..." else "Check for Updates", modifier = Modifier.weight(1f))
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            Text(
                "About",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {}.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Version 2.0", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Link GitHub Token") },
            text = {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("GitHub Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putString("github_token", tokenInput.trim()).apply()
                    showTokenDialog = false
                    Toast.makeText(context, "Token saved", Toast.LENGTH_SHORT).show()
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showUpdateDialog && updateRelease != null) {
        val release = updateRelease!!
        val tag = release.optString("tag_name", "")
        val body = release.optString("body", "لا توجد ملاحظات.")
        val assets = release.optJSONArray("assets")
        var downloadUrl = ""
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                if (name.endsWith(".apk") && url.isNotBlank()) {
                    downloadUrl = url
                    break
                }
                if (downloadUrl.isBlank() && url.isNotBlank()) downloadUrl = url
            }
        }
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("تحديث متاح: $tag") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(body)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (downloadUrl.isNotBlank()) {
                            updateManager.downloadAndInstallApk(context, downloadUrl, tag.ifBlank { "latest" })
                            Toast.makeText(context, "بدأ تنزيل التحديث", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "لم يتم العثور على ملف APK", Toast.LENGTH_SHORT).show()
                        }
                        showUpdateDialog = false
                    }
                ) { Text("تحديث") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("لاحقاً") }
            }
        )
    }
}
