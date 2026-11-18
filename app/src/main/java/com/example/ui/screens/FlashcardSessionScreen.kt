package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Swipe
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.FlipCard3DContainer
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
                    Column {
                        Text(
                            text = if (queue.isNotEmpty() && index < queue.size) {
                                "${mode.label} · ${index + 1}/${queue.size}"
                            } else "${mode.label}复习",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (queue.isNotEmpty() && index < queue.size) {
                            Text(
                                text = if (countTowardsSchedule) "FSRS 间隔调度中" else "自由翻卡模式",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("正在编排 FSRS 记忆队列…", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        if (queue.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
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
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("本轮翻卡学习已完成！", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (countTowardsSchedule) "已完成 ${queue.size} 张闪卡，FSRS-4.5 记忆间隔已实时更新。"
                            else "已完成 ${queue.size} 张自由翻卡浏览（未改变原有记忆调度）。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("fc_session_done_btn")
                        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            // Subtle Session Telemetry & Gesture Cue Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(
                        text = if (stateEntity == null) "新卡" else "复习 ${stateEntity.reps} 次",
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.primary
                    )
                    if (stateEntity != null) {
                        StatusPill(
                            text = "稳定度 ${"%.1f".format(stateEntity.stability)} 天",
                            backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                            textColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Swipe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "点击或左右滑翻面",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main Stacked-Deck 3D Flip Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val cardScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(cardScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                    ) {
                        // Two ghost deck cards stacked beneath when queue still has upcoming cards
                        if (index + 2 < queue.size) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth(0.86f)
                                    .height(48.dp)
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 14.dp)
                            ) {}
                        }
                        if (index + 1 < queue.size) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth(0.93f)
                                    .height(48.dp)
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 7.dp)
                            ) {}
                        }

                        // Card-to-card animated transition + 3D Y-axis Flip inside
                        AnimatedContent(
                            targetState = currentCard,
                            transitionSpec = {
                                (slideInHorizontally(tween(240)) { it / 3 } + fadeIn(tween(220)))
                                    .togetherWith(slideOutHorizontally(tween(180)) { -it / 4 } + fadeOut(tween(160)))
                            },
                            label = "deck_card_advance"
                        ) { activeCard ->
                            FlipCard3DContainer(
                                isFlipped = isFlipped,
                                onFlip = { isFlipped = !isFlipped },
                                enableSwipeToFlip = true,
                                cornerRadius = 22.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fc_card_flip_surface"),
                                frontContent = {
                                    CardFrontFace(
                                        card = activeCard,
                                        loadedTables = loadedTables
                                    )
                                },
                                backContent = {
                                    CardBackFace(
                                        card = activeCard,
                                        exampleResult = exampleResult,
                                        onRunExample = {
                                            scope.launch {
                                                exampleResult = vm.runFlashcardExample(activeCard)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Bottom Action Dock with animated switch between Flip Bar and 4 FSRS Rating Cards
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 2.dp
            ) {
                AnimatedContent(
                    targetState = isFlipped,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "fc_dock_mode"
                ) { flipped ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (!flipped) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isFlipped = false
                                        index++
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .height(48.dp)
                                        .testTag("fc_skip_btn")
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("跳过")
                                }
                                Button(
                                    onClick = { isFlipped = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(48.dp)
                                        .testTag("fc_flip_btn")
                                ) {
                                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("翻转查看答案", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = { isFlipped = false },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.size(width = 44.dp, height = 54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "翻回正面",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                FlashcardRating.entries.forEach { rating ->
                                    val preview = previews[rating]
                                    val interval = if (countTowardsSchedule) (preview?.intervalLabel ?: "—") else "浏览"
                                    val (bg, fg) = when (rating) {
                                        FlashcardRating.AGAIN -> ErrorLightSurface to ErrorText
                                        FlashcardRating.HARD -> WarmBeige to WarningText
                                        FlashcardRating.GOOD -> CloudSeaBlue to PrimaryAction
                                        FlashcardRating.EASY -> LightMintGreen to SuccessText
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
                                        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("fc_rate_${rating.name.lowercase()}")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 4.dp),
                                            verticalArrangement = Arrangement.Center,
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
}

@Composable
private fun CardFrontFace(
    card: FlashcardItem,
    loadedTables: List<SourceTable>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(card.id.uppercase(), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    StatusPill(card.type, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    CompatibilityBadge(card.supportStatus)
                }
                StatusPill(
                    text = "正面 · 待翻卡",
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    textColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = card.front.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!card.front.code.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = card.front.code,
                        fontFamily = JetBrainsMonoFont,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (loadedTables.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                loadedTables.take(2).forEach { t ->
                    ResultDataTable(
                        title = "示例表：${t.name}",
                        columns = t.columns.map { it.name },
                        rows = t.rows.map { r -> r.map { it?.toString()?.trim('"') } },
                        maxDisplayRows = 6
                    )
                }
            }

            if (card.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    card.tags.take(3).forEach { tag ->
                        StatusPill(
                            text = "#$tag",
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "轻触卡片或左右滑动 · 3D 翻面核对答案",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CardBackFace(
    card: FlashcardItem,
    exampleResult: ExecutionResult?,
    onRunExample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill("背面 · 答案解析", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary)
                StatusPill(card.id.uppercase(), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "↺ 轻触翻回正面",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "原题：${card.front.question}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "核心答案",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.back.answer,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (card.back.explanation.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "原理拆解",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.back.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (card.back.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "补充提示：${card.back.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        val displaySql = card.back.code?.takeIf { it.isNotBlank() } ?: card.back.sqliteCode
        if (!displaySql.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displaySql,
                    fontFamily = JetBrainsMonoFont,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (!card.back.code.isNullOrBlank() || !card.back.sqliteCode.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRunExample,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("fc_run_example_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("本地实操运行示例")
            }
        }

        exampleResult?.let { res ->
            Spacer(modifier = Modifier.height(12.dp))
            if (!res.success) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = res.errorMessage ?: "运行失败",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
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
