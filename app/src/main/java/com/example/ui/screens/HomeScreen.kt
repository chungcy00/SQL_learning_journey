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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.FunctionIconTile
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StagePill
import com.example.ui.components.StatusPill
import com.example.ui.components.businessProjectIllustration
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
fun HomeScreen(
    vm: MainViewModel,
    onOpenKnowledgePoint: (String) -> Unit,
    onOpenLearnWithQuery: (String?, String) -> Unit,
    onOpenLab: (String?) -> Unit,
    onOpenProjects: () -> Unit,
    onOpenProjectDetail: (String) -> Unit,
    onOpenWrongBook: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenOfflineStatus: () -> Unit,
    onOpenFlashcardDue: () -> Unit
) {
    val stats by vm.statsOverview.collectAsState()
    val settings by vm.settingsFlow.collectAsState()
    val completedKps by vm.completedKpIds.collectAsState()
    val flashcardStates by vm.flashcardStates.collectAsState()
    val wrongRecords by vm.wrongRecords.collectAsState()
    val offlineReport by vm.offlineReport.collectAsState()
    val chapters = vm.chapters()
    val projects = vm.businessProjects()
    var searchQuery by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val dueCardsCount = remember(flashcardStates, vm.flashcards()) {
        val stateMap = flashcardStates.associateBy { it.cardId }
        val due = stateMap.values.count { it.dueAtMs <= now }
        val newAvail = (vm.flashcards().size - stateMap.size).coerceAtLeast(0).coerceAtMost(settings.dailyNewCardLimit)
        due + newAvail
    }
    val activeWrongCount = remember(wrongRecords) { wrongRecords.count { !it.mastered } }
    val continueKp = vm.contentRepo.getKnowledgePoint(settings.lastKpId) ?: vm.knowledgePoints().firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand & Offline Status Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.brand_logo_d),
                        contentDescription = "SQL 学习标志",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SQL 学习旅程",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MySQL 8.4 教学 · 离线本地执行",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onOpenOfflineStatus,
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        offlineReport == null -> WarmBeige
                        offlineReport!!.isReady -> LightMintGreen
                        else -> ErrorLightSurface
                    },
                    modifier = Modifier.testTag("home_offline_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (offlineReport?.isReady == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "离线校验状态",
                            tint = when {
                                offlineReport == null -> WarningText
                                offlineReport!!.isReady -> SuccessText
                                else -> ErrorText
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                offlineReport == null -> "自检中"
                                offlineReport!!.isReady -> "离线就绪"
                                else -> "需修复"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                offlineReport == null -> WarningText
                                offlineReport!!.isReady -> SuccessText
                                else -> ErrorText
                            }
                        )
                    }
                }
            }
        }

        // Quick Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索 152 个知识点、关键字或语法…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        Surface(
                            onClick = { onOpenLearnWithQuery(null, searchQuery.trim()) },
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryAction
                        ) {
                            Text(
                                text = "查找",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input")
            )
        }

        // Continue Learning Hero Card
        item {
            val allDone = stats.completedKps >= stats.totalKps && stats.totalKps > 0
            SeaSaltCard(
                containerColor = CloudSeaBlue,
                borderColor = MaterialTheme.colorScheme.outlineVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill(
                            text = "连续学习 ${stats.streakDays} 天 · 累计 ${stats.totalStudySeconds / 60} 分钟",
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            textColor = PrimaryAction
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = continueKp?.title ?: "开启 SQL 第一课",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = continueKp?.learningObjective?.ifBlank { continueKp.concept } ?: "20 章课程 · 300 道实操题 · 200 张闪卡",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (continueKp != null) onOpenKnowledgePoint(continueKp.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                                modifier = Modifier.testTag("home_continue_learning_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("继续学习")
                            }
                            Surface(
                                onClick = onOpenFlashcardDue,
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.testTag("home_due_flashcards_btn")
                            ) {
                                Text(
                                    text = "待复习 $dueCardsCount 卡",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PrimaryAction,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Image(
                        painter = painterResource(
                            id = if (allDone) R.drawable.illus_state_learning_complete else R.drawable.illus_state_home_learning
                        ),
                        contentDescription = "学习主视觉",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Real Progress Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        label = "知识点",
                        value = "${stats.completedKps}/${stats.totalKps}",
                        progress = stats.completedKps.toFloat() / stats.totalKps.coerceAtLeast(1),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "题库通关",
                        value = "${stats.completedQuestions}/${stats.totalQuestions}",
                        progress = stats.completedQuestions.toFloat() / stats.totalQuestions.coerceAtLeast(1),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "闪卡已学",
                        value = "${stats.builtinCardsReviewedUnique}/${stats.totalBuiltinCards}",
                        progress = stats.builtinCardsReviewedUnique.toFloat() / stats.totalBuiltinCards.coerceAtLeast(1),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Function Entry Grid
        item {
            SectionHeader(title = "快捷工作台", subtitle = "随时写 SQL、刷业务项目、复盘错题与收藏")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FunctionIconTile(
                    iconRes = R.drawable.fn_code_lab,
                    label = "代码实验室",
                    badge = "7业务库+个人",
                    onClick = { onOpenLab(null) },
                    testTag = "home_entry_lab",
                    modifier = Modifier.weight(1f)
                )
                FunctionIconTile(
                    iconRes = R.drawable.fn_project,
                    label = "业务项目",
                    badge = "6大行业30题",
                    onClick = onOpenProjects,
                    testTag = "home_entry_projects",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FunctionIconTile(
                    iconRes = R.drawable.fn_wrongbook,
                    label = "错题本",
                    badge = if (activeWrongCount > 0) "$activeWrongCount 待攻克" else "暂无待攻克",
                    onClick = onOpenWrongBook,
                    testTag = "home_entry_wrongbook",
                    modifier = Modifier.weight(1f)
                )
                FunctionIconTile(
                    iconRes = R.drawable.fn_favorites,
                    label = "收藏与片段",
                    badge = "本地永久保留",
                    onClick = onOpenFavorites,
                    testTag = "home_entry_favorites",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4 Learning Stages Overview
        item {
            SectionHeader(
                title = "四阶段学习路线",
                subtitle = "20 章体系化进阶，支持自由跳转",
                trailing = {
                    Surface(
                        onClick = { onOpenLearnWithQuery(null, "") },
                        shape = RoundedCornerShape(10.dp),
                        color = CloudSeaBlue
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("全部章节", style = MaterialTheme.typography.labelMedium, color = PrimaryAction)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = PrimaryAction, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            val stages = listOf("入门", "核心", "进阶", "实战")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stages.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { st ->
                            val stageChs = chapters.filter { it.stage == st }
                            val totalStageKps = stageChs.sumOf { it.knowledgePoints.size }
                            val doneStageKps = stageChs.sumOf { ch -> ch.knowledgePoints.count { it.id in completedKps } }
                            SeaSaltCard(
                                onClick = { onOpenLearnWithQuery(st, "") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_stage_$st")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StagePill(st)
                                    Text(
                                        text = "${stageChs.size} 章",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$doneStageKps / $totalStageKps 知识点",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalStageKps > 0) doneStageKps.toFloat() / totalStageKps else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = PrimaryAction,
                                    trackColor = CloudSeaBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        // Featured 6 Business Projects
        item {
            SectionHeader(
                title = "企业业务实战项目",
                subtitle = "真实企业 SQLite 数据库，50 项商业指标口径"
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(projects, key = { it.projectId }) { proj ->
                    Surface(
                        onClick = { onOpenProjectDetail(proj.projectId) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .width(230.dp)
                            .testTag("home_project_${proj.projectId}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Image(
                                painter = painterResource(id = businessProjectIllustration(proj.projectId)),
                                contentDescription = proj.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = proj.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = proj.subtitle,
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

@Composable
private fun MetricBox(
    label: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PrimaryAction,
                trackColor = CloudSeaBlue
            )
        }
    }
}
