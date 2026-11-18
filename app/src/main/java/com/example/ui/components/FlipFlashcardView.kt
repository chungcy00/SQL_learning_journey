package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.FlashcardStateEntity
import com.example.model.FlashcardItem
import com.example.ui.theme.JetBrainsMonoFont
import kotlin.math.abs
import kotlin.math.sin

/**
 * True 3D Y-axis flip card container with physical perspective lift and optional horizontal swipe-to-flip.
 */
@Composable
fun FlipCard3DContainer(
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    enableSwipeToFlip: Boolean = true,
    cornerRadius: Dp = 20.dp,
    frontBorderColor: Color = MaterialTheme.colorScheme.outline,
    backBorderColor: Color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
    frontContent: @Composable BoxScope.() -> Unit,
    backContent: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current.density
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "flashcard_flip_y"
    )
    val showingBack = rotation > 90f
    // Gentle scale dip mid-flip so the card feels physically suspended in 3D space
    val midFlipLift = 1f - 0.045f * sin(Math.toRadians(rotation.toDouble())).toFloat()
    var dragAccumulated by remember(isFlipped) { mutableFloatStateOf(0f) }

    val swipeModifier = if (enableSwipeToFlip) {
        Modifier.pointerInput(isFlipped) {
            detectHorizontalDragGestures(
                onDragStart = { dragAccumulated = 0f },
                onDragEnd = {
                    if (abs(dragAccumulated) > 56f) {
                        onFlip()
                    }
                    dragAccumulated = 0f
                },
                onDragCancel = { dragAccumulated = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    dragAccumulated += dragAmount
                }
            )
        }
    } else Modifier

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = if (showingBack) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (showingBack) 1.5.dp else 1.dp,
            color = if (showingBack) backBorderColor else frontBorderColor
        ),
        tonalElevation = if (showingBack) 3.dp else 2.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
                scaleX = midFlipLift
                scaleY = midFlipLift
            }
            .then(swipeModifier)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onFlip
            )
    ) {
        if (!showingBack) {
            Box(modifier = Modifier.fillMaxWidth()) {
                frontContent()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = 180f }
            ) {
                backContent()
            }
        }
    }
}

/**
 * Interactive 3D flippable flashcard item for the Flashcards tab browser.
 */
@Composable
fun MiniFlipFlashcardItem(
    card: FlashcardItem,
    state: FlashcardStateEntity?,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onCopyToCustom: () -> Unit,
    onStudySingleChapter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val surface = MaterialTheme.colorScheme.surface

    FlipCard3DContainer(
        isFlipped = isFlipped,
        onFlip = onFlip,
        enableSwipeToFlip = false,
        cornerRadius = 18.dp,
        modifier = modifier,
        frontContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(primaryContainer.copy(alpha = 0.45f), surface)
                        )
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(card.id.uppercase(), primaryContainer, MaterialTheme.colorScheme.primary)
                        StatusPill(card.type, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.onSurfaceVariant)
                        CompatibilityBadge(card.supportStatus)
                    }
                    Surface(
                        shape = CircleShape,
                        color = primaryContainer,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "翻转查看答案",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = card.front.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!card.front.code.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = card.front.code,
                            fontFamily = JetBrainsMonoFont,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state == null) "新卡 · 待初学" else "已复习 ${state.reps} 次 · S=${"%.1f".format(state.stability)}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "轻触翻面看答案",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        backContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(tertiaryContainer.copy(alpha = 0.65f), surface)
                        )
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("背面 · 答案", tertiaryContainer, MaterialTheme.colorScheme.tertiary)
                        Text(
                            text = card.id.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onStudySingleChapter,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "练习本章闪卡",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopyToCustom,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制为自定义卡",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = card.front.question,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(42.dp)
                            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = card.back.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (card.back.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.back.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val codeToShow = card.back.code?.takeIf { it.isNotBlank() } ?: card.back.sqliteCode
                if (!codeToShow.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = codeToShow,
                            fontFamily = JetBrainsMonoFont,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "↺ 再次点击翻回正面",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )
}
