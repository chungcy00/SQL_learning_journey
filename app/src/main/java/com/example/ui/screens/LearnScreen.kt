package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.StagePill
import com.example.ui.components.StatusPill
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.LightMintGreen
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SuccessText
import com.example.ui.theme.WarmBeige
import com.example.ui.theme.WarningText
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LearnScreen(
    vm: MainViewModel,
    initialStage: String? = null,
    initialQuery: String = "",
    onOpenKnowledgePoint: (String) -> Unit
) {
    val chapters = vm.chapters()
    val completedKps by vm.completedKpIds.collectAsState()
    val attempts by vm.questionAttempts.collectAsState()
    val fcStates by vm.flashcardStates.collectAsState()
    val wrongRecords by vm.wrongRecords.collectAsState()

    var selectedStage by remember(initialStage) { mutableStateOf(initialStage ?: "全部") }
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val expandedChapters = remember { mutableStateMapOf<String, Boolean>("ch01" to true) }

    val filteredChapters = remember(chapters, selectedStage, query) {
        chapters.filter { ch ->
            (selectedStage == "全部" || ch.stage == selectedStage) &&
                (query.isBlank() ||
                    ch.title.contains(query, ignoreCase = true) ||
                    ch.knowledgePoints.any { kp ->
                        kp.title.contains(query, ignoreCase = true) ||
                            kp.concept.contains(query, ignoreCase = true) ||
                            kp.syntax.contains(query, ignoreCase = true)
                    })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SQL 体系课程",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusPill(
                    text = "${completedKps.size}/152 已学",
                    backgroundColor = CloudSeaBlue,
                    textColor = PrimaryAction
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索知识点标题、关键字或语法…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索课程") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("learn_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            val stages = listOf("全部", "入门", "核心", "进阶", "实战")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stages.forEach { st ->
                    FilterChip(
                        selected = selectedStage == st,
                        onClick = { selectedStage = st },
                        label = { Text(st) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryAction,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("learn_stage_filter_$st")
                    )
                }
            }
        }

        if (filteredChapters.isEmpty()) {
            BoxEmptyWrapper(query) {
                query = ""
                selectedStage = "全部"
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredChapters, key = { it.id }) { ch ->
                    val kpsToShow = remember(ch, query) {
                        if (query.isBlank()) ch.knowledgePoints
                        else ch.knowledgePoints.filter {
                            it.title.contains(query, ignoreCase = true) ||
                                it.concept.contains(query, ignoreCase = true) ||
                                it.syntax.contains(query, ignoreCase = true) ||
                                ch.title.contains(query, ignoreCase = true)
                        }
                    }
                    val doneCount = ch.knowledgePoints.count { it.id in completedKps }
                    val isExpanded = if (query.isNotBlank()) true else (expandedChapters[ch.id] == true)

                    SeaSaltCard(
                        onClick = { expandedChapters[ch.id] = !isExpanded },
                        modifier = Modifier.testTag("learn_chapter_${ch.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StagePill(ch.stage)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "第${ch.number}章 · ${ch.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$doneCount/${ch.knowledgePoints.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PrimaryAction
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "收起章节" else "展开章节"
                                )
                            }
                        }

                        if (ch.objective.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ch.objective,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (ch.knowledgePoints.isNotEmpty()) doneCount.toFloat() / ch.knowledgePoints.size else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryAction,
                            trackColor = CloudSeaBlue
                        )

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                kpsToShow.forEach { kp ->
                                    val done = kp.id in completedKps
                                    val mastery = remember(kp.id, completedKps, attempts, fcStates, wrongRecords) {
                                        vm.computeMastery(kp)
                                    }
                                    Surface(
                                        onClick = { onOpenKnowledgePoint(kp.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (done) LightMintGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.background,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("learn_kp_${kp.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = if (done) "已完成" else "未完成",
                                                tint = if (done) SuccessText else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = kp.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CompatibilityBadge(kp.compatibility?.status ?: "直接兼容")
                                                    StatusPill(
                                                        text = if (mastery.score != null) "${mastery.label} ${mastery.score}%" else mastery.label,
                                                        backgroundColor = when (mastery.label) {
                                                            "已掌握" -> LightMintGreen
                                                            "基本掌握" -> CloudSeaBlue
                                                            "需加强", "重点巩固" -> WarmBeige
                                                            else -> MaterialTheme.colorScheme.surface
                                                        },
                                                        textColor = when (mastery.label) {
                                                            "已掌握" -> SuccessText
                                                            "基本掌握" -> PrimaryAction
                                                            "需加强", "重点巩固" -> WarningText
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
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
            }
        }
    }
}

@Composable
private fun BoxEmptyWrapper(query: String, onReset: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        EmptyIllustrationCard(
            title = "未找到匹配知识点",
            description = "没有包含 \"$query\" 的章节或知识点，请尝试调整筛选条件。",
            actionLabel = "重置搜索",
            onAction = onReset
        )
    }
}
