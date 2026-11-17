package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.EmptyIllustrationCard
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.StatusPill
import com.example.ui.components.businessProjectIllustration
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.LightMintGreen
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SuccessText
import com.example.ui.theme.WarmBeige
import com.example.ui.theme.WarningText
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onOpenProjectDetail: (String) -> Unit,
    onOpenQuestion: (String) -> Unit
) {
    val projects = vm.businessProjects()
    val crossChallenges = remember(vm.questions()) {
        vm.questions().filter { it.chapterId == "ch20" }
    }
    val attempts by vm.questionAttempts.collectAsState()
    val passedIds = remember(attempts) { attempts.filter { it.isCorrect }.map { it.questionId }.toSet() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("企业业务项目实战") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("projects_back_btn")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "6 大行业专项项目（每项 5 个进阶任务）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(projects, key = { it.projectId }) { proj ->
                val doneCount = proj.taskQuestionIds.count { it in passedIds }
                SeaSaltCard(
                    onClick = { onOpenProjectDetail(proj.projectId) },
                    modifier = Modifier.testTag("project_card_${proj.projectId}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = businessProjectIllustration(proj.projectId)),
                            contentDescription = proj.title,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(proj.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                StatusPill("$doneCount/5", CloudSeaBlue, PrimaryAction)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(proj.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { doneCount / 5f },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = PrimaryAction,
                                trackColor = CloudSeaBlue
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "第20章 · 跨域企业综合挑战（${crossChallenges.count { it.id in passedIds }}/20）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(crossChallenges, key = { it.id }) { q ->
                val passed = q.id in passedIds
                SeaSaltCard(
                    onClick = { onOpenQuestion(q.id) },
                    modifier = Modifier.testTag("cross_challenge_${q.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (passed) SuccessText else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(q.taskId ?: q.id, WarmBeige, WarningText)
                                if (!q.crossGroup.isNullOrBlank()) {
                                    StatusPill(q.crossGroup, CloudSeaBlue, PrimaryAction)
                                }
                                DifficultyBadge(q.difficulty)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(q.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    vm: MainViewModel,
    projectId: String,
    onBack: () -> Unit,
    onOpenQuestion: (String) -> Unit,
    onOpenLab: (String) -> Unit
) {
    val proj = vm.businessProjects().firstOrNull { it.projectId == projectId }
    val attempts by vm.questionAttempts.collectAsState()
    val passedIds = remember(attempts) { attempts.filter { it.isCorrect }.map { it.questionId }.toSet() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(proj?.title ?: "项目详情") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("project_detail_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        if (proj == null) {
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                EmptyIllustrationCard(
                    title = "未找到业务项目",
                    description = "项目编号 $projectId 不存在。",
                    actionLabel = "返回",
                    onAction = onBack
                )
            }
            return@Scaffold
        }

        val schema = vm.schemas()[proj.datasetId]
        val tasks = proj.taskQuestionIds.mapNotNull { vm.contentRepo.getQuestion(it) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SeaSaltCard(containerColor = CloudSeaBlue) {
                    Image(
                        painter = painterResource(id = businessProjectIllustration(proj.projectId)),
                        contentDescription = proj.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(proj.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(proj.background, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    proj.goals.forEach { g ->
                        Text("• $g", style = MaterialTheme.typography.bodySmall, color = PrimaryAction)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onOpenLab(proj.datasetId) },
                        modifier = Modifier.fillMaxWidth().testTag("project_open_lab_btn")
                    ) {
                        Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("在代码实验室探索 ${proj.datasetId} 数据库")
                    }
                }
            }

            if (schema != null && schema.tables.isNotEmpty()) {
                item {
                    SeaSaltCard {
                        Text("业务数据表结构概览（${schema.tables.size} 张表）", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        schema.tables.entries.take(8).forEach { (tName, tInfo) ->
                            Text(
                                text = "• $tName (${tInfo.rowCount} 行)：${tInfo.columns.take(5).joinToString(", ") { it.name }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text("项目 5 项实战任务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            items(tasks, key = { it.id }) { q ->
                val passed = q.id in passedIds
                Surface(
                    onClick = { onOpenQuestion(q.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (passed) LightMintGreen.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().testTag("project_task_${q.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (passed) SuccessText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(q.taskId ?: q.id, WarmBeige, WarningText)
                                DifficultyBadge(q.difficulty)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(q.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(q.requirements, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
