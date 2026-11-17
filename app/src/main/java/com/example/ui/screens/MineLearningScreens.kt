package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.EmptyIllustrationCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongBookScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenQuestion: (String) -> Unit
) {
    val wrongRecords by vm.wrongRecords.collectAsState()
    var filterTab by remember { mutableStateOf("待攻克") }

    val activeList = remember(wrongRecords) { wrongRecords.filter { !it.mastered } }
    val masteredList = remember(wrongRecords) { wrongRecords.filter { it.mastered } }
    val displayList = if (filterTab == "待攻克") activeList else masteredList

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能错题本") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wrongbook_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterTab == "待攻克",
                    onClick = { filterTab = "待攻克" },
                    label = { Text("待攻克 (${activeList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryAction,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("wrongbook_tab_active")
                )
                FilterChip(
                    selected = filterTab == "已掌握",
                    onClick = { filterTab = "已掌握" },
                    label = { Text("已掌握 (${masteredList.size})") },
                    modifier = Modifier.testTag("wrongbook_tab_mastered")
                )
            }

            if (displayList.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EmptyIllustrationCard(
                        title = if (filterTab == "待攻克") "当前暂无待攻克错题" else "暂无已掌握错题记录",
                        description = "做题过程中答错的题目会自动进入此处；连续 2 次独立做对后将移入「已掌握」，历史错误次数始终保留。"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayList, key = { it.questionId }) { rec ->
                        val q = vm.contentRepo.getQuestion(rec.questionId)
                        SeaSaltCard(
                            onClick = { onOpenQuestion(rec.questionId) },
                            modifier = Modifier.testTag("wrong_item_${rec.questionId}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPill(rec.questionId.uppercase(), CloudSeaBlue, PrimaryAction)
                                    q?.let { DifficultyBadge(it.difficulty) }
                                    StatusPill(
                                        text = if (rec.mastered) "已掌握" else "连对 ${rec.consecutiveCorrect}/2",
                                        backgroundColor = if (rec.mastered) LightMintGreen else WarmBeige,
                                        textColor = if (rec.mastered) SuccessText else WarningText
                                    )
                                }
                                Text(
                                    text = "累计错 ${rec.errorCount} 次",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ErrorText
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = q?.title ?: "题目 ${rec.questionId}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (rec.lastErrorMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "最近诊断：${rec.lastErrorMessage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenKnowledgePoint: (String) -> Unit,
    onOpenQuestion: (String) -> Unit,
    onOpenLabWithSql: (String) -> Unit
) {
    val favorites by vm.favorites.collectAsState()
    val notes by vm.notes.collectAsState()
    var filterType by remember { mutableStateOf("全部") }

    val filteredFavs = remember(favorites, filterType) {
        if (filterType == "全部") favorites
        else favorites.filter { it.category == filterType }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏夹与笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("favorites_back_btn")) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("全部", "知识点", "练习题", "闪卡", "SQL 片段").forEach { cat ->
                        FilterChip(
                            selected = filterType == cat,
                            onClick = { filterType = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            if (filteredFavs.isEmpty() && notes.isEmpty()) {
                item {
                    EmptyIllustrationCard(
                        title = "暂无收藏与笔记",
                        description = "在知识点、练习题、闪卡或代码实验室点击收藏与保存笔记后，会统一呈现在此处。"
                    )
                }
            } else {
                items(filteredFavs, key = { it.id }) { fav ->
                    SeaSaltCard(
                        onClick = {
                            when (fav.targetType) {
                                "kp" -> onOpenKnowledgePoint(fav.targetId)
                                "question" -> onOpenQuestion(fav.targetId)
                                "snippet" -> if (fav.payload.isNotBlank()) onOpenLabWithSql(fav.payload)
                            }
                        },
                        modifier = Modifier.testTag("fav_item_${fav.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(fav.category, CloudSeaBlue, PrimaryAction)
                            IconButton(
                                onClick = {
                                    vm.toggleFavorite(fav.targetType, fav.targetId, fav.title, fav.subtitle, fav.payload)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "移除收藏", tint = ErrorText, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(fav.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (fav.payload.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fav.payload,
                                fontFamily = JetBrainsMonoFont,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (notes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("学习笔记 (${notes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(notes, key = { it.id }) { n ->
                        SeaSaltCard(
                            onClick = {
                                if (n.targetType == "kp") onOpenKnowledgePoint(n.targetId)
                            }
                        ) {
                            StatusPill("${n.targetType.uppercase()} · ${n.targetId}", WarmBeige, WarningText)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(n.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val stats by vm.statsOverview.collectAsState()
    val completedKps by vm.completedKpIds.collectAsState()
    val chapters = vm.chapters()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("真实学习数据统计") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stats_back_btn")) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryTile(
                        title = "累计学习时长",
                        value = "${stats.totalStudySeconds / 60} 分钟",
                        sub = "连续学习 ${stats.streakDays} 天 · 活跃 ${stats.studyDays} 天",
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryTile(
                        title = "课程知识点",
                        value = "${stats.completedKps} / ${stats.totalKps}",
                        sub = "完成率 ${(stats.completedKps * 100 / stats.totalKps.coerceAtLeast(1))}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryTile(
                        title = "首次通过率",
                        value = if (stats.firstAttemptAccuracy == null) "暂无数据"
                        else "${(stats.firstAttemptAccuracy!! * 100).toInt()}%",
                        sub = if (stats.firstAttemptTotalCount == 0) "尚未提交任何题目"
                        else "${stats.firstAttemptPassedCount}/${stats.firstAttemptTotalCount} 首题即过",
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryTile(
                        title = "独立完成率",
                        value = if (stats.independentCompletionRate == null) "暂无数据"
                        else "${(stats.independentCompletionRate!! * 100).toInt()}%",
                        sub = if (stats.completedQuestions == 0) "分母：已通过题数 0"
                        else "${stats.independentPassedCount}/${stats.completedQuestions} 无提示无看答案",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                SeaSaltCard {
                    Text("近 7 日学习时长（分钟）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    val maxMins = (stats.dailyMinutesSeries.maxOfOrNull { it.second } ?: 0).coerceAtLeast(10)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.dailyMinutesSeries.forEach { (day, mins) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${mins}m", style = MaterialTheme.typography.labelSmall, color = PrimaryAction)
                                Spacer(modifier = Modifier.height(4.dp))
                                val barFraction = (mins.toFloat() / maxMins).coerceIn(0.08f, 1f)
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height((60 * barFraction).dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (mins > 0) PrimaryAction else CloudSeaBlue)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                SeaSaltCard {
                    Text("FSRS 记忆闪卡概况", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "内置卡已学 ${stats.builtinCardsReviewedUnique}/${stats.totalBuiltinCards} · 已稳定掌握 ${stats.builtinCardsMastered} 张",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "自定义闪卡 ${stats.customCardsCount} 张（稳定掌握 ${stats.customCardsMastered} 张）· 总有效复习事件 ${stats.totalReviewEvents} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text("20 章课程完成明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(chapters, key = { it.id }) { ch ->
                val done = ch.knowledgePoints.count { it.id in completedKps }
                val total = ch.knowledgePoints.size.coerceAtLeast(1)
                SeaSaltCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("第${ch.number}章 · ${ch.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("$done / $total", style = MaterialTheme.typography.labelMedium, color = PrimaryAction)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = PrimaryAction,
                        trackColor = CloudSeaBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun StatSummaryTile(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    SeaSaltCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryAction)
        Spacer(modifier = Modifier.height(4.dp))
        Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
