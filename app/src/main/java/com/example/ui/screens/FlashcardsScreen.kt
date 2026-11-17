package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FlashcardReviewMode
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.LightMintGreen
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SuccessText
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
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val now = System.currentTimeMillis()
    val stateMap = remember(fcStates) { fcStates.associateBy { it.cardId } }
    val dueCount = remember(stateMap, now) { stateMap.values.count { it.dueAtMs <= now } }
    val newCount = remember(stateMap, builtinCards, customCards) {
        (builtinCards.size + customCards.size - stateMap.size).coerceAtLeast(0)
    }

    val filteredCards = remember(builtinCards, searchQuery) {
        if (searchQuery.isBlank()) builtinCards.take(40)
        else builtinCards.filter {
            it.front.question.contains(searchQuery, true) ||
                it.back.answer.contains(searchQuery, true) ||
                it.id.contains(searchQuery, true) ||
                it.tags.any { t -> t.contains(searchQuery, true) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("记忆闪卡 · FSRS-4.5", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("200 张内置专业卡 + 自定义卡 · 间隔记忆算法", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = { onOpenCustomManager(null) },
                    modifier = Modifier.testTag("fc_custom_manage_btn")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("自定义卡 (${customCards.size})")
                }
            }
        }

        // Review Center Hero
        item {
            SeaSaltCard(containerColor = CloudSeaBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("今日记忆中心", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "到期 $dueCount 张 · 未学新卡 $newCount 张\n已累计学习 ${stats.builtinCardsReviewedUnique}/200 张 · 稳定掌握 ${stats.builtinCardsMastered} 张",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "目标记忆保持率：${(settings.desiredRetention * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryAction
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Image(
                        painter = painterResource(id = R.drawable.illus_state_flashcard_review),
                        contentDescription = "闪卡复习",
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

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
                                selectedContainerColor = PrimaryAction,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("fc_mode_${mode.id}")
                        )
                    }
                }

                if (selectedMode == FlashcardReviewMode.CHAPTER) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (countTowardsSchedule) "正式复习（更新 FSRS 调度）" else "自由浏览（不改变记忆间隔）",
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                    modifier = Modifier.fillMaxWidth().testTag("fc_start_session_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("开始「${selectedMode.label}」复习")
                }
            }
        }

        copiedNotice?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LightMintGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = SuccessText, modifier = Modifier.padding(10.dp))
                }
            }
        }

        // Card Browser
        item {
            SectionHeader(
                title = "闪卡浏览器",
                subtitle = if (searchQuery.isBlank()) "展示前 40 张内置卡，支持搜索与复制为自定义卡" else "匹配到 ${filteredCards.size} 张"
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索闪卡问题、答案、卡号或标签…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索闪卡") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("fc_search_input")
            )
        }

        if (filteredCards.isEmpty()) {
            item {
                EmptyIllustrationCard(
                    title = "未找到匹配闪卡",
                    description = "换个关键词试试，或点击右上角新建个人专属闪卡。"
                )
            }
        } else {
            items(filteredCards, key = { it.id }) { card ->
                val st = stateMap[card.id]
                SeaSaltCard(modifier = Modifier.testTag("fc_card_${card.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(card.id.uppercase(), CloudSeaBlue, PrimaryAction)
                            StatusPill(card.type, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onSurfaceVariant)
                            CompatibilityBadge(card.supportStatus)
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val copy = vm.duplicateBuiltinCard(card)
                                    copiedNotice = "已将 ${card.id} 复制到自定义闪卡（${copy.id}）"
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制为自定义卡", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = card.front.question,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "答：${card.back.answer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (st != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "已复习 ${st.reps} 次 · 稳定度 ${"%.1f".format(st.stability)} 天",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessText
                        )
                    }
                }
            }
        }
    }
}
