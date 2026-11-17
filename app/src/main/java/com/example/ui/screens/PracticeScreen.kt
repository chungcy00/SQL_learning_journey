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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.LightMintGreen
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SuccessText
import com.example.ui.theme.WarmBeige
import com.example.ui.theme.WarningText
import com.example.ui.viewmodel.MainViewModel

@Composable
fun PracticeScreen(
    vm: MainViewModel,
    initialChapterId: String? = null,
    onOpenQuestion: (String) -> Unit,
    onOpenProjects: () -> Unit,
    onOpenWrongBook: () -> Unit
) {
    val allQuestions = vm.questions()
    val chapters = vm.chapters()
    val attempts by vm.questionAttempts.collectAsState()
    val wrongRecords by vm.wrongRecords.collectAsState()

    val passedIds = remember(attempts) {
        attempts.filter { it.isCorrect }.map { it.questionId }.toSet()
    }
    val attemptedWrongIds = remember(wrongRecords) {
        wrongRecords.filter { !it.mastered }.map { it.questionId }.toSet()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("全部") }
    var selectedDifficulty by remember { mutableStateOf("全部") }
    var selectedStatus by remember { mutableStateOf("全部") }
    var selectedChapter by remember(initialChapterId) { mutableStateOf(initialChapterId ?: "全部") }

    val filtered = remember(allQuestions, searchQuery, selectedType, selectedDifficulty, selectedStatus, selectedChapter, passedIds, attemptedWrongIds) {
        allQuestions.filter { q ->
            (selectedChapter == "全部" || q.chapterId == selectedChapter) &&
                (selectedType == "全部" || q.type == selectedType) &&
                (selectedDifficulty == "全部" || q.difficulty == selectedDifficulty) &&
                (selectedStatus == "全部" ||
                    (selectedStatus == "未通过" && q.id !in passedIds) ||
                    (selectedStatus == "已通过" && q.id in passedIds) ||
                    (selectedStatus == "待攻克" && q.id in attemptedWrongIds)) &&
                (searchQuery.isBlank() ||
                    q.title.contains(searchQuery, true) ||
                    q.id.contains(searchQuery, true) ||
                    q.context.contains(searchQuery, true) ||
                    q.requirements.contains(searchQuery, true))
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
                Column {
                    Text(
                        text = "题库实战 (${passedIds.size}/300)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "6 大题型 · 5 档难度 · 隔离判题与错题追踪",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onOpenProjects,
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryAction,
                        modifier = Modifier.testTag("practice_projects_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BusinessCenter, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("业务项目", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索题号（如 q0001）、关键词或业务场景…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜题") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("practice_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Type chips
            val types = listOf("全部", "选择", "代码填空", "独立编写", "代码纠错", "结果预测", "业务情景")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                types.forEach { tp ->
                    FilterChip(
                        selected = selectedType == tp,
                        onClick = { selectedType = tp },
                        label = { Text(tp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryAction,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("practice_type_$tp")
                    )
                }
            }

            // Difficulty & status chips
            val diffs = listOf("全部", "入门", "简单", "中等", "困难", "挑战")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("全部", "未通过", "已通过", "待攻克").forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = st },
                        label = { Text(st) },
                        modifier = Modifier.testTag("practice_status_$st")
                    )
                }
                diffs.drop(1).forEach { df ->
                    FilterChip(
                        selected = selectedDifficulty == df,
                        onClick = { selectedDifficulty = if (selectedDifficulty == df) "全部" else df },
                        label = { Text(df) },
                        modifier = Modifier.testTag("practice_diff_$df")
                    )
                }
            }

            // Chapter quick selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedChapter == "全部",
                    onClick = { selectedChapter = "全部" },
                    label = { Text("全部章") }
                )
                chapters.forEach { ch ->
                    FilterChip(
                        selected = selectedChapter == ch.id,
                        onClick = { selectedChapter = ch.id },
                        label = { Text("Ch${ch.number}") }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyIllustrationCard(
                    title = "没有符合筛选条件的题目",
                    description = "可重置筛选条件查看全部 300 道题目，或前往错题本专项复盘。",
                    actionLabel = "重置筛选",
                    onAction = {
                        searchQuery = ""
                        selectedType = "全部"
                        selectedDifficulty = "全部"
                        selectedStatus = "全部"
                        selectedChapter = "全部"
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "匹配 ${filtered.size} 题",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (attemptedWrongIds.isNotEmpty()) {
                            Surface(
                                onClick = onOpenWrongBook,
                                shape = RoundedCornerShape(8.dp),
                                color = ErrorLightSurface
                            ) {
                                Text(
                                    text = "错题本 (${attemptedWrongIds.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ErrorText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                items(filtered, key = { it.id }) { q ->
                    val passed = q.id in passedIds
                    val isWrong = q.id in attemptedWrongIds
                    SeaSaltCard(
                        onClick = { onOpenQuestion(q.id) },
                        modifier = Modifier.testTag("practice_q_${q.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    passed -> Icons.Default.CheckCircle
                                    isWrong -> Icons.Default.ErrorOutline
                                    else -> Icons.Outlined.RadioButtonUnchecked
                                },
                                contentDescription = when {
                                    passed -> "已通过"
                                    isWrong -> "待攻克"
                                    else -> "未作答"
                                },
                                tint = when {
                                    passed -> SuccessText
                                    isWrong -> ErrorText
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = q.id.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrimaryAction,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusPill(
                                        text = q.type,
                                        backgroundColor = CloudSeaBlue,
                                        textColor = PrimaryAction
                                    )
                                    DifficultyBadge(q.difficulty)
                                    if (!q.taskId.isNullOrBlank()) {
                                        StatusPill(
                                            text = q.taskId,
                                            backgroundColor = WarmBeige,
                                            textColor = WarningText
                                        )
                                    }
                                    if (q.requiredAstFeatures.isNotEmpty()) {
                                        StatusPill(
                                            text = "专项结构",
                                            backgroundColor = LightMintGreen,
                                            textColor = SuccessText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = q.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (q.requirements.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = q.requirements,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "开始做题",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
