package com.example.ui.screens

import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.ImportExportEngine
import com.example.model.ExecutionResult
import com.example.model.KeyboardMode
import com.example.model.QueryTabState
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.FieldGroup
import com.example.ui.components.FullscreenSqlEditorDialog
import com.example.ui.components.ResultDataTable
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.SqlCodeEditorBox
import com.example.ui.components.SqlCustomKeyboardPanel
import com.example.ui.components.SqlEditorController
import com.example.ui.components.StatusPill
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.ErrorLightSurface
import com.example.ui.theme.ErrorText
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.LightMintGreen
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SuccessText
import com.example.ui.theme.WarmBeige
import com.example.ui.theme.WarningText
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private val BUILTIN_DB_OPTIONS = listOf(
    "retail" to "零售销售",
    "inventory" to "库存仓储",
    "ecommerce" to "电商用户",
    "finance" to "企业财务",
    "hr" to "人力资源",
    "procurement" to "采购供应",
    "enterprise" to "综合企业"
)

private val LAB_SNIPPETS = listOf(
    "SELECT 模板" to "SELECT *\nFROM \nLIMIT 20;",
    "GROUP BY 聚合" to "SELECT col, COUNT(*) AS cnt\nFROM \nGROUP BY col\nORDER BY cnt DESC;",
    "LEFT JOIN" to "SELECT a.*, b.*\nFROM table_a AS a\nLEFT JOIN table_b AS b\n  ON a.id = b.a_id\nLIMIT 20;",
    "WITH CTE" to "WITH base AS (\n  SELECT *\n  FROM \n)\nSELECT *\nFROM base\nLIMIT 20;",
    "ROW_NUMBER()" to "SELECT *,\n  ROW_NUMBER() OVER (PARTITION BY col ORDER BY val DESC) AS rn\nFROM \nLIMIT 20;"
)

@Composable
fun LabScreen(
    vm: MainViewModel,
    initialDatasetId: String? = null,
    initialSql: String? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val settings by vm.settingsFlow.collectAsState()
    val personalDatasets by vm.personalDatasets.collectAsState()
    val queryHistory by vm.queryHistory.collectAsState()
    val scope = rememberCoroutineScope()

    val startDataset = (initialDatasetId ?: settings.lastDatasetId).removeSuffix("-v2").ifBlank { "retail" }
    val tabs = remember {
        mutableStateListOf(
            QueryTabState(
                id = "tab_1",
                title = "查询 1",
                datasetId = startDataset,
                sql = initialSql?.ifBlank { null } ?: "-- 在此编写 SQL 查询，支持 MySQL 8.4 语法与本地写操作\nSELECT name, type FROM sqlite_master WHERE type = 'table';"
            )
        )
    }
    var activeTabIdx by remember { mutableIntStateOf(0) }
    var selectedSectionTab by remember { mutableIntStateOf(if (initialSql != null) 1 else 1) } // 0=数据表, 1=代码, 2=结果

    LaunchedEffect(initialDatasetId, initialSql) {
        if (!initialDatasetId.isNullOrBlank() || !initialSql.isNullOrBlank()) {
            val cleanDs = (initialDatasetId ?: tabs[activeTabIdx].datasetId).removeSuffix("-v2")
            tabs[activeTabIdx] = tabs[activeTabIdx].copy(
                datasetId = cleanDs,
                sql = initialSql ?: tabs[activeTabIdx].sql
            )
            selectedSectionTab = 1
        }
    }

    val currentTab = tabs.getOrElse(activeTabIdx) { tabs.first() }
    val activeDatasetId = currentTab.datasetId

    val editorController = remember(currentTab.id) {
        SqlEditorController(
            initialText = currentTab.sql,
            autoPair = settings.autoPair,
            autoUppercase = settings.autoUppercase,
            autoIndent = settings.autoIndent
        )
    }
    LaunchedEffect(currentTab.id, currentTab.sql) {
        if (editorController.value.text != currentTab.sql) {
            editorController.setExternalText(currentTab.sql)
        }
    }
    LaunchedEffect(editorController.value.text, activeTabIdx) {
        if (activeTabIdx in tabs.indices && tabs[activeTabIdx].sql != editorController.value.text) {
            tabs[activeTabIdx] = tabs[activeTabIdx].copy(sql = editorController.value.text)
        }
        delay(500)
        vm.saveDraft("lab:${currentTab.id}", editorController.value.text, editorController.value.selection.start, activeDatasetId)
    }

    var tableSchemas by remember { mutableStateOf<List<Pair<String, List<String>>>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(activeDatasetId, refreshTrigger) {
        tableSchemas = vm.inspectLabTables(activeDatasetId)
    }

    val fieldGroups = remember(tableSchemas) {
        tableSchemas.map { (t, cols) -> FieldGroup(t, cols) }
    }

    var isRunning by remember { mutableStateOf(false) }
    var cancelSignal by remember { mutableStateOf<CancellationSignal?>(null) }
    var useCustomKeyboard by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    var pendingExportFormat by remember { mutableStateOf("csv") }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val res = currentTab.lastResult
        if (uri != null && res != null && res.success) {
            scope.launch {
                runCatching {
                    val content = if (pendingExportFormat == "csv") {
                        ImportExportEngine.exportResultToCsv(res.columns, res.rows)
                    } else {
                        ImportExportEngine.exportResultToJson(res.columns, res.rows)
                    }
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    }
                    statusBanner = "已安全导出 ${res.rows.size} 行结果至选定位置"
                }.onFailure {
                    statusBanner = "导出失败：${it.message}"
                }
            }
        }
    }

    fun executeActiveSql() {
        val sql = editorController.value.text
        if (sql.isBlank()) return
        scope.launch {
            val sig = CancellationSignal()
            cancelSignal = sig
            isRunning = true
            val res = vm.runLabQuery(activeDatasetId, sql, sig)
            tabs[activeTabIdx] = tabs[activeTabIdx].copy(sql = sql, lastResult = res)
            refreshTrigger++
            isRunning = false
            cancelSignal = null
            selectedSectionTab = 2
        }
    }

    val runActionBar: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRunning) {
                OutlinedButton(
                    onClick = { cancelSignal?.cancel() },
                    modifier = Modifier.weight(1f).testTag("lab_stop_btn")
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("停止查询")
                }
            } else {
                Button(
                    onClick = { executeActiveSql() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                    modifier = Modifier.weight(1.3f).testTag("lab_run_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("运行 SQL")
                }
            }
            OutlinedButton(
                onClick = {
                    val code = editorController.value.text.trim()
                    if (code.isNotBlank()) {
                        vm.toggleFavorite(
                            targetType = "snippet",
                            targetId = UUID.randomUUID().toString().take(8),
                            title = "${activeDatasetId} 实验室片段",
                            subtitle = code.lineSequence().firstOrNull().orEmpty(),
                            payload = code
                        )
                        statusBanner = "已保存至「收藏与片段」"
                    }
                },
                modifier = Modifier.weight(1f).testTag("lab_save_snippet_btn")
            ) {
                Icon(Icons.Default.BookmarkAdd, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("存为片段")
            }
        }
    }

    if (isFullscreen) {
        FullscreenSqlEditorDialog(
            controller = editorController,
            isDarkEditor = settings.editorDarkTheme,
            fontSizeSp = settings.editorFontSizeSp,
            useCustomKeyboard = useCustomKeyboard,
            onToggleCustomKeyboard = { useCustomKeyboard = !useCustomKeyboard },
            fieldGroups = fieldGroups,
            initialMode = KeyboardMode.entries.firstOrNull { it.id == settings.defaultKeyboardMode } ?: KeyboardMode.SYNTAX,
            enableHaptic = settings.hapticFeedback,
            onDismiss = { isFullscreen = false },
            actionBar = runActionBar
        )
    }

    Scaffold(
        bottomBar = {
            if (selectedSectionTab == 1 && useCustomKeyboard) {
                SqlCustomKeyboardPanel(
                    controller = editorController,
                    fieldGroups = fieldGroups,
                    initialMode = KeyboardMode.entries.firstOrNull { it.id == settings.defaultKeyboardMode } ?: KeyboardMode.SYNTAX,
                    enableHaptic = settings.hapticFeedback,
                    onHideKeyboard = { useCustomKeyboard = false }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Header + Database Switcher
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("lab_back_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                        Column {
                            Text("SQL 代码实验室", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("独立沙盒工作副本，支持增删改查且不污染题库", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (BUILTIN_DB_OPTIONS.any { it.first == activeDatasetId }) {
                        Surface(
                            onClick = {
                                scope.launch {
                                    val ok = vm.resetLabWorkspace(activeDatasetId)
                                    refreshTrigger++
                                    statusBanner = if (ok) "已重置 $activeDatasetId 工作副本至初始状态" else "重置工作副本失败"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = CloudSeaBlue,
                            modifier = Modifier.testTag("lab_reset_workspace_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "重置工作副本", tint = PrimaryAction, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重置副本", style = MaterialTheme.typography.labelMedium, color = PrimaryAction)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BUILTIN_DB_OPTIONS.forEach { (id, label) ->
                        FilterChip(
                            selected = activeDatasetId == id,
                            onClick = {
                                tabs[activeTabIdx] = tabs[activeTabIdx].copy(datasetId = id)
                            },
                            label = { Text("$label ($id)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryAction,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("lab_db_$id")
                        )
                    }
                    personalDatasets.forEach { pds ->
                        FilterChip(
                            selected = activeDatasetId == pds.id,
                            onClick = {
                                tabs[activeTabIdx] = tabs[activeTabIdx].copy(datasetId = pds.id)
                            },
                            label = { Text("个人:${pds.name}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryAction,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("lab_db_${pds.id}")
                        )
                    }
                }

                statusBanner?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LightMintGreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = SuccessText, modifier = Modifier.weight(1f))
                            IconButton(onClick = { statusBanner = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "关闭提示", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // 3 Mode Tabs: 数据表 | 代码 | 结果
            TabRow(
                selectedTabIndex = selectedSectionTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryAction
            ) {
                listOf("数据表 (${tableSchemas.size})", "代码 (${tabs.size})", "结果与历史").forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedSectionTab == idx,
                        onClick = { selectedSectionTab = idx },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.testTag("lab_section_tab_$idx")
                    )
                }
            }

            when (selectedSectionTab) {
                0 -> {
                    // Schema Explorer
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (tableSchemas.isEmpty()) {
                            item {
                                EmptyIllustrationCard(
                                    title = "暂无可见数据表",
                                    description = "当前数据库无表结构或正在初始化工作副本。"
                                )
                            }
                        } else {
                            items(tableSchemas, key = { it.first }) { (tblName, cols) ->
                                SeaSaltCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tblName,
                                            fontFamily = JetBrainsMonoFont,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAction
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                val sample = "SELECT *\nFROM $tblName\nLIMIT 20;"
                                                tabs[activeTabIdx] = tabs[activeTabIdx].copy(sql = sample)
                                                editorController.setExternalText(sample)
                                                selectedSectionTab = 1
                                            },
                                            modifier = Modifier.testTag("lab_select_tbl_$tblName")
                                        ) {
                                            Text("生成查询")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "字段 (${cols.size})：${cols.joinToString(", ")}",
                                        fontFamily = JetBrainsMonoFont,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Code Workspace (Multi-Tab + Snippets + Editor)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Query Tabs Strip
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tabs.forEachIndexed { idx, tab ->
                                    val selected = idx == activeTabIdx
                                    Surface(
                                        onClick = { activeTabIdx = idx },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) PrimaryAction else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (selected) PrimaryAction else MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.testTag("lab_query_tab_$idx")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = tab.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (tabs.size > 1) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "关闭标签",
                                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .let { m ->
                                                            m
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (tabs.size < 5) {
                                    Surface(
                                        onClick = {
                                            val newIdx = tabs.size + 1
                                            tabs.add(
                                                QueryTabState(
                                                    id = "tab_${UUID.randomUUID().toString().take(6)}",
                                                    title = "查询 $newIdx",
                                                    datasetId = activeDatasetId,
                                                    sql = ""
                                                )
                                            )
                                            activeTabIdx = tabs.lastIndex
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = CloudSeaBlue,
                                        modifier = Modifier.testTag("lab_add_tab_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "新建标签页", tint = PrimaryAction, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("新标签", style = MaterialTheme.typography.labelMedium, color = PrimaryAction)
                                        }
                                    }
                                }
                                if (tabs.size > 1) {
                                    OutlinedButton(
                                        onClick = {
                                            val removeAt = activeTabIdx
                                            tabs.removeAt(removeAt)
                                            activeTabIdx = (removeAt - 1).coerceAtLeast(0)
                                        }
                                    ) {
                                        Text("关闭当前")
                                    }
                                }
                            }
                        }

                        // Quick Snippets Strip
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LAB_SNIPPETS.forEach { (label, code) ->
                                    Surface(
                                        onClick = { editorController.insertSnippet(code) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = CloudSeaBlue
                                    ) {
                                        Text(
                                            text = "+ $label",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryAction,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Editor Box + Action Bar
                        item {
                            SqlCodeEditorBox(
                                controller = editorController,
                                isDarkEditor = settings.editorDarkTheme,
                                fontSizeSp = settings.editorFontSizeSp,
                                useCustomKeyboard = useCustomKeyboard,
                                onToggleCustomKeyboard = { useCustomKeyboard = !useCustomKeyboard },
                                onToggleFullscreen = { isFullscreen = true },
                                minHeightDp = 170
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            runActionBar()
                        }
                    }
                }

                else -> {
                    // Results & History
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val res: ExecutionResult? = currentTab.lastResult
                        item {
                            if (res == null) {
                                EmptyIllustrationCard(
                                    title = "尚无本次执行结果",
                                    description = "切换到「代码」页点击运行 SQL，即可查看表格数据并安全导出 CSV / JSON。",
                                    actionLabel = "去编写 SQL",
                                    onAction = { selectedSectionTab = 1 }
                                )
                            } else {
                                SeaSaltCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(
                                            text = if (res.success) "执行成功 · ${res.elapsedMs}ms" else "执行失败",
                                            backgroundColor = if (res.success) LightMintGreen else ErrorLightSurface,
                                            textColor = if (res.success) SuccessText else ErrorText
                                        )
                                        if (res.success && res.columns.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        pendingExportFormat = "csv"
                                                        exportLauncher.launch("${activeDatasetId}_result.csv")
                                                    },
                                                    modifier = Modifier.testTag("lab_export_csv_btn")
                                                ) {
                                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("CSV")
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        pendingExportFormat = "json"
                                                        exportLauncher.launch("${activeDatasetId}_result.json")
                                                    },
                                                    modifier = Modifier.testTag("lab_export_json_btn")
                                                ) {
                                                    Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("JSON")
                                                }
                                            }
                                        }
                                    }

                                    if (res.compatibilityNotes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(shape = RoundedCornerShape(10.dp), color = WarmBeige, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "MySQL 兼容转换：${res.compatibilityNotes.joinToString("；")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = WarningText,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (!res.success) {
                                        Surface(shape = RoundedCornerShape(10.dp), color = ErrorLightSurface, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = res.errorMessage ?: "执行失败",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ErrorText,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    } else {
                                        ResultDataTable(
                                            title = "查询结果 ($activeDatasetId)",
                                            columns = res.columns,
                                            rows = res.rows,
                                            totalRowsHint = res.totalRows
                                        )
                                    }
                                }
                            }
                        }

                        if (queryHistory.isNotEmpty()) {
                            item {
                                Text("最近查询记录 (${queryHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(queryHistory.take(20), key = { it.id }) { h ->
                                SeaSaltCard(
                                    onClick = {
                                        tabs[activeTabIdx] = tabs[activeTabIdx].copy(datasetId = h.datasetId, sql = h.sql)
                                        editorController.setExternalText(h.sql)
                                        selectedSectionTab = 1
                                    },
                                    modifier = Modifier.testTag("lab_history_${h.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(h.datasetId, CloudSeaBlue, PrimaryAction)
                                        Text(
                                            text = if (h.success) "${h.rowCount} 行 · ${h.elapsedMs}ms" else "报错",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (h.success) SuccessText else ErrorText
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = h.sql,
                                        fontFamily = JetBrainsMonoFont,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
