package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.BackupInspectSummary
import com.example.ui.components.SeaSaltCard
import com.example.ui.components.SectionHeader
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
import kotlinx.coroutines.launch

@Composable
fun MineScreen(
    vm: MainViewModel,
    onOpenWrongBook: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenDatasets: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOfflineStatus: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats by vm.statsOverview.collectAsState()
    val wrongRecords by vm.wrongRecords.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val personalDatasets by vm.personalDatasets.collectAsState()
    val offlineReport by vm.offlineReport.collectAsState()

    var pendingRestoreSummary by remember { mutableStateOf<BackupInspectSummary?>(null) }
    var backupBanner by remember { mutableStateOf<String?>(null) }
    var backupError by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val jsonStr = vm.generateBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                    }
                    backupBanner = "完整学习备份 JSON 已导出至指定路径"
                    backupError = false
                }.onFailure {
                    backupBanner = "备份导出失败：${it.message}"
                    backupError = true
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: error("无法读取所选备份文件")
                    val inspected = vm.inspectBackupJson(raw).getOrThrow()
                    pendingRestoreSummary = inspected
                }.onFailure {
                    backupBanner = "备份文件校验失败：${it.message}"
                    backupError = true
                }
            }
        }
    }

    val activeWrongCount = wrongRecords.count { !it.mastered }

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
                Text("我的学习中心", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                StatusPill(
                    text = "纯本地存储",
                    backgroundColor = CloudSeaBlue,
                    textColor = PrimaryAction
                )
            }
        }

        // Local Learning Summary Card
        item {
            SeaSaltCard(containerColor = CloudSeaBlue) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.brand_logo_d),
                        contentDescription = "应用品牌标志",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SQL 学习 · 离线个人空间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "已学 ${stats.completedKps}/152 知识点 · 通关 ${stats.completedQuestions}/300 题 · 累计 ${stats.totalStudySeconds / 60} 分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = "连续 ${stats.streakDays} 天",
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        textColor = PrimaryAction
                    )
                    StatusPill(
                        text = if (stats.firstAttemptAccuracy == null) "首次正确率：暂无数据"
                        else "首次正确率：${(stats.firstAttemptAccuracy!! * 100).toInt()}%",
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        textColor = SuccessText
                    )
                }
            }
        }

        backupBanner?.let { msg ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (backupError) ErrorLightSurface else LightMintGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (backupError) ErrorText else SuccessText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Core Personal Learning Tools
        item {
            SectionHeader(title = "学习资产与数据", subtitle = "所有记录、错题、笔记与数据库均保留在本地")
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MineMenuRow(
                    iconRes = R.drawable.fn_stats,
                    title = "学习数据统计",
                    subtitle = "知识点进度、首次正确率、独立完成率、7日学习曲线",
                    badge = "${stats.completedQuestions}/300",
                    onClick = onOpenStats,
                    testTag = "mine_entry_stats"
                )
                MineMenuRow(
                    iconRes = R.drawable.fn_wrongbook,
                    title = "智能错题本",
                    subtitle = "连续2次独立做对自动标记掌握，历史记录不清空",
                    badge = if (activeWrongCount > 0) "$activeWrongCount 待攻克" else "已清零",
                    onClick = onOpenWrongBook,
                    testTag = "mine_entry_wrongbook"
                )
                MineMenuRow(
                    iconRes = R.drawable.fn_favorites,
                    title = "收藏夹与笔记",
                    subtitle = "知识点、题目、闪卡与 SQL 实验室片段",
                    badge = "${favorites.size} 项收藏",
                    onClick = onOpenFavorites,
                    testTag = "mine_entry_favorites"
                )
                MineMenuRow(
                    iconRes = R.drawable.fn_dataset,
                    title = "个人数据集管理",
                    subtitle = "导入 CSV / JSON / SQLite / SQL 或复制内置企业库",
                    badge = "${personalDatasets.size} 个自建库",
                    onClick = onOpenDatasets,
                    testTag = "mine_entry_datasets"
                )
            }
        }

        // Offline Integrity & Backup/Restore
        item {
            SectionHeader(title = "离线自检与备份恢复", subtitle = "SHA-256 资产校验与 JSON 无损迁徙")
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MineMenuRow(
                    iconRes = R.drawable.fn_offline,
                    title = "离线包自检与存储状态",
                    subtitle = offlineReport?.let {
                        if (it.isReady) "已完成 ${it.totalFilesChecked} 个资源文件 SHA-256 校验"
                        else "发现 ${it.missingOrCorruptFiles.size} 项异常，点击查看详情"
                    } ?: "点击查看资源校验详情",
                    badge = if (offlineReport?.isReady == true) "就绪" else "自检",
                    onClick = onOpenOfflineStatus,
                    testTag = "mine_entry_offline"
                )

                SeaSaltCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.fn_backup),
                            contentDescription = "备份与恢复",
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("本地备份与恢复", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("支持合并保留现有数据或覆盖恢复（事务失败自动回滚）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportBackupLauncher.launch("sql_learning_backup.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                            modifier = Modifier.weight(1f).testTag("mine_backup_export_btn")
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出备份")
                        }
                        OutlinedButton(
                            onClick = { importBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f).testTag("mine_backup_import_btn")
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("恢复备份")
                        }
                    }
                }

                MineMenuRow(
                    iconRes = R.drawable.fn_settings,
                    title = "学习与编辑器偏好设置",
                    subtitle = "主题、字体字号、FSRS 保持率、键盘模式、进度重置",
                    badge = null,
                    onClick = onOpenSettings,
                    testTag = "mine_entry_settings"
                )
            }
        }
    }

    // Restore Mode Confirmation Dialog (Merge vs Overwrite)
    pendingRestoreSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { pendingRestoreSummary = null },
            title = { Text("确认恢复本地备份") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("已校验备份文件，共包含 ${summary.recordCount} 条学习与闪卡记录。")
                    Surface(shape = RoundedCornerShape(8.dp), color = WarmBeige) {
                        Text(
                            text = "• 合并恢复：保留当前已有数据，并合入备份中的记录。\n• 覆盖恢复：清空现有学习数据并完全替换为备份内容（异常时自动回滚）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningText,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = summary
                        pendingRestoreSummary = null
                        scope.launch {
                            vm.restoreBackup(target.payload, overwrite = false)
                                .onSuccess {
                                    backupBanner = "已合并恢复 ${target.recordCount} 条记录"
                                    backupError = false
                                }
                                .onFailure {
                                    backupBanner = "合并恢复失败（已自动回滚）：${it.message}"
                                    backupError = true
                                }
                        }
                    },
                    modifier = Modifier.testTag("backup_merge_confirm_btn")
                ) {
                    Text("合并恢复")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { pendingRestoreSummary = null }) { Text("取消") }
                    TextButton(
                        onClick = {
                            val target = summary
                            pendingRestoreSummary = null
                            scope.launch {
                                vm.restoreBackup(target.payload, overwrite = true)
                                    .onSuccess {
                                        backupBanner = "已覆盖恢复 ${target.recordCount} 条记录"
                                        backupError = false
                                    }
                                    .onFailure {
                                        backupBanner = "覆盖恢复失败（已自动回滚）：${it.message}"
                                        backupError = true
                                    }
                            }
                        },
                        modifier = Modifier.testTag("backup_overwrite_confirm_btn")
                    ) {
                        Text("覆盖恢复", color = ErrorText)
                    }
                }
            }
        )
    }
}

@Composable
private fun MineMenuRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    badge: String?,
    onClick: () -> Unit,
    testTag: String
) {
    SeaSaltCard(onClick = onClick, modifier = Modifier.testTag(testTag)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CloudSeaBlue,
                modifier = Modifier.size(44.dp)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!badge.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                StatusPill(badge, CloudSeaBlue, PrimaryAction)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
