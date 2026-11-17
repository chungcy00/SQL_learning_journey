package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExecutionResult
import com.example.model.SourceTable
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.ResultDataTable
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StepFlowDiagram
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeDetailScreen(
    vm: MainViewModel,
    kpId: String,
    onBack: () -> Unit,
    onOpenKnowledgePoint: (String) -> Unit,
    onOpenQuestion: (String) -> Unit,
    onOpenLabWithSql: (String?, String) -> Unit
) {
    val kp = vm.contentRepo.getKnowledgePoint(kpId)
    val allKps = vm.knowledgePoints()
    val completedKps by vm.completedKpIds.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val notes by vm.notes.collectAsState()
    val scope = rememberCoroutineScope()

    if (kp == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("知识点详情") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { inner ->
            Column(modifier = Modifier.padding(inner).padding(16.dp)) {
                EmptyIllustrationCard(
                    title = "未找到该知识点",
                    description = "知识点编号 $kpId 不存在。",
                    actionLabel = "返回",
                    onAction = onBack
                )
            }
        }
        return
    }

    LaunchedEffect(kp.id) {
        vm.markKpVisited(kp.id)
    }

    val isCompleted = kp.id in completedKps
    val isFavorited = favorites.any { it.id == "kp:${kp.id}" }
    val mastery = vm.computeMastery(kp)

    var loadedTables by remember(kp.id) { mutableStateOf<List<SourceTable>>(kp.sourceTables) }
    LaunchedEffect(kp.id) {
        if (kp.sourceTables.isEmpty() && kp.fixtureIds.isNotEmpty()) {
            loadedTables = vm.contentRepo.loadLessonFixtureTables(kp.fixtureIds.first())
        }
    }

    var editableSql by remember(kp.id) {
        mutableStateOf(kp.mysqlSql.ifBlank { kp.sqliteSql })
    }
    var runResult by remember(kp.id) { mutableStateOf<ExecutionResult?>(null) }
    var isRunning by remember(kp.id) { mutableStateOf(false) }

    val savedNote = notes.firstOrNull { it.id == "kp:${kp.id}" }?.content ?: ""
    var noteDraft by remember(kp.id, savedNote) { mutableStateOf(savedNote) }
    var noteSavedBanner by remember { mutableStateOf(false) }

    val currentIndex = allKps.indexOfFirst { it.id == kp.id }
    val prevKp = if (currentIndex > 0) allKps[currentIndex - 1] else null
    val nextKp = if (currentIndex in 0 until allKps.lastIndex) allKps[currentIndex + 1] else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kp.id.uppercase(), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("kp_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            vm.toggleFavorite("kp", kp.id, kp.title, kp.chapterId)
                        },
                        modifier = Modifier.testTag("kp_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isFavorited) "取消收藏" else "收藏知识点",
                            tint = if (isFavorited) PrimaryAction else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Header + Completion CTA
            item {
                SeaSaltCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompatibilityBadge(kp.compatibility?.status ?: "直接兼容")
                        StatusPill(
                            text = if (mastery.score != null) "掌握度 ${mastery.score}% (${mastery.label})" else "掌握度：尚未评估",
                            backgroundColor = CloudSeaBlue,
                            textColor = PrimaryAction
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = kp.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (kp.learningObjective.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = kp.learningObjective,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryAction,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { vm.toggleKpCompleted(kp) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) SuccessText else PrimaryAction
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kp_complete_toggle_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCompleted) "已完成本节学习（点击可取消）" else "完成本节学习")
                    }
                }
            }

            // Concept & Syntax
            item {
                SeaSaltCard {
                    Text("核心概念", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(kp.concept.ifBlank { kp.learningObjective }, style = MaterialTheme.typography.bodyMedium)

                    if (kp.syntax.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("MySQL 8.4 标准语法", style = MaterialTheme.typography.labelLarge, color = PrimaryAction)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CloudSeaBlue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = kp.syntax,
                                fontFamily = JetBrainsMonoFont,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    if (kp.parameters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        kp.parameters.forEach { (k, v) ->
                            Text(
                                text = "• $k：$v",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!kp.compatibility?.detail.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarmBeige,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "方言说明：${kp.compatibility?.detail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningText,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Step Flow Diagram
            if (kp.executionSteps.isNotEmpty()) {
                item {
                    StepFlowDiagram(steps = kp.executionSteps)
                }
            }

            // Source Tables Preview
            if (loadedTables.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        loadedTables.forEach { tbl ->
                            ResultDataTable(
                                title = "示例数据表：${tbl.name}",
                                columns = tbl.columns.map { it.name },
                                rows = tbl.rows.map { r -> r.map { it?.toString()?.trim('"') } },
                                maxDisplayRows = 12
                            )
                        }
                    }
                }
            }

            // Runnable SQL Example
            if (kp.mysqlSql.isNotBlank() || kp.sqliteSql.isNotBlank()) {
                item {
                    SeaSaltCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("示例 SQL 实操", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedButton(
                                onClick = {
                                    onOpenLabWithSql(kp.datasetId, editableSql)
                                },
                                modifier = Modifier.testTag("kp_open_in_lab_btn")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("去实验室")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editableSql,
                            onValueChange = { editableSql = it },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFont),
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kp_example_sql_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isRunning = true
                                        runResult = vm.runKpExample(kp, editableSql)
                                        isRunning = false
                                    }
                                },
                                enabled = !isRunning,
                                modifier = Modifier.testTag("kp_run_example_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isRunning) "运行中…" else "运行示例")
                            }
                            if (kp.mysqlSql.isNotBlank() && editableSql != kp.mysqlSql) {
                                OutlinedButton(onClick = { editableSql = kp.mysqlSql }) {
                                    Text("恢复原例")
                                }
                            }
                        }

                        runResult?.let { res ->
                            Spacer(modifier = Modifier.height(10.dp))
                            if (!res.success) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ErrorLightSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = res.errorMessage ?: "执行失败",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorText,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                if (res.compatibilityNotes.isNotEmpty()) {
                                    Text(
                                        text = "兼容转换：${res.compatibilityNotes.joinToString("；")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                ResultDataTable(
                                    title = "执行结果（${res.elapsedMs}ms）",
                                    columns = res.columns,
                                    rows = res.rows,
                                    totalRowsHint = res.totalRows
                                )
                            }
                        }
                    }
                }
            }

            // Common Error & Use Case
            if (kp.commonError != null || kp.comparison.isNotBlank()) {
                item {
                    SeaSaltCard(containerColor = WarmBeige) {
                        if (kp.commonError != null) {
                            Text("常见误区与修正", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WarningText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("误区：${kp.commonError.example}", style = MaterialTheme.typography.bodySmall)
                            Text("修正：${kp.commonError.correction}", style = MaterialTheme.typography.bodySmall, color = SuccessText)
                        }
                        if (kp.comparison.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("易混淆对比：${kp.comparison}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Related Questions
            if (kp.relatedQuestionIds.isNotEmpty()) {
                item {
                    SeaSaltCard {
                        Text("配套练习题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            kp.relatedQuestionIds.forEach { qId ->
                                val q = vm.contentRepo.getQuestion(qId)
                                if (q != null) {
                                    Surface(
                                        onClick = { onOpenQuestion(q.id) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = CloudSeaBlue,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("kp_linked_question_${q.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(q.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                                Text("${q.type} · ${q.difficulty}", style = MaterialTheme.typography.labelSmall, color = PrimaryAction)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "去做题", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Personal Notes
            item {
                SeaSaltCard {
                    Text("我的学习笔记", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = {
                            noteDraft = it
                            noteSavedBanner = false
                        },
                        placeholder = { Text("记录核心口径、SQL 易错点或面试总结…") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kp_note_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (noteSavedBanner) {
                            Text("笔记已保存至本地", style = MaterialTheme.typography.labelSmall, color = SuccessText)
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        Button(
                            onClick = {
                                vm.saveNote("kp", kp.id, noteDraft)
                                noteSavedBanner = true
                            },
                            modifier = Modifier.testTag("kp_save_note_btn")
                        ) {
                            Text("保存笔记")
                        }
                    }
                }
            }

            // Prev / Next KP Navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { prevKp?.let { onOpenKnowledgePoint(it.id) } },
                        enabled = prevKp != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kp_prev_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上一节")
                    }
                    Button(
                        onClick = { nextKp?.let { onOpenKnowledgePoint(it.id) } },
                        enabled = nextKp != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kp_next_btn")
                    ) {
                        Text("下一节")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
