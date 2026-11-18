package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FlashcardReviewMode
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.FlipCard3DContainer
import com.example.ui.components.MiniFlipFlashcardItem
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun FlashcardsScreen(
    vm: MainViewModel,
    onStartSession: (modeId: String, chapterId: String?, countTowardsSchedule: Boolean) -> Unit,
    onOpenCustomManager: (editCardId: String?) -> Unit
) {
    val builtinCards = vm.flashcards()
    val customCards by vm.userFlashcards.collectAsState()
    val fcStates by vm.flashcardStates.collectAsState()
    val stats by vm.statsOverview.collectAsState()
    val settings by vm.settingsFlow.collectAsState()
    val chapters = vm.chapters()
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(FlashcardReviewMode.DUE) }
    var selectedChapter by remember { mutableStateOf("ch01") }
    var countTowardsSchedule by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var browserChapterFilter by remember { mutableStateOf("全部") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    // Spotlight inline quick-flip deck state
    var spotlightOffset by remember { mutableIntStateOf(0) }
    var spotlightFlipped by remember { mutableStateOf(false) }

    // Per-card flip state inside the browser list
    val flippedBrowserCards = remember { mutableStateMapOf<String, Boolean>() }

    val now = System.currentTimeMillis()
    val stateMap = remember(fcStates) { fcStates.associateBy { it.cardId } }
    val dueCount = remember(stateMap, now) { stateMap.values.count { it.dueAtMs <= now } }
    val newCount = remember(stateMap, builtinCards, customCards) {
        (builtinCards.size + customCards.size - stateMap.size).coerceAtLeast(0)
    }

    val spotlightPool = remember(builtinCards, stateMap, now) {
        val due = builtinCards.filter { val s = stateMap[it.id]; s != null && s.dueAtMs <= now }
        val fresh = builtinCards.filter { it.id !in stateMap }
        (due + fresh + builtinCards).distinctBy { it.id }
    }
    val spotlightCard = spotlightPool.getOrNull(
        if (spotlightPool.isEmpty()) 0 else spotlightOffset % spotlightPool.size
    )

    val filteredCards = remember(builtinCards, searchQuery, browserChapterFilter) {
        val base = builtinCards.filter { card ->
            val matchChapter = browserChapterFilter == "全部" || card.chapterId == browserChapterFilter
            val matchQuery = searchQuery.isBlank() ||
                card.front.question.contains(searchQuery, true) ||
                card.back.answer.contains(searchQuery, true) ||
                card.id.contains(searchQuery, true) ||
                card.tags.any { t -> t.contains(searchQuery, true) }
            matchChapter && matchQuery
        }
        if (searchQuery.isBlank() && browserChapterFilter == "全部") base.take(40) else base
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "记忆闪卡",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusPill(
                            text = "FSRS-4.5",
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            textColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "3D 翻卡精学 · 200 内置卡 + ${customCards.size} 自定义卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { onOpenCustomManager(null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fc_custom_manage_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("自定义 (${customCards.size})")
                }
            }
        }

        // Sea-Salt Gradient Study Hub Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "今日记忆调度台",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DeckMetricTile(
                                    label = "待复习",
                                    value = "$dueCount",
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                DeckMetricTile(
                                    label = "未学新卡",
                                    value = "$newCount",
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                                DeckMetricTile(
                                    label = "已掌握",
                                    value = "${stats.builtinCardsMastered}",
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.illus_state_flashcard_review),
                            contentDescription = "闪卡复习",
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "累计涉猎 ${stats.builtinCardsReviewedUnique}/200 张",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "目标保持率 ${(settings.desiredRetention * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (stats.builtinCardsReviewedUnique / 200f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    // 7 Review Modes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FlashcardReviewMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("fc_mode_${mode.id}")
                            )
                        }
                    }

                    AnimatedVisibility(visible = selectedMode == FlashcardReviewMode.CHAPTER) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chapters.forEach { ch ->
                                FilterChip(
                                    selected = selectedChapter == ch.id,
                                    onClick = { selectedChapter = ch.id },
                                    label = { Text("Ch${ch.number}") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (countTowardsSchedule) "正式调度（自动计算下次复习时间）" else "自由浏览（仅翻卡，不改间隔）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = countTowardsSchedule,
                            onCheckedChange = { countTowardsSchedule = it },
                            modifier = Modifier.testTag("fc_schedule_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onStartSession(
                                selectedMode.id,
                                if (selectedMode == FlashcardReviewMode.CHAPTER) selectedChapter else null,
                                countTowardsSchedule
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("fc_start_session_btn")
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "进入「${selectedMode.label}」3D 翻卡学习",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Inline 3D Spotlight Quick-Flip Deck right inside the tab!
        if (spotlightCard != null) {
            item {
                SectionHeader(
                    title = "随手速翻一张",
                    subtitle = "轻触卡片或左右滑体验 3D 翻卡记忆",
                    trailing = {
                        TextButton(
                            onClick = {
                                spotlightFlipped = false
                                spotlightOffset++
                            },
                            modifier = Modifier.testTag("fc_spotlight_next_btn")
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("换一张")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Stacked deck backing behind the active spotlight flip card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(40.dp)
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp)
                    ) {}

                    FlipCard3DContainer(
                        isFlipped = spotlightFlipped,
                        onFlip = { spotlightFlipped = !spotlightFlipped },
                        enableSwipeToFlip = true,
                        modifier = Modifier.testTag("fc_spotlight_flip_card"),
                        frontContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
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
                                        StatusPill("精选速翻", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                                        StatusPill(spotlightCard.id.uppercase(), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Flip, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("正面 · 点击翻面", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = spotlightCard.front.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!spotlightCard.front.code.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = spotlightCard.front.code,
                                            fontFamily = JetBrainsMonoFont,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "↻ 先默想答案，再点击卡片翻面核对",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        backContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
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
                                    StatusPill("背面 · 核心解析", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary)
                                    Text("↺ 点击翻回正面", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = spotlightCard.back.answer,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (spotlightCard.back.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = spotlightCard.back.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val code = spotlightCard.back.code?.takeIf { it.isNotBlank() } ?: spotlightCard.back.sqliteCode
                                if (!code.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = code,
                                            fontFamily = JetBrainsMonoFont,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        copiedNotice?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Flippable Card Deck Browser
        item {
            SectionHeader(
                title = "闪卡翻阅库",
                subtitle = if (searchQuery.isBlank() && browserChapterFilter == "全部") {
                    "点击任意闪卡可原地 3D 翻面查看答案（展示前 40 张）"
                } else {
                    "匹配到 ${filteredCards.size} 张 · 点击卡片直接翻面"
                },
                trailing = {
                    if (flippedBrowserCards.values.any { it }) {
                        TextButton(onClick = { flippedBrowserCards.clear() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("全部正面")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索闪卡问题、答案、卡号或标签…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索闪卡") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fc_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = browserChapterFilter == "全部",
                    onClick = { browserChapterFilter = "全部" },
                    label = { Text("全部章") }
                )
                chapters.forEach { ch ->
                    FilterChip(
                        selected = browserChapterFilter == ch.id,
                        onClick = { browserChapterFilter = ch.id },
                        label = { Text("Ch${ch.number}") }
                    )
                }
            }
        }

        if (filteredCards.isEmpty()) {
            item {
                EmptyIllustrationCard(
                    title = "未找到匹配闪卡",
                    description = "换个关键词或章节试试，或点击右上角新建个人专属闪卡。"
                )
            }
        } else {
            items(filteredCards, key = { it.id }) { card ->
                val isCardFlipped = flippedBrowserCards[card.id] == true
                MiniFlipFlashcardItem(
                    card = card,
                    state = stateMap[card.id],
                    isFlipped = isCardFlipped,
                    onFlip = { flippedBrowserCards[card.id] = !isCardFlipped },
                    onCopyToCustom = {
                        scope.launch {
                            val copy = vm.duplicateBuiltinCard(card)
                            copiedNotice = "已将 ${card.id} 复制到自定义闪卡（${copy.id}）"
                        }
                    },
                    onStudySingleChapter = {
                        onStartSession(FlashcardReviewMode.CHAPTER.id, card.chapterId, countTowardsSchedule)
                    },
                    modifier = Modifier.testTag("fc_card_${card.id}")
                )
            }
        }
    }
}

@Composable
private fun DeckMetricTile(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
