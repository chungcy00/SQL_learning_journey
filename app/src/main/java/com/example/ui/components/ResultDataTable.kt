package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.PrimaryAction

@Composable
fun ResultDataTable(
    title: String,
    columns: List<String>,
    rows: List<List<String?>>,
    maxDisplayRows: Int = 50,
    totalRowsHint: Int? = null
) {
    val visibleRows = rows.take(maxDisplayRows)
    val effectiveTotal = totalRowsHint ?: rows.size

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryAction
                )
                Text(
                    text = if (effectiveTotal > visibleRows.size) "展示 ${visibleRows.size}/${effectiveTotal} 行" else "${rows.size} 行",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (columns.isEmpty()) {
                Text(
                    text = "无返回列",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .background(CloudSeaBlue, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        columns.forEach { col ->
                            Text(
                                text = col,
                                fontFamily = JetBrainsMonoFont,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAction,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(108.dp).padding(end = 6.dp)
                            )
                        }
                    }

                    if (visibleRows.isEmpty()) {
                        Text(
                            text = "（0 行匹配结果）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    } else {
                        visibleRows.forEachIndexed { idx, row ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (idx % 2 == 1) MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(vertical = 7.dp, horizontal = 8.dp)
                            ) {
                                columns.indices.forEach { cIdx ->
                                    val cell = row.getOrNull(cIdx)
                                    Text(
                                        text = cell ?: "NULL",
                                        fontFamily = JetBrainsMonoFont,
                                        fontStyle = if (cell == null) FontStyle.Italic else FontStyle.Normal,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (cell == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(108.dp).padding(end = 6.dp)
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
