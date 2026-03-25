package com.focusfirst.ui.components

import android.graphics.PixelFormat
import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.Renderer
import com.google.android.filament.utils.ModelViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Probe whether Filament's native library actually loads on this device/emulator.
// Filament 1.70.0 ships x86_64 + arm64 + armeabi-v7a + x86 .so files, but they
// are Deflate-compressed inside the AAR.  With extractNativeLibs="true" Android
// extracts them to disk on install, so they always load correctly.  This one-time
// probe catches any remaining environment where Filament cannot initialise (e.g.
// very old Mali drivers that don't support GLES 3.0).
private val filamentSupported: Boolean by lazy {
    runCatching {
        val engine = com.google.android.filament.Engine.create()
        engine.destroy()
    }.isSuccess
}

/**
 * Renders a planet for the given [modelPath].
 * On ARM devices: photorealistic GLB via Filament [ModelViewer].
 * On x86/x86_64 (emulator): 2-D Canvas fallback so the app doesn't crash.
 */
@Composable
fun PlanetView(
    modelPath: String,
    modifier:  Modifier = Modifier,
    size:      Dp       = 240.dp,
) {
    if (filamentSupported) {
        FilamentPlanetView(modelPath = modelPath, modifier = modifier, size = size)
    } else {
        CanvasPlanetView(modelPath = modelPath, modifier = modifier, size = size)
    }
}

// ─── Filament renderer ────────────────────────────────────────────────────────

private const val DEFAULT_OBJECT_Z    = -4f
private const val ROTATION_DEG_PER_SEC = 20f

@Composable
private fun FilamentPlanetView(
    modelPath: String,
    modifier:  Modifier = Modifier,
    size:      Dp       = 240.dp,
) {
    key(modelPath) {
        val scope = rememberCoroutineScope()

        val viewerRef    = remember { arrayOfNulls<ModelViewer>(1) }
        val bboxDataRef  = remember { arrayOfNulls<FloatArray>(1) }   // [scale, cx, cy, cz]
        val angleRef     = remember { floatArrayOf(0f) }
        val prevTimeRef  = remember { longArrayOf(0L) }
        val choreographer = remember { Choreographer.getInstance() }

        val frameCallback = remember {
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    val viewer = viewerRef[0]
                    if (viewer == null) {
                        choreographer.postFrameCallback(this)
                        return
                    }

                    val prev = prevTimeRef[0]
                    if (prev != 0L) {
                        val dtSec = (frameTimeNanos - prev) / 1_000_000_000f
                        angleRef[0] = (angleRef[0] + ROTATION_DEG_PER_SEC * dtSec) % 360f
                    }
                    prevTimeRef[0] = frameTimeNanos

                    val bbox  = bboxDataRef[0]
                    val asset = viewer.asset
                    if (bbox != null && asset != null) {
                        val s    = bbox[0]; val cx = bbox[1]; val cy = bbox[2]; val cz = bbox[3]
                        val rad  = Math.toRadians(angleRef[0].toDouble()).toFloat()
                        val cosA = cos(rad)
                        val sinA = sin(rad)
                        val tx   = s * (cz * sinA - cx * cosA)
                        val ty   = -s * cy
                        val tz   = s * (-cx * sinA - cz * cosA) + DEFAULT_OBJECT_Z
                        // Column-major 4×4: T(0,0,DEFAULT_Z) · Scale(s) · Ry(angle) · T(-center)
                        val m = floatArrayOf(
                             s * cosA,  0f,  s * sinA, 0f,   // col 0
                             0f,        s,   0f,       0f,   // col 1
                            -s * sinA,  0f,  s * cosA, 0f,   // col 2
                             tx,        ty,  tz,       1f,   // col 3
                        )
                        val tm   = viewer.engine.transformManager
                        val inst = tm.getInstance(asset.root)
                        tm.setTransform(inst, m)
                    }

                    viewer.render(frameTimeNanos)
                    choreographer.postFrameCallback(this)
                }
            }
        }

        AndroidView(
            modifier = modifier.size(size),
            factory  = { context ->
                SurfaceView(context).apply {
                    setZOrderMediaOverlay(true)
                    holder.setFormat(PixelFormat.TRANSLUCENT)

                    val viewer = ModelViewer(this)
                    viewerRef[0] = viewer

                    viewer.renderer.clearOptions =
                        Renderer.ClearOptions().apply { clear = true }

                    scope.launch {
                        val buffer = withContext(Dispatchers.IO) {
                            context.assets.open(modelPath).use { stream ->
                                ByteBuffer.wrap(stream.readBytes())
                            }
                        }
                        if (viewerRef[0] == null) return@launch

                        viewer.loadModelGlb(buffer)
                        viewer.transformToUnitCube()

                        val bb            = viewer.asset!!.boundingBox
                        val maxHalfExtent = maxOf(
                            bb.halfExtent[0], bb.halfExtent[1], bb.halfExtent[2]
                        )
                        val s          = if (maxHalfExtent > 1e-6f) 1f / maxHalfExtent else 1f
                        bboxDataRef[0] = floatArrayOf(s, bb.center[0], bb.center[1], bb.center[2])
                    }

                    choreographer.postFrameCallback(frameCallback)
                }
            },
            onRelease = {
                choreographer.removeFrameCallback(frameCallback)
                viewerRef[0]?.let { viewer ->
                    viewer.destroyModel()
                    viewer.engine.destroy()
                }
                viewerRef[0]  = null
                bboxDataRef[0] = null
            },
        )
    }
}

// ─── Canvas fallback (x86 / emulator) ────────────────────────────────────────

@Composable
private fun CanvasPlanetView(
    modelPath: String,
    modifier:  Modifier = Modifier,
    size:      Dp       = 240.dp,
) {
    val skinName = modelPath.substringAfter("models/").substringBefore("_stage")
    val stage    = modelPath.substringAfter("_stage").substringBefore(".glb").toIntOrNull() ?: 1

    val rotation by rememberInfiniteTransition(label = "planet_rotation").animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label         = "rotation",
    )

    val colors = getPlanetColors(skinName, stage)

    Canvas(modifier = modifier.size(size)) {
        val cx     = this.size.width / 2f
        val cy     = this.size.height / 2f
        val radius = this.size.width * 0.44f

        // Atmospheric glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.glow.copy(alpha = 0.4f), colors.glow.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(cx, cy), radius = radius * 1.4f,
            ),
            radius = radius * 1.4f, center = Offset(cx, cy),
        )

        // Planet base
        drawCircle(color = colors.base, radius = radius, center = Offset(cx, cy))

        // Land patches
        val patchRandom = Random(stage * 137 + skinName.hashCode())
        val patchCount  = 5 + stage * 2
        repeat(patchCount) { i ->
            val baseAngle = patchRandom.nextFloat() * 360f
            val animAngle = Math.toRadians((baseAngle + rotation * (0.2f + i * 0.05f)).toDouble())
            val dist = patchRandom.nextFloat() * radius * 0.65f
            val px = cx + cos(animAngle).toFloat() * dist
            val py = cy + sin(animAngle).toFloat() * dist * 0.55f
            val dfc = sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
            if (dfc < radius * 0.88f) {
                val pr = radius * (0.1f + patchRandom.nextFloat() * 0.18f)
                drawOval(
                    color   = colors.land,
                    topLeft = Offset(px - pr, py - pr * 0.7f),
                    size    = Size(pr * 2f, pr * 1.4f),
                    alpha   = 0.55f + patchRandom.nextFloat() * 0.35f,
                )
            }
        }

        // Clouds (stage 3+)
        if (stage >= 3) {
            val cloudRandom = Random(stage * 31 + 7)
            repeat((stage - 1) * 2) { _ ->
                val baseAngle = cloudRandom.nextFloat() * 360f
                val animAngle = Math.toRadians((baseAngle + rotation * 0.7f).toDouble())
                val dist = cloudRandom.nextFloat() * radius * 0.7f
                val px = cx + cos(animAngle).toFloat() * dist
                val py = cy + sin(animAngle).toFloat() * dist * 0.5f
                if (sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy)) < radius * 0.82f) {
                    drawOval(
                        color   = Color.White,
                        topLeft = Offset(px - radius * 0.18f, py - radius * 0.07f),
                        size    = Size(radius * 0.36f, radius * 0.14f),
                        alpha   = 0.2f + stage / 8f,
                    )
                }
            }
        }

        // Polar ice caps (stage 4+)
        if (stage >= 4) {
            drawOval(color = Color.White, topLeft = Offset(cx - radius * 0.55f, cy - radius * 0.92f), size = Size(radius * 1.1f, radius * 0.35f), alpha = 0.75f)
            drawOval(color = Color.White, topLeft = Offset(cx - radius * 0.45f, cy + radius * 0.62f), size = Size(radius * 0.9f,  radius * 0.28f), alpha = 0.6f)
        }

        // City lights (stage 5+)
        if (stage >= 5) {
            val cityRandom = Random(stage * 23)
            repeat(8) {
                val angle = cityRandom.nextFloat() * Math.PI.toFloat() + Math.PI.toFloat() / 2f
                val dist  = cityRandom.nextFloat() * radius * 0.6f
                drawCircle(color = Color(0xFFFFD700), radius = 2.5f, center = Offset(cx + cos(angle) * dist, cy + sin(angle) * dist * 0.6f), alpha = 0.5f + cityRandom.nextFloat() * 0.4f)
            }
        }

        // Atmosphere rim
        drawCircle(color = colors.atmosphere, radius = radius, center = Offset(cx, cy), style = Stroke(width = radius * 0.06f), alpha = 0.5f)

        // Light shine (top-left)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f), Color.Transparent),
                center = Offset(cx - radius * 0.25f, cy - radius * 0.3f), radius = radius * 0.75f,
            ),
            radius = radius, center = Offset(cx, cy),
        )

        // Shadow (right side)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                center = Offset(cx + radius * 0.4f, cy), radius = radius * 1.1f,
            ),
            radius = radius, center = Offset(cx, cy),
        )

        // Clip ring
        drawCircle(color = Color.Black, radius = radius, center = Offset(cx, cy), style = Stroke(width = radius * 0.15f))
    }
}

// ─── Colour schemes ───────────────────────────────────────────────────────────

data class PlanetColorScheme(val base: Color, val land: Color, val atmosphere: Color, val glow: Color)

fun getPlanetColors(skin: String, stage: Int): PlanetColorScheme = when (skin) {
    "mars"  -> PlanetColorScheme(Color(0xFF3A1000), Color(0xFF922800), Color(0xFFE84B1A), Color(0xFF5A1800))
    "ocean" -> PlanetColorScheme(Color(0xFF020D28), Color(0xFF071D55), Color(0xFF2B7BE0), Color(0xFF0A2A6E))
    "ice"   -> PlanetColorScheme(Color(0xFF0F1428), Color(0xFF3A5090), Color(0xFF85B7EB), Color(0xFF2A3A70))
    "lava"  -> PlanetColorScheme(Color(0xFF0D0200), Color(0xFFB83000), Color(0xFFFF4500), Color(0xFF5A1800))
    "alien" -> PlanetColorScheme(Color(0xFF0E001F), Color(0xFF5500AA), Color(0xFF9B59FF), Color(0xFF2D0057))
    else    -> when (stage) {
        1    -> PlanetColorScheme(Color(0xFF1A1208), Color(0xFF3D2A1A), Color(0xFF6B6560), Color(0xFF1A1208))
        2    -> PlanetColorScheme(Color(0xFF0F2010), Color(0xFF2D5A2D), Color(0xFF1A6B3C), Color(0xFF0F2010))
        3    -> PlanetColorScheme(Color(0xFF0A3015), Color(0xFF2D7A3D), Color(0xFF1A9E5F), Color(0xFF0A2A10))
        4    -> PlanetColorScheme(Color(0xFF082A40), Color(0xFF3A8A50), Color(0xFF2B7BE0), Color(0xFF082040))
        5    -> PlanetColorScheme(Color(0xFF0A3A50), Color(0xFF4A9A60), Color(0xFF2B7BE0), Color(0xFF082848))
        else -> PlanetColorScheme(Color(0xFF0F4060), Color(0xFF5AAA70), Color(0xFF60B0FF), Color(0xFF0A3060))
    }
}
