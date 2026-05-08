package com.focusfirst.ui.mascot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.focusfirst.R
import com.focusfirst.data.model.TimerMode
import com.focusfirst.data.model.TimerPhase

/**
 * Animated mascot composable for the Toki timer ring.
 *
 * Resolves the correct [MascotState] from the current timer state, then
 * renders the matching drawable with a continuous floating animation and
 * a bouncy cross-fade when the state changes.
 *
 * Drawables in [res/drawable] carry their original SVG colours (cat body near-black,
 * coloured props, white eye-whites). The caller is responsible for placing a light
 * background circle behind this composable so the dark cat body is visible against it.
 */
@Composable
fun TokiMascot(
    isIdle:   Boolean,
    isPaused: Boolean,
    mode:     TimerMode,
    phase:    TimerPhase,
    modifier: Modifier = Modifier,
) {
    val state     = MascotState.from(isIdle, isPaused, mode, phase)
    val amplitude = dimensionResource(R.dimen.mascot_float_amplitude).value

    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -amplitude,
        targetValue  =  amplitude,
        animationSpec = infiniteRepeatable(
            animation  = tween(MascotDefaults.FLOAT_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot_float_offset",
    )

    // Capture density at composition time; used inside graphicsLayer for dp→px.
    val density = LocalDensity.current.density

    BoxWithConstraints(
        modifier          = modifier.aspectRatio(1f),
        contentAlignment  = Alignment.Center,
    ) {
        val mascotSize = maxWidth * MascotDefaults.SIZE_FRACTION

        AnimatedContent(
            targetState  = state,
            transitionSpec = {
                (fadeIn(tween(MascotDefaults.TRANSITION_IN_MS)) + scaleIn(
                    initialScale  = MascotDefaults.TRANSITION_SCALE,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessLow,
                    ),
                )).togetherWith(
                    fadeOut(tween(MascotDefaults.TRANSITION_OUT_MS)) + scaleOut(
                        targetScale   = MascotDefaults.TRANSITION_SCALE,
                        animationSpec = tween(MascotDefaults.TRANSITION_OUT_MS),
                    )
                )
            },
            label = "mascot_state",
        ) { currentState ->
            Box(
                modifier = Modifier
                    .size(mascotSize)
                    // translationY + scale both live in the graphicsLayer lambda
                    // (draw phase).  floatOffset is read there via its State getter,
                    // so only the GPU layer is invalidated each frame — no Compose
                    // recomposition from the infinite float animation.
                    .graphicsLayer {
                        translationY    = floatOffset * density
                        val breathScale = 1f + (floatOffset / MascotDefaults.BREATH_SCALE_DIVISOR)
                        scaleX = breathScale
                        scaleY = breathScale
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter            = painterResource(currentState.drawableRes),
                    contentDescription = "Toki",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
