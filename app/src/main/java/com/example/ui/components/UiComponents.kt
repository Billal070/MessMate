package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MealStatus
import com.example.data.local.MealType
import com.example.data.local.RiceTransactionType
import com.example.ui.theme.LocalMessMateColors

@Composable
fun Modifier.warmMeshBackground(): Modifier {
    val bg = MaterialTheme.colorScheme.background
    val extra = LocalMessMateColors.current
    val glowAlphaTop = if (extra.isDark) 0.14f else 0.12f
    val glowAlphaSide = if (extra.isDark) 0.10f else 0.14f
    return this
        .background(bg)
        .drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(extra.flameStart.copy(alpha = glowAlphaTop), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.08f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.12f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(extra.flameMid.copy(alpha = glowAlphaSide), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.92f, size.height * 0.05f)
            )
        }
}

@Composable
fun flameGradientBrush(): Brush {
    val extra = LocalMessMateColors.current
    return Brush.linearGradient(
        colors = listOf(extra.flameStart, extra.flameMid, extra.flameEnd)
    )
}

@Composable
fun flamePillBrush(): Brush {
    val extra = LocalMessMateColors.current
    return Brush.horizontalGradient(
        colors = listOf(extra.flameStart, extra.flameMid)
    )
}

@Composable
fun warmTextFieldColors(): TextFieldColors {
    val extra = LocalMessMateColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = extra.borderStrong,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = extra.surfaceLow,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun MessCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = LocalMessMateColors.current.divider,
    cornerRadius: Dp = 22.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    shadowElevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val extra = LocalMessMateColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (extra.isDark) 0.dp else shadowElevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = extra.flameStart.copy(alpha = 0.08f),
                spotColor = extra.flameStart.copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun WarmHeroCard(
    modifier: Modifier = Modifier,
    borderColor: Color = LocalMessMateColors.current.divider,
    content: @Composable ColumnScope.() -> Unit
) {
    val extra = LocalMessMateColors.current
    val shape = RoundedCornerShape(26.dp)
    val dotColor = extra.flameStart.copy(alpha = if (extra.isDark) 0.16f else 0.14f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (extra.isDark) 0.dp else 4.dp,
                shape = shape,
                ambientColor = extra.flameStart.copy(alpha = 0.12f),
                spotColor = extra.flameStart.copy(alpha = 0.14f)
            ),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(extra.heroBgTop, extra.surfaceLow, extra.heroBgBottom)
                    )
                )
                .drawBehind {
                    val step = 11.dp.toPx()
                    val dotRadius = 1.6.dp.toPx()
                    val startX = size.width * 0.56f
                    val endY = size.height * 0.62f
                    var y = step
                    while (y < endY) {
                        var x = startX
                        while (x < size.width - 8.dp.toPx()) {
                            drawCircle(
                                color = dotColor,
                                radius = dotRadius,
                                center = Offset(x, y)
                            )
                            x += step
                        }
                        y += step
                    }
                }
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun WarmGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    gradient: Brush = flamePillBrush()
) {
    val extra = LocalMessMateColors.current
    val shape = RoundedCornerShape(16.dp)
    val effectiveBrush = if (enabled) {
        gradient
    } else {
        Brush.horizontalGradient(
            listOf(extra.neutralGrey.copy(alpha = 0.45f), extra.neutralGrey.copy(alpha = 0.45f))
        )
    }

    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .shadow(
                elevation = if (enabled && !extra.isDark) 6.dp else 0.dp,
                shape = shape,
                ambientColor = extra.flameStart.copy(alpha = 0.30f),
                spotColor = extra.flameStart.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(effectiveBrush)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WarmOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val extra = LocalMessMateColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WarmIconBadge(
    icon: ImageVector,
    tint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    cornerRadius: Dp = 14.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun WarmTopBar(
    title: String,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val extra = LocalMessMateColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onNavigateBack != null) {
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extra.divider),
                    shadowElevation = if (extra.isDark) 0.dp else 1.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun WarmActionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val extra = LocalMessMateColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.divider),
        shadowElevation = if (extra.isDark) 0.dp else 1.dp,
        modifier = modifier.heightIn(min = 34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = color.copy(alpha = 0.14f),
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WarmSegmentedTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    val extra = LocalMessMateColors.current
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = extra.surfaceElevated,
        border = BorderStroke(1.dp, extra.divider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedIndex == index
                val shape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(shape)
                        .then(
                            if (selected) {
                                Modifier.background(flamePillBrush())
                            } else {
                                Modifier.background(Color.Transparent)
                            }
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("${tagPrefix}_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun WarmProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    barBrush: Brush = flamePillBrush()
) {
    val extra = LocalMessMateColors.current
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(extra.surfaceElevated)
            .border(1.dp, extra.divider, CircleShape)
            .padding(2.dp)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped.coerceAtLeast(0.05f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barBrush)
            )
        }
    }
}

@Composable
fun CircularFlameProgressRing(
    progress: Float,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
    strokeWidth: Dp = 6.dp
) {
    val extra = LocalMessMateColors.current
    val clamped = progress.coerceIn(0f, 1f)
    val trackColor = extra.surfaceElevated
    val startColor = extra.flameStart
    val endColor = extra.flameMid

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (clamped > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(startColor, endColor, startColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = centerLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
fun WarmAlertBanner(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStatCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val extra = LocalMessMateColors.current
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (extra.isDark) 0.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = extra.flameStart.copy(alpha = 0.06f),
                spotColor = extra.flameStart.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.divider)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyStateBox(
    icon: ImageVector = Icons.Outlined.Info,
    message: String,
    modifier: Modifier = Modifier
) {
    val extra = LocalMessMateColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.divider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WarmIconBadge(
                icon = icon,
                tint = MaterialTheme.colorScheme.primary,
                containerColor = extra.primarySoft,
                borderColor = extra.primarySoftBorder,
                size = 48.dp,
                iconSize = 24.dp,
                cornerRadius = 16.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val extra = LocalMessMateColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extra.danger,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(stringResource(R.string.action_delete), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun localizedMealName(type: MealType): String = when (type) {
    MealType.BREAKFAST -> stringResource(R.string.meal_breakfast)
    MealType.LUNCH -> stringResource(R.string.meal_lunch)
    MealType.DINNER -> stringResource(R.string.meal_dinner)
}

@Composable
fun localizedMealTime(type: MealType): String = when (type) {
    MealType.BREAKFAST -> stringResource(R.string.meal_time_breakfast)
    MealType.LUNCH -> stringResource(R.string.meal_time_lunch)
    MealType.DINNER -> stringResource(R.string.meal_time_dinner)
}

@Composable
fun localizedMealStatus(status: MealStatus): String = when (status) {
    MealStatus.PENDING -> stringResource(R.string.meal_status_pending)
    MealStatus.COUNTED -> stringResource(R.string.meal_status_counted)
    MealStatus.SKIPPED -> stringResource(R.string.meal_status_skipped)
    MealStatus.OFF -> stringResource(R.string.meal_status_off)
}

@Composable
fun mealStatusColor(status: MealStatus): Color {
    val extra = LocalMessMateColors.current
    return when (status) {
        MealStatus.COUNTED -> extra.success
        MealStatus.PENDING -> extra.amber
        MealStatus.SKIPPED -> extra.warning
        MealStatus.OFF -> extra.danger
    }
}

@Composable
fun mealStatusContainerColor(status: MealStatus): Color {
    val extra = LocalMessMateColors.current
    return when (status) {
        MealStatus.COUNTED -> extra.successContainer
        MealStatus.PENDING -> extra.amberContainer
        MealStatus.SKIPPED -> extra.warningContainer
        MealStatus.OFF -> extra.dangerContainer
    }
}

@Composable
fun localizedRiceTxType(type: RiceTransactionType): String = when (type) {
    RiceTransactionType.RICE_ADDED -> stringResource(R.string.rice_tx_added)
    RiceTransactionType.BREAKFAST_DEDUCTION -> stringResource(R.string.rice_tx_breakfast)
    RiceTransactionType.LUNCH_DEDUCTION -> stringResource(R.string.rice_tx_lunch)
    RiceTransactionType.DINNER_DEDUCTION -> stringResource(R.string.rice_tx_dinner)
    RiceTransactionType.MANUAL_ADJUSTMENT -> stringResource(R.string.rice_tx_adjustment)
}

@Composable
fun localizedSemesterName(semesterKey: String): String = when (semesterKey) {
    "1st Semester" -> stringResource(R.string.semester_1)
    "2nd Semester" -> stringResource(R.string.semester_2)
    "3rd Semester" -> stringResource(R.string.semester_3)
    "4th Semester" -> stringResource(R.string.semester_4)
    "5th Semester" -> stringResource(R.string.semester_5)
    "6th Semester" -> stringResource(R.string.semester_6)
    "7th Semester" -> stringResource(R.string.semester_7)
    "8th Semester" -> stringResource(R.string.semester_8)
    else -> semesterKey
}
