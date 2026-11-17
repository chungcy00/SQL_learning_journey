package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.FsrsEngine
import com.example.model.ExecutionResult
import com.example.model.FlashcardItem
import com.example.model.FlashcardRating
import com.example.model.FlashcardReviewMode
import com.example.model.SourceTable
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.ResultDataTable
import com.example.ui.components.SeaSaltCard
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardSessionScreen(
    vm: MainViewModel,
    modeId: String,
    chapterId: String?,
    countTowardsSchedule: Boolean,
    onBack: () -> Unit
) {
    val mode = FlashcardReviewMode.entries.firstOrNull { it.id == modeId } ?: FlashcardReviewMode.DUE
    val settings by vm.settingsFlow.collectAsState()
    val fcStates by vm.flashcardStates.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val scope = rememberCoroutineScope()

    var queue by remember { mutableStateOf<List<FlashcardItem>>(emptyList()) }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var lastEventId by remember { mutableStateOf<String?>(null) }
    var lastRatedIndex by remember { mutableIntStateOf(-1) }
    var exampleResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var loadedTables by remember { mutableStateOf<List<SourceTable>>(emptyList()) }

    LaunchedEffect(modeId, chapterId, countTowardsSchedule) {
        val (sess, cards) = vm.startOrResumeFlashcardSession(mode, chapterId, countTowardsSchedule)
        sessionId = sess?.sessionId
        queue = cards
        initialized = true
    }

    val currentCard = queue.getOrNull(index)
    LaunchedEffect(currentCard?.id) {
        exampleResult = null
        loadedTables = if (currentCard != null) {
            if (currentCard.front.sourceTables.isNotEmpty()) currentCard.front.sourceTables
            else vm.contentRepo.loadFlashcardFixtureTables(currentCard)
        } else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (queue.isNotEmpty() && index < queue.size) {
                            "${mode.label} · ${index + 1}/${queue.size}"
                        } else "${mode.label}复习"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("fc_session_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出复习")
                    }
                },
                actions = {
                    if (lastEventId != null && lastRatedIndex >= 0) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    lastEventId?.let { eid ->
                                        if (vm.undoFlashcardEvent(eid)) {
                                            index = lastRatedIndex
                                            isFlipped = true
                                            lastEventId = null
                                            lastRatedIndex = -1
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("fc_session_undo_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤回上次评分")
                        }
                    }
                    if (currentCard != null) {
                        val isFav = favorites.any { it.id == "flashcard:${currentCard.id}" }
                        IconButton(
                            onClick = {
                                vm.toggleFavorite("flashcard", currentCard.id, currentCard.front.question, currentCard.type)
                            }
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "收藏闪卡",
                                tint = if (isFav) PrimaryAction else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (!initialized) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("正在编排 FSRS 记忆队列…", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        if (queue.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                EmptyIllustrationCard(
                    illustrationRes = R.drawable.illus_state_flashcard_review,
                    title = "当前模式暂无待复习闪卡",
                    description = "「${mode.label}」队列为空，可尝试切换为「新卡」或「随机」模式。",
                    actionLabel = "返回闪卡主页",
                    onAction = onBack
                )
            }
            return@Scaffold
        }

        if (currentCard == null) {
            // Session Complete
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SeaSaltCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.illus_state_learning_complete),
                            contentDescription = "本轮完成",
                            modifier = Modifier.size(136.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("本轮复习已完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (countTowardsSchedule) "已完成 ${queue.size} 张闪卡，FSRS-4.5 记忆间隔已实时更新。"
                            else "已完成 ${queue.size} 张自由浏览（未改变原有记忆调度）。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("fc_session_done_btn")) {
                            Text("返回闪卡中心")
                        }
                    }
                }
            }
            return@Scaffold
        }

        val stateEntity = fcStates.firstOrNull { it.cardId == currentCard.id }
        val currentState = stateEntity?.let {
            FsrsEngine.newCardState(it.cardId).copy(
                dueAtMs = it.dueAtMs,
                stability = it.stability,
                difficulty = it.difficulty,
                lastReviewAtMs = it.lastReviewAtMs,
                reps = it.reps,
                lapses = it.lapses,
                state = it.state
            )
        } ?: FsrsEngine.newCardState(currentCard.id)

        val previews = remember(currentState, settings.desiredRetention, isFlipped) {
            FsrsEngine.previewAllRatings(currentState, settings.desiredRetention)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LinearProgressIndicator(
                progress = { (index + 1).toFloat() / queue.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = PrimaryAction,
                trackColor = CloudSeaBlue
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SeaSaltCard(
                        onClick = { isFlipped = !isFlipped },
                        modifier = Modifier.testTag("fc_card_flip_surface")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(currentCard.id.uppercase(), CloudSeaBlue, PrimaryAction)
                                StatusPill(currentCard.type, WarmBeige, WarningText)
                                CompatibilityBadge(currentCard.supportStatus)
                            }
                            Text(
                                text = if (isFlipped) "背面 · 答案解析" else "正面 · 点击翻面",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryAction
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = currentCard.front.question,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (!currentCard.front.code.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CloudSeaBlue,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = currentCard.front.code,
                                    fontFamily = JetBrainsMonoFont,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (loadedTables.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            loadedTables.take(2).forEach { t ->
                                ResultDataTable(
                                    title = "示例表：${t.name}",
                                    columns = t.columns.map { it.name },
                                    rows = t.rows.map { r -> r.map { it?.toString()?.trim('"') } },
                                    maxDisplayRows = 6
                                )
                            }
                        }

                        if (isFlipped) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LightMintGreen,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("答案要点", style = MaterialTheme.typography.labelLarge, color = SuccessText, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(currentCard.back.answer, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    if (currentCard.back.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(currentCard.back.explanation, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (!currentCard.back.code.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = currentCard.back.code,
                                                fontFamily = JetBrainsMonoFont,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!currentCard.back.code.isNullOrBlank() || !currentCard.back.sqliteCode.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            exampleResult = vm.runFlashcardExample(currentCard)
                                        }
                                    },
                                    modifier = Modifier.testTag("fc_run_example_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("本地实操运行示例")
                                }
                            }
                        }
                    }
                }

                exampleResult?.let { res ->
                    item {
                        SeaSaltCard {
                            if (!res.success) {
                                Surface(shape = RoundedCornerShape(10.dp), color = ErrorLightSurface, modifier = Modifier.fillMaxWidth()) {
                                    Text(res.errorMessage ?: "运行失败", color = ErrorText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                                }
                            } else {
                                ResultDataTable(
                                    title = "本地执行结果（${res.elapsedMs}ms）",
                                    columns = res.columns,
                                    rows = res.rows
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Dock: Flip first, then 4 FSRS Rating buttons + Skip
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (!isFlipped) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isFlipped = false
                                    index++
                                },
                                modifier = Modifier.weight(0.9f).testTag("fc_skip_btn")
                            ) {
                                Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("跳过")
                            }
                            Button(
                                onClick = { isFlipped = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                                modifier = Modifier.weight(2f).testTag("fc_flip_btn")
                            ) {
                                Icon(Icons.Default.Flip, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("翻转查看答案")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FlashcardRating.entries.forEach { rating ->
                                val preview = previews[rating]
                                val interval = if (countTowardsSchedule) (preview?.intervalLabel ?: "—") else "浏览"
                                val bg = when (rating) {
                                    FlashcardRating.AGAIN -> ErrorLightSurface
                                    FlashcardRating.HARD -> WarmBeige
                                    FlashcardRating.GOOD -> CloudSeaBlue
                                    FlashcardRating.EASY -> LightMintGreen
                                }
                                val fg = when (rating) {
                                    FlashcardRating.AGAIN -> ErrorText
                                    FlashcardRating.HARD -> WarningText
                                    FlashcardRating.GOOD -> PrimaryAction
                                    FlashcardRating.EASY -> SuccessText
                                }
                                Surface(
                                    onClick = {
                                        scope.launch {
                                            val (_, evt) = vm.rateCardInSession(
                                                card = currentCard,
                                                rating = rating,
                                                sessionId = sessionId,
                                                countTowardsSchedule = countTowardsSchedule
                                            )
                                            lastEventId = evt?.eventId
                                            lastRatedIndex = index
                                            isFlipped = false
                                            index++
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = bg,
                                    border = BorderStroke(1.dp, fg.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("fc_rate_${rating.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(rating.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = fg)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(interval, style = MaterialTheme.typography.labelSmall, color = fg)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
