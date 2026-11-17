package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ImportColumnSpec
import com.example.model.ImportPreviewState
import com.example.model.KeyboardMode
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.ResultDataTable
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.ErrorLightSurface
import com.example.ui.theme.ErrorText
import com.example.ui.theme.LightMintGreen
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SuccessText
import com.example.ui.theme.WarmBeige
import com.example.ui.theme.WarningText
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenInLab: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val personalDatasets by vm.personalDatasets.collectAsState()

    var pendingPreview by remember { mutableStateOf<ImportPreviewState?>(null) }
    var datasetName by remember { mutableStateOf("") }
    var tableName by remember { mutableStateOf("imported_table") }
    var editableColumns by remember { mutableStateOf<List<ImportColumnSpec>>(emptyList()) }
    var banner by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                } ?: uri.lastPathSegment ?: "import.csv"

                vm.parseUriForImport(uri, displayName)
                    .onSuccess { prev ->
                        pendingPreview = prev
                        datasetName = prev.fileName.substringBeforeLast('.')
                        tableName = prev.fileName.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "my_table" }
                        editableColumns = prev.columns
                        banner = null
                    }
                    .onFailure {
                        banner = "导入解析失败：${it.message}"
                        isError = true
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人数据集管理") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("datasets_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
            item {
                SeaSaltCard(containerColor = CloudSeaBlue) {
                    Text("导入自有数据集", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "通过系统文件选择器安全导入 CSV / JSON / SQLite / SQL 脚本，全程无需存储权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                        modifier = Modifier.fillMaxWidth().testTag("datasets_import_file_btn")
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("选择本地文件导入 (CSV / JSON / SQLite / SQL)")
                    }
                }
            }

            banner?.let { msg ->
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isError) ErrorLightSurface else LightMintGreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = if (isError) ErrorText else SuccessText, modifier = Modifier.padding(10.dp))
                    }
                }
            }

            item {
                Text("内置模板快速复制为个人可编辑库", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("retail", "inventory", "ecommerce", "finance", "hr", "procurement", "enterprise").forEach { dbId ->
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    vm.copyBuiltinToPersonal(dbId)
                                        .onSuccess {
                                            banner = "已复制 $dbId 为个人数据集「${it.name}」"
                                            isError = false
                                        }
                                        .onFailure {
                                            banner = "复制失败：${it.message}"
                                            isError = true
                                        }
                                }
                            },
                            modifier = Modifier.testTag("datasets_copy_$dbId")
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dbId)
                        }
                    }
                }
            }

            item {
                Text("我的本地个人数据库 (${personalDatasets.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (personalDatasets.isEmpty()) {
                item {
                    EmptyIllustrationCard(
                        title = "暂无个人数据集",
                        description = "导入你的业务 CSV/JSON/SQLite 文件，或点击上方内置库复制一份自由实验。"
                    )
                }
            } else {
                items(personalDatasets, key = { it.id }) { ds ->
                    SeaSaltCard(modifier = Modifier.testTag("personal_ds_${ds.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ds.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${ds.sourceType.uppercase()} · ${ds.tableCount} 张表 · ${ds.rowCount} 行",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                OutlinedButton(onClick = { onOpenInLab(ds.id) }) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("查询")
                                }
                                IconButton(onClick = { vm.deletePersonalDataset(ds) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除数据集", tint = ErrorText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Import Preview Dialog
    pendingPreview?.let { prev ->
        AlertDialog(
            onDismissRequest = { pendingPreview = null },
            title = { Text("确认导入「${prev.fileName}」") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill("${prev.sourceType.uppercase()} · 共 ${prev.totalRowCount} 行", CloudSeaBlue, PrimaryAction)
                    OutlinedTextField(
                        value = datasetName,
                        onValueChange = { datasetName = it },
                        label = { Text("数据集名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (prev.sourceType == "csv" || prev.sourceType == "json") {
                        OutlinedTextField(
                            value = tableName,
                            onValueChange = { tableName = it },
                            label = { Text("目标表名") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (editableColumns.isNotEmpty()) {
                            Text("推断列类型（点击可切换 INTEGER/REAL/TEXT）：", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                editableColumns.forEachIndexed { idx, col ->
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            val nextType = when (col.sqliteType) {
                                                "INTEGER" -> "REAL"
                                                "REAL" -> "TEXT"
                                                else -> "INTEGER"
                                            }
                                            editableColumns = editableColumns.toMutableList().also {
                                                it[idx] = col.copy(sqliteType = nextType)
                                            }
                                        },
                                        label = { Text("${col.name}:${col.sqliteType}") }
                                    )
                                }
                            }
                            if (prev.sampleRows.isNotEmpty()) {
                                ResultDataTable(
                                    title = "数据预览（前5行）",
                                    columns = editableColumns.map { it.name },
                                    rows = prev.sampleRows.take(5)
                                )
                            }
                        }
                    }
                    if (prev.warnings.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = WarmBeige) {
                            Text(prev.warnings.joinToString("\n"), style = MaterialTheme.typography.bodySmall, color = WarningText, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cols = editableColumns
                        val dsName = datasetName
                        val tblName = tableName
                        val previewObj = prev
                        pendingPreview = null
                        scope.launch {
                            vm.commitImportPersonalDataset(previewObj, dsName, tblName, cols)
                                .onSuccess {
                                    banner = "成功导入个人数据库「${it.name}」"
                                    isError = false
                                }
                                .onFailure {
                                    banner = "入库失败：${it.message}"
                                    isError = true
                                }
                        }
                    },
                    modifier = Modifier.testTag("datasets_confirm_import_btn")
                ) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPreview = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineStatusScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val report by vm.offlineReport.collectAsState()
    val checking by vm.isCheckingOffline.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("离线就绪与资源校验") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("offline_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SeaSaltCard(containerColor = if (report?.isReady == true) LightMintGreen else WarmBeige) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.illus_state_offline_ready),
                            contentDescription = "离线状态示意图",
                            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (report?.isReady == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (report?.isReady == true) SuccessText else WarningText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        checking -> "正在逐文件校验 SHA-256…"
                                        report?.isReady == true -> "全部离线资源校验通过"
                                        else -> "离线校验发现异常"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            report?.let { r ->
                                Text(
                                    text = "校验时间：${dateFormatter.format(Date(r.checkedAtMs))}\n已校验文件数：${r.totalFilesChecked} 个",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { vm.runOfflineSelfCheck() },
                        enabled = !checking,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                        modifier = Modifier.fillMaxWidth().testTag("offline_recheck_btn")
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (checking) "正在自检中…" else "立即重新校验 SHA-256 与 SQLite 引擎")
                    }
                }
            }

            report?.let { r ->
                item {
                    SeaSaltCard {
                        Text("引擎与本地存储明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("SQLite 冒烟测试：${if (r.sqliteSmokePassed) "通过" else "失败"}（${r.sqliteSmokeDetail}）", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("内置内容资产大小：${"%.2f".format(r.totalAssetBytes / 1048576.0)} MB", style = MaterialTheme.typography.bodySmall)
                        Text("已解压业务库占用：${"%.2f".format(r.extractedDbBytes / 1048576.0)} MB", style = MaterialTheme.typography.bodySmall)
                        Text("个人自建库占用：${"%.2f".format(r.personalDbBytes / 1048576.0)} MB", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (r.missingOrCorruptFiles.isNotEmpty()) {
                    item {
                        SeaSaltCard(containerColor = ErrorLightSurface) {
                            Text("缺失或损坏清单 (${r.missingOrCorruptFiles.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ErrorText)
                            Spacer(modifier = Modifier.height(6.dp))
                            r.missingOrCorruptFiles.forEach { path ->
                                Text("• $path", style = MaterialTheme.typography.bodySmall, color = ErrorText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val settings by vm.settingsFlow.collectAsState()
    var resetScopePending by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习与编辑器设置") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
            // Theme Mode
            item {
                SeaSaltCard {
                    Text("外观主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("light" to "海盐浅色", "dark" to "海盐深色", "system" to "跟随系统").forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.appThemeMode == mode,
                                onClick = { vm.updateSettings { it.copy(appThemeMode = mode) } },
                                label = { Text(label) },
                                modifier = Modifier.testTag("settings_theme_$mode")
                            )
                        }
                    }
                }
            }

            // SQL Editor Settings
            item {
                SeaSaltCard {
                    Text("SQL 编辑器与自定义键盘", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("字体字号：${settings.editorFontSizeSp}sp", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.editorFontSizeSp.toFloat(),
                        onValueChange = { v -> vm.updateSettings { it.copy(editorFontSizeSp = v.toInt()) } },
                        valueRange = 12f..20f,
                        steps = 7
                    )

                    SettingSwitchRow("编辑器深色代码面板", settings.editorDarkTheme) { v ->
                        vm.updateSettings { it.copy(editorDarkTheme = v) }
                    }
                    SettingSwitchRow("自动配对括号与单引号", settings.autoPair) { v ->
                        vm.updateSettings { it.copy(autoPair = v) }
                    }
                    SettingSwitchRow("空格自动转大写关键字", settings.autoUppercase) { v ->
                        vm.updateSettings { it.copy(autoUppercase = v) }
                    }
                    SettingSwitchRow("换行自动继承缩进", settings.autoIndent) { v ->
                        vm.updateSettings { it.copy(autoIndent = v) }
                    }
                    SettingSwitchRow("SQL 键盘按键触觉反馈", settings.hapticFeedback) { v ->
                        vm.updateSettings { it.copy(hapticFeedback = v) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("默认键盘页签：", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyboardMode.entries.forEach { km ->
                            FilterChip(
                                selected = settings.defaultKeyboardMode == km.id,
                                onClick = { vm.updateSettings { it.copy(defaultKeyboardMode = km.id) } },
                                label = { Text(km.label) }
                            )
                        }
                    }
                }
            }

            // FSRS Settings
            item {
                SeaSaltCard {
                    Text("FSRS-4.5 记忆算法参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("期望记忆保持率：${(settings.desiredRetention * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.desiredRetention.toFloat(),
                        onValueChange = { v -> vm.updateSettings { it.copy(desiredRetention = v.toDouble()) } },
                        valueRange = 0.80f..0.95f
                    )
                    Text("每日新闪卡上限：${settings.dailyNewCardLimit} 张", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.dailyNewCardLimit.toFloat(),
                        onValueChange = { v -> vm.updateSettings { it.copy(dailyNewCardLimit = v.toInt()) } },
                        valueRange = 5f..60f
                    )
                }
            }

            // Granular Progress Reset
            item {
                SeaSaltCard {
                    Text("分模块进度重置（高风险操作）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ErrorText)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("可按模块独立清理，不影响你的个人自建数据集与收藏笔记。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "courses" to "清空课程知识点完成记录",
                            "questions" to "清空全部刷题与错题记录",
                            "flashcards" to "重置 FSRS 闪卡记忆调度（保留自定义卡）",
                            "lab" to "清空查询历史与草稿",
                            "all" to "重置所有学习进度（保留个人库/自定义卡）"
                        ).forEach { (scopeKey, label) ->
                            OutlinedButton(
                                onClick = { resetScopePending = scopeKey to label },
                                modifier = Modifier.fillMaxWidth().testTag("settings_reset_$scopeKey")
                            ) {
                                Text(label, color = ErrorText)
                            }
                        }
                    }
                }
            }
        }
    }

    resetScopePending?.let { (scopeKey, label) ->
        AlertDialog(
            onDismissRequest = { resetScopePending = null },
            title = { Text("确认执行重置？") },
            text = { Text("即将执行：$label。此操作不可撤销，建议先在「我的 -> 导出备份」保存 JSON 备份。") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.resetProgress(scopeKey)
                        resetScopePending = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorText)
                ) {
                    Text("确认重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetScopePending = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
