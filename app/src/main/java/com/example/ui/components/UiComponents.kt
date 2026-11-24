package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
fun MessMateLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.app_name)
) {
    Image(
        painter = painterResource(id = R.drawable.ic_messmate_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

@Composable
fun Modifier.warmMeshBackground(): Modifier {
    val bg = MaterialTheme.colorScheme.background
    val extra = LocalMessMateColors.current
    val glowAlphaTop = if (extra.isDark) 0.14f else 0.12f
    val glowAlphaSide = if (extra.isDark) 0.10f else 0.14f
    val topGlowColor = extra.flameStart.copy(alpha = glowAlphaTop)
    val sideGlowColor = extra.flameMid.copy(alpha = glowAlphaSide)

    // drawWithCache caches radial gradient shaders across draw frames for zero per-frame allocations
    return this
        .background(bg)
        .drawWithCache {
            val topCenter = Offset(size.width * 0.12f, size.height * 0.08f)
            val topRadius = (size.width * 0.72f).coerceAtLeast(1f)
            val topBrush = Brush.radialGradient(
                colors = listOf(topGlowColor, Color.Transparent),
                center = topCenter,
                radius = topRadius
            )

            val sideCenter = Offset(size.width * 0.92f, size.height * 0.05f)
            val sideRadius = (size.width * 0.65f).coerceAtLeast(1f)
            val sideBrush = Brush.radialGradient(
                colors = listOf(sideGlowColor, Color.Transparent),
                center = sideCenter,
                radius = sideRadius
            )

            onDrawBehind {
                drawCircle(brush = topBrush, radius = topRadius, center = topCenter)
                drawCircle(brush = sideBrush, radius = sideRadius, center = sideCenter)
            }
        }
}

@Composable
fun flameGradientBrush(): Brush {
    val extra = LocalMessMateColors.current
    return remember(extra) {
        Brush.linearGradient(
            colors = listOf(extra.flameStart, extra.flameMid, extra.flameEnd)
        )
    }
}

@Composable
fun flamePillBrush(): Brush {
    val extra = LocalMessMateColors.current
    return remember(extra) {
        Brush.horizontalGradient(
            colors = listOf(extra.flameStart, extra.flameMid)
        )
    }
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
    animateSize: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val extra = LocalMessMateColors.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (extra.isDark) 0.dp else shadowElevation,
                shape = shape,
                ambientColor = extra.flameStart.copy(alpha = 0.08f),
                spotColor = extra.flameStart.copy(alpha = 0.10f)
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .then(if (animateSize) Modifier.smoothContentSize() else Modifier)
                .padding(contentPadding),
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
    val shape = remember { RoundedCornerShape(26.dp) }
    val dotColor = extra.flameStart.copy(alpha = if (extra.isDark) 0.16f else 0.14f)
    val heroBrush = remember(extra) {
        Brush.linearGradient(
            colors = listOf(extra.heroBgTop, extra.surfaceLow, extra.heroBgBottom)
        )
    }

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
                .background(heroBrush)
                .drawWithCache {
                    val step = 11.dp.toPx()
                    val dotRadius = 1.6.dp.toPx()
                    val startX = size.width * 0.56f
                    val maxX = size.width - 8.dp.toPx()
                    val endY = size.height * 0.62f
                    val points = buildList {
                        var y = step
                        while (y < endY) {
                            var x = startX
                            while (x < maxX) {
                                add(Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                    }
                    onDrawBehind {
                        for (pt in points) {
                            drawCircle(color = dotColor, radius = dotRadius, center = pt)
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .smoothContentSize()
                    .padding(20.dp),
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
    val shape = remember { RoundedCornerShape(16.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val disabledBrush = remember(extra) {
        Brush.horizontalGradient(
            listOf(extra.neutralGrey.copy(alpha = 0.45f), extra.neutralGrey.copy(alpha = 0.45f))
        )
    }
    val effectiveBrush = if (enabled) gradient else disabledBrush

    Box(
        modifier = modifier
            .pressScaleFeedback(interactionSource = interactionSource, enabled = enabled)
            .heightIn(min = 50.dp)
            .shadow(
                elevation = if (enabled && !extra.isDark) 6.dp else 0.dp,
                shape = shape,
                ambientColor = extra.flameStart.copy(alpha = 0.30f),
                spotColor = extra.flameStart.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(effectiveBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                enabled = enabled,
                onClick = onClick
            )
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
    val interactionSource = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, extra.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
            .pressScaleFeedback(interactionSource = interactionSource, enabled = enabled)
            .heightIn(min = 48.dp)
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
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, shape)
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
                val backInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onNavigateBack,
                    interactionSource = backInteraction,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, extra.divider),
                    shadowElevation = if (extra.isDark) 0.dp else 1.dp,
                    modifier = Modifier
                        .pressScaleFeedback(backInteraction)
                        .size(44.dp)
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
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extra.divider),
        shadowElevation = if (extra.isDark) 0.dp else 1.dp,
        modifier = modifier
            .pressScaleFeedback(interactionSource)
            .heightIn(min = 34.dp)
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
    val reducedMotion = LocalReducedMotion.current
    val animatedBg by animateColorAsState(
        targetValue = containerColor,
        animationSpec = MotionSpec.standardTween(reducedMotion),
        label = "status_pill_bg"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = color,
        animationSpec = MotionSpec.standardTween(reducedMotion),
        label = "status_pill_fg"
    )

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(animatedBg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedTextColor,
                modifier = Modifier.size(13.dp)
            )
        }
        if (reducedMotion) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = animatedTextColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            AnimatedContent(
                targetState = text,
                transitionSpec = {
                    fadeIn(tween(150, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(tween(100, easing = FastOutSlowInEasing))
                },
                label = "status_pill_text"
            ) { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = animatedTextColor,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
    val reducedMotion = LocalReducedMotion.current
    val scrollState = rememberScrollState()
    val pillBrush = flamePillBrush()
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
                val shape = remember { RoundedCornerShape(14.dp) }
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = MotionSpec.fastTween(reducedMotion),
                    label = "segmented_tab_text"
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(shape)
                        .then(
                            if (selected) {
                                Modifier.background(pillBrush)
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
                        color = textColor,
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
    val reducedMotion = LocalReducedMotion.current
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = MotionSpec.emphasisTween(reducedMotion),
        label = "warm_progress_bar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(extra.surfaceElevated)
            .border(1.dp, extra.divider, CircleShape)
            .padding(2.dp)
    ) {
        if (animatedProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceAtLeast(0.05f))
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
    val reducedMotion = LocalReducedMotion.current
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = MotionSpec.emphasisTween(reducedMotion),
        label = "circular_flame_progress"
    )
    val trackColor = extra.surfaceElevated
    val sweepBrush = remember(extra) {
        Brush.sweepGradient(colors = listOf(extra.flameStart, extra.flameMid, extra.flameStart))
    }

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
            if (animatedProgress > 0.001f) {
                drawArc(
                    brush = sweepBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        AnimatedMetricText(
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
    val shape = remember { RoundedCornerShape(18.dp) }
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (extra.isDark) 0.dp else 2.dp,
                shape = shape,
                ambientColor = extra.flameStart.copy(alpha = 0.06f),
                spotColor = extra.flameStart.copy(alpha = 0.08f)
            ),
        shape = shape,
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
            AnimatedMetricText(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
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
