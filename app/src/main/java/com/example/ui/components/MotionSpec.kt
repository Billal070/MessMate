package com.example.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Provides whether Android's system reduced-motion preference (ANIMATOR_DURATION_SCALE == 0)
 * is active. Read once per context to avoid repeated ContentResolver IPC on recomposition.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { isReducedMotionEnabled(context) }
}

fun isReducedMotionEnabled(context: Context): Boolean {
    return runCatching {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }.getOrDefault(false)
}

/**
 * Centralized, lightweight motion constants (150-240ms) adhering to Material 3 motion guidelines
 * and automatically collapsing to 0ms when reduced motion is enabled.
 */
object MotionSpec {
    const val DURATION_FAST = 150
    const val DURATION_STANDARD = 200
    const val DURATION_EMPHASIS = 230

    fun duration(baseMs: Int, reducedMotion: Boolean): Int =
        if (reducedMotion) 0 else baseMs

    fun <T> fastTween(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        tween(durationMillis = duration(DURATION_FAST, reducedMotion), easing = FastOutSlowInEasing)

    fun <T> standardTween(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        tween(durationMillis = duration(DURATION_STANDARD, reducedMotion), easing = FastOutSlowInEasing)

    fun <T> emphasisTween(reducedMotion: Boolean): FiniteAnimationSpec<T> =
        tween(durationMillis = duration(DURATION_EMPHASIS, reducedMotion), easing = FastOutSlowInEasing)
}

/**
 * Subtle tactile press scale feedback (0.97f on press) using graphicsLayer (draw-phase only,
 * zero layout invalidation). Disabled cleanly under reduced motion.
 */
fun Modifier.pressScaleFeedback(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed && !reducedMotion) pressedScale else 1f,
        animationSpec = MotionSpec.fastTween(reducedMotion),
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Lightweight card expand/collapse size animation respecting reduced motion.
 */
fun Modifier.smoothContentSize(): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) {
        this
    } else {
        this.animateContentSize(
            animationSpec = tween(
                durationMillis = MotionSpec.DURATION_STANDARD,
                easing = FastOutSlowInEasing
            )
        )
    }
}

/**
 * Smooth, subtle animated counter / metric value display (160ms vertical fade slide).
 */
@Composable
fun AnimatedMetricText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1
) {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            style = style,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(160, easing = FastOutSlowInEasing)) { it / 4 }) togetherWith
                (fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(120, easing = FastOutSlowInEasing)) { -it / 4 })
        },
        label = "metric_text_transition"
    ) { target ->
        Text(
            text = target,
            modifier = modifier,
            color = color,
            style = style,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
