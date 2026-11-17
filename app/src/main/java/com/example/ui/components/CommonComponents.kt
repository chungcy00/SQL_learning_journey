package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.ErrorLightSurface
import com.example.ui.theme.ErrorText
import com.example.ui.theme.LightMintGreen
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SuccessText
import com.example.ui.theme.WarmBeige
import com.example.ui.theme.WarningText

@Composable
fun SeaSaltCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StagePill(stage: String) {
    val (bg, fg) = when (stage) {
        "入门" -> LightMintGreen to SuccessText
        "核心" -> CloudSeaBlue to PrimaryAction
        "进阶" -> WarmBeige to WarningText
        "实战" -> ErrorLightSurface to ErrorText
        else -> CloudSeaBlue to PrimaryAction
    }
    StatusPill(text = stage, backgroundColor = bg, textColor = fg)
}

@Composable
fun DifficultyBadge(difficulty: String) {
    val (bg, fg) = when (difficulty) {
        "入门" -> LightMintGreen to SuccessText
        "简单" -> CloudSeaBlue to PrimaryAction
        "中等" -> WarmBeige to WarningText
        "困难", "挑战" -> ErrorLightSurface to ErrorText
        else -> CloudSeaBlue to PrimaryAction
    }
    StatusPill(text = difficulty, backgroundColor = bg, textColor = fg)
}

@Composable
fun CompatibilityBadge(status: String) {
    val (bg, fg) = when (status) {
        "直接兼容" -> LightMintGreen to SuccessText
        "转换执行" -> WarmBeige to WarningText
        "仅理论" -> CloudSeaBlue to PrimaryAction
        else -> ErrorLightSurface to ErrorText
    }
    StatusPill(text = status, backgroundColor = bg, textColor = fg)
}

@Composable
fun FunctionIconTile(
    @DrawableRes iconRes: Int,
    label: String,
    badge: String,
    onClick: () -> Unit,
    testTag: String = "",
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CloudSeaBlue,
                modifier = Modifier.size(42.dp)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.padding(7.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryAction
                )
            }
        }
    }
}

@Composable
fun EmptyIllustrationCard(
    @DrawableRes illustrationRes: Int = R.drawable.illus_state_empty,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    SeaSaltCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = illustrationRes),
                contentDescription = title,
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

fun businessProjectIllustration(projectId: String): Int {
    val key = projectId.lowercase()
    return when {
        key.contains("retail") || key.endsWith("01") -> R.drawable.illus_business_retail_sales
        key.contains("inventory") || key.endsWith("02") -> R.drawable.illus_business_inventory_management
        key.contains("ecommerce") || key.endsWith("03") -> R.drawable.illus_business_ecommerce_customer
        key.contains("finance") || key.endsWith("04") -> R.drawable.illus_business_financial_data
        key.contains("hr") || key.endsWith("05") -> R.drawable.illus_business_human_resources
        key.contains("procurement") || key.endsWith("06") -> R.drawable.illus_business_procurement_data
        else -> R.drawable.illus_business_retail_sales
    }
}
