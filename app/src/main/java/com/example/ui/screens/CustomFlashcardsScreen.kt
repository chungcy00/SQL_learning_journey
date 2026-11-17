package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserFlashcardEntity
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.ErrorText
import com.example.ui.theme.PrimaryAction
import com.example.ui.viewmodel.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFlashcardsScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val customCards by vm.userFlashcards.collectAsState()
    var editingCard by remember { mutableStateOf<UserFlashcardEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义闪卡 (${customCards.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("custom_fc_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCard = null
                    showEditor = true
                },
                containerColor = PrimaryAction,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("custom_fc_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建自定义闪卡")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (customCards.isEmpty()) {
                item {
                    EmptyIllustrationCard(
                        title = "暂无自定义闪卡",
                        description = "点击右下角 + 号创建专属 SQL 记忆卡，或在闪卡浏览列表中复制内置卡进行个性化修改。",
                        actionLabel = "新建第一张闪卡",
                        onAction = {
                            editingCard = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(customCards, key = { it.id }) { card ->
                    SeaSaltCard(modifier = Modifier.testTag("custom_fc_item_${card.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(card.type, CloudSeaBlue, PrimaryAction)
                                StatusPill(card.difficulty, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        editingCard = card
                                        showEditor = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { vm.deleteCustomFlashcard(card.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = ErrorText, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(card.frontQuestion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("答：${card.backAnswer}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showEditor) {
        CustomCardEditorDialog(
            initial = editingCard,
            onDismiss = { showEditor = false },
            onSave = { front, frontCode, back, expl, backCode, note ->
                val now = System.currentTimeMillis()
                val entity = editingCard?.copy(
                    frontQuestion = front,
                    frontCode = frontCode.ifBlank { null },
                    backAnswer = back,
                    backExplanation = expl,
                    backCode = backCode.ifBlank { null },
                    note = note,
                    updatedAtMs = now
                ) ?: UserFlashcardEntity(
                    id = "custom_${UUID.randomUUID().toString().take(8)}",
                    chapterId = "ch01",
                    type = "概念辨析",
                    difficulty = "中等",
                    frontQuestion = front,
                    frontCode = frontCode.ifBlank { null },
                    backAnswer = back,
                    backExplanation = expl,
                    backCode = backCode.ifBlank { null },
                    note = note,
                    createdAtMs = now,
                    updatedAtMs = now
                )
                vm.saveCustomFlashcard(entity)
                showEditor = false
            }
        )
    }
}

@Composable
private fun CustomCardEditorDialog(
    initial: UserFlashcardEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var front by remember(initial) { mutableStateOf(initial?.frontQuestion ?: "") }
    var frontCode by remember(initial) { mutableStateOf(initial?.frontCode ?: "") }
    var back by remember(initial) { mutableStateOf(initial?.backAnswer ?: "") }
    var expl by remember(initial) { mutableStateOf(initial?.backExplanation ?: "") }
    var backCode by remember(initial) { mutableStateOf(initial?.backCode ?: "") }
    var note by remember(initial) { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建自定义闪卡" else "编辑闪卡") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("正面问题 *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("custom_fc_front_input")
                )
                OutlinedTextField(
                    value = frontCode,
                    onValueChange = { frontCode = it },
                    label = { Text("正面 SQL 片段（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("背面答案 *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("custom_fc_back_input")
                )
                OutlinedTextField(
                    value = expl,
                    onValueChange = { expl = it },
                    label = { Text("详细解析（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = backCode,
                    onValueChange = { backCode = it },
                    label = { Text("示例代码（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("个人备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(front.trim(), frontCode.trim(), back.trim(), expl.trim(), backCode.trim(), note.trim()) },
                enabled = front.isNotBlank() && back.isNotBlank(),
                modifier = Modifier.testTag("custom_fc_save_btn")
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
