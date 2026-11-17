package com.example.ui.screens

import android.os.CancellationSignal
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.JudgeResult
import com.example.model.KeyboardMode
import com.example.model.QuestionFixture
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.DifficultyBadge
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    vm: MainViewModel,
    questionId: String,
    onBack: () -> Unit,
    onOpenQuestion: (String) -> Unit
) {
    val question = vm.contentRepo.getQuestion(questionId)
    val allQuestions = vm.questions()
    val settings by vm.settingsFlow.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val scope = rememberCoroutineScope()

    if (question == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("题目详情") },
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
                    title = "未找到题目",
                    description = "题号 $questionId 不存在。",
                    actionLabel = "返回",
                    onAction = onBack
                )
            }
        }
        return
    }

    val isFavorited = favorites.any { it.id == "question:${question.id}" }
    val isPrediction = question.type == "结果预测"
    val isChoice = question.type == "选择"
    val isBlank = question.type == "代码填空"

    var fixtures by remember(question.id) { mutableStateOf<List<QuestionFixture>>(emptyList()) }
    LaunchedEffect(question.id) {
        fixtures = vm.loadFixturesForQuestion(question)
    }

    val editorController = remember(question.id) {
        SqlEditorController(
            initialText = if (question.type == "代码纠错") question.initialCode else "",
            autoPair = settings.autoPair,
            autoUppercase = settings.autoUppercase,
            autoIndent = settings.autoIndent
        )
    }

    // Restore saved draft for coding questions
    LaunchedEffect(question.id) {
        val draft = vm.loadDraft("q:${question.id}")
        if (!draft.isNullOrBlank()) {
            editorController.setExternalText(draft)
        }
    }
    LaunchedEffect(editorController.value.text) {
        delay(500)
        if (!isChoice && !isPrediction) {
            vm.saveDraft("q:${question.id}", editorController.value.text, editorController.value.selection.start)
        }
    }

    var selectedOptionId by remember(question.id) { mutableStateOf("") }
    var blankInput by remember(question.id) { mutableStateOf("") }
    var predictionChoice by remember(question.id) { mutableStateOf("") }
    var hintsRevealed by remember(question.id) { mutableIntStateOf(0) }
    var viewedAnswer by remember(question.id) { mutableStateOf(false) }
    var hasSubmittedOnce by remember(question.id) { mutableStateOf(false) }

    var trialResult by remember(question.id) { mutableStateOf<ExecutionResult?>(null) }
    var judgeResult by remember(question.id) { mutableStateOf<JudgeResult?>(null) }
    var isRunning by remember(question.id) { mutableStateOf(false) }
    var activeSignal by remember { mutableStateOf<CancellationSignal?>(null) }
    var useCustomKeyboard by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Dynamic field groups from public fixture or enterprise schema
    val fieldGroups = remember(fixtures, question.datasetId) {
        val pubTables = fixtures.firstOrNull { it.variant == "public" }?.tables.orEmpty()
        if (pubTables.isNotEmpty()) {
            pubTables.map { t -> FieldGroup(t.name, t.columns.map { it.name }) }
        } else if (!question.datasetId.isNullOrBlank()) {
            val clean = question.datasetId.removeSuffix("-v2")
            val schema = vm.schemas()[clean]
            schema?.tables?.map { (tName, tInfo) ->
                FieldGroup(tName, tInfo.columns.map { it.name })
            }.orEmpty()
        } else emptyList()
    }

    val currentIdx = allQuestions.indexOfFirst { it.id == question.id }
    val prevQ = if (currentIdx > 0) allQuestions[currentIdx - 1] else null
    val nextQ = if (currentIdx in 0 until allQuestions.lastIndex) allQuestions[currentIdx + 1] else null

    fun currentAnswerPayload(): String = when {
        isChoice -> selectedOptionId
        isPrediction -> predictionChoice
        isBlank -> blankInput
        else -> editorController.value.text
    }

    val actionButtons: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isChoice && !isPrediction) {
                if (isRunning) {
                    OutlinedButton(
                        onClick = { activeSignal?.cancel() },
                        modifier = Modifier.weight(1f).testTag("question_stop_btn")
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("停止运行")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val sig = CancellationSignal()
                                activeSignal = sig
                                isRunning = true
                                val sqlInput = if (isBlank) blankInput else editorController.value.text
                                trialResult = vm.trialRunQuestion(question, fixtures, sqlInput, sig)
                                isRunning = false
                                activeSignal = null
                            }
                        },
                        enabled = question.supportStatus != "仅理论",
                        modifier = Modifier.weight(1f).testTag("question_trial_run_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("试运行")
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isRunning = true
                        val res = vm.submitQuestion(
                            question = question,
                            fixtures = fixtures,
                            userInput = currentAnswerPayload(),
                            hintsUsed = hintsRevealed,
                            viewedAnswer = viewedAnswer
                        )
                        judgeResult = res
                        hasSubmittedOnce = true
                        isRunning = false
                    }
                },
                enabled = !isRunning && currentAnswerPayload().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                modifier = Modifier.weight(1.2f).testTag("question_submit_btn")
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("提交判题")
            }
        }
    }

    if (isFullscreen && !isChoice && !isPrediction && !isBlank) {
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
            actionBar = actionButtons
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${question.id.uppercase()} · ${question.type}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("question_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            vm.toggleFavorite("question", question.id, question.title, "${question.type} · ${question.difficulty}")
                        },
                        modifier = Modifier.testTag("question_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "收藏题目",
                            tint = if (isFavorited) PrimaryAction else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (!isChoice && !isPrediction && !isBlank && useCustomKeyboard) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Question Header & Statement
            item {
                SeaSaltCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DifficultyBadge(question.difficulty)
                        CompatibilityBadge(question.supportStatus)
                        if (question.requiredAstFeatures.isNotEmpty()) {
                            StatusPill(
                                text = "专项: ${question.requiredAstFeatures.joinToString()}",
                                backgroundColor = LightMintGreen,
                                textColor = SuccessText
                            )
                        }
                        if (!question.taskId.isNullOrBlank()) {
                            StatusPill(question.taskId, WarmBeige, WarningText)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = question.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (question.context.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = question.context,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (question.requirements.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CloudSeaBlue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = question.requirements,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Public Fixture Tables Preview (only public data, never hidden fixtures)
            val publicTables = fixtures.firstOrNull { it.variant == "public" }?.tables.orEmpty()
            if (publicTables.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        publicTables.forEach { tbl ->
                            ResultDataTable(
                                title = "公开数据表：${tbl.name}",
                                columns = tbl.columns.map { it.name },
                                rows = tbl.rows.map { r -> r.map { it?.toString()?.trim('"') } },
                                maxDisplayRows = 12
                            )
                        }
                    }
                }
            }

            // Answer Input Area by Question Type
            item {
                SeaSaltCard {
                    when {
                        isChoice -> {
                            Text("请选择最准确的答案：", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                question.options.forEach { opt ->
                                    val selected = selectedOptionId == opt.id
                                    Surface(
                                        onClick = { selectedOptionId = opt.id },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) CloudSeaBlue else MaterialTheme.colorScheme.background,
                                        border = BorderStroke(1.dp, if (selected) PrimaryAction else MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("question_option_${opt.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selected,
                                                onClick = { selectedOptionId = opt.id }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "${opt.id}. ${opt.text}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                                if (hasSubmittedOnce && opt.explanation.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = opt.explanation,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (opt.id == question.correctOptionId) SuccessText else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        isPrediction -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = WarmBeige,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "结果预测题规则：提交前禁止运行 SQL 与提前查看参考答案，请根据数据表手算推演。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningText,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (question.initialCode.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CloudSeaBlue,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = question.initialCode,
                                        fontFamily = JetBrainsMonoFont,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            question.predictionOptions.forEachIndexed { idx, opt ->
                                val selected = predictionChoice == opt
                                Surface(
                                    onClick = { predictionChoice = opt },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) CloudSeaBlue else MaterialTheme.colorScheme.background,
                                    border = BorderStroke(1.dp, if (selected) PrimaryAction else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("prediction_option_$idx")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selected, onClick = { predictionChoice = opt })
                                        Text(opt, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        isBlank -> {
                            Text("代码填空上下文：", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CloudSeaBlue,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = question.initialCode,
                                    fontFamily = JetBrainsMonoFont,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = blankInput,
                                onValueChange = { blankInput = it },
                                label = { Text("填入 ____ 处的 SQL 内容（多空用 || 分隔）") },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFont),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("question_blank_input")
                            )
                        }

                        else -> {
                            SqlCodeEditorBox(
                                controller = editorController,
                                isDarkEditor = settings.editorDarkTheme,
                                fontSizeSp = settings.editorFontSizeSp,
                                useCustomKeyboard = useCustomKeyboard,
                                onToggleCustomKeyboard = { useCustomKeyboard = !useCustomKeyboard },
                                onToggleFullscreen = { isFullscreen = true },
                                minHeightDp = 135
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    actionButtons()
                }
            }

            // 3-Level Progressive Hints & View Answer (locked before submit on Prediction!)
            item {
                SeaSaltCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (question.hints.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    if (hintsRevealed < question.hints.size) hintsRevealed++
                                },
                                enabled = hintsRevealed < question.hints.size,
                                modifier = Modifier.weight(1f).testTag("question_hint_btn")
                            ) {
                                Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("提示 ($hintsRevealed/${question.hints.size})")
                            }
                        }

                        val canViewAnswer = !isPrediction || hasSubmittedOnce
                        OutlinedButton(
                            onClick = { viewedAnswer = true },
                            enabled = canViewAnswer && !viewedAnswer,
                            modifier = Modifier.weight(1f).testTag("question_view_answer_btn")
                        ) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (!canViewAnswer) "提交后可看答案" else if (viewedAnswer) "已看答案" else "查看参考答案")
                        }
                    }

                    if (hintsRevealed > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            question.hints.take(hintsRevealed).forEachIndexed { i, h ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WarmBeige,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "第 ${i + 1} 级提示：$h",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarningText,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (viewedAnswer) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CloudSeaBlue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "参考答案（注意：查看答案后本次通过不计入“独立完成率”）",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryAction,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val ansText = when {
                                    isChoice -> "正确选项：${question.correctOptionId}"
                                    isPrediction -> question.predictionAnswerText ?: ""
                                    isBlank -> "填空答案：${question.blankAnswerHint}\n完整语句：${question.referenceSolutions.firstOrNull()?.mysqlSql.orEmpty()}"
                                    else -> question.referenceSolutions.firstOrNull()?.mysqlSql.orEmpty()
                                }
                                Text(
                                    text = ansText,
                                    fontFamily = JetBrainsMonoFont,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (question.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(question.explanation, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            // Trial Run Result (Non-scoring)
            trialResult?.let { res ->
                item {
                    SeaSaltCard {
                        Text(
                            text = "试运行输出（仅测试，不计正式成绩 · ${res.elapsedMs}ms）",
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryAction
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!res.success) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ErrorLightSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = res.errorMessage ?: "运行出错",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorText,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            ResultDataTable(
                                title = "试运行结果",
                                columns = res.columns,
                                rows = res.rows,
                                totalRowsHint = res.totalRows
                            )
                        }
                    }
                }
            }

            // Formal Judge Result
            judgeResult?.let { jr ->
                item {
                    SeaSaltCard(
                        containerColor = if (jr.passed) LightMintGreen else ErrorLightSurface
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (jr.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = jr.summary,
                                tint = if (jr.passed) SuccessText else ErrorText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = jr.summary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (jr.passed) SuccessText else ErrorText
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = jr.chineseFeedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (jr.fixtureResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                jr.fixtureResults.forEach { fx ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(fx.fixtureName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    text = if (fx.passed) "通过" else "未通过",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (fx.passed) SuccessText else ErrorText
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(fx.message, style = MaterialTheme.typography.bodySmall)
                                            if (!fx.passed && fx.visibility == "public" && fx.expectedColumns.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                ResultDataTable(
                                                    title = "公开预期对比（前12行）",
                                                    columns = fx.expectedColumns,
                                                    rows = fx.expectedRowsPreview
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

            // Prev / Next Question Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { prevQ?.let { onOpenQuestion(it.id) } },
                        enabled = prevQ != null,
                        modifier = Modifier.weight(1f).testTag("question_prev_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("上一题")
                    }
                    Button(
                        onClick = { nextQ?.let { onOpenQuestion(it.id) } },
                        enabled = nextQ != null,
                        modifier = Modifier.weight(1f).testTag("question_next_btn")
                    ) {
                        Text("下一题")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
