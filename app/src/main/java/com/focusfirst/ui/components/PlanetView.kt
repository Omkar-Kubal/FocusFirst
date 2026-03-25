package com.focusfirst.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader

/**
 * Renders a rotating 3D GLB planet using SceneView / Filament.
 *
 * [modelPath] maps to app/src/main/assets/ — e.g. "models/earth_stage1.glb".
 * [isOpaque] = false keeps the SceneView background transparent so the
 * parent #000000 surface shows through.
 *
 * If a GLB is missing (e.g. future skins not yet shipped), falls back to
 * earth_stage1.glb. Earth and Mars files are guaranteed present and will
 * re-throw rather than silently falling back.
 */
@Composable
fun PlanetView(
    modelPath: String,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
) {
    val engine            = rememberEngine()
    val modelLoader       = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    var isLoading   by remember { mutableStateOf(true) }
    // Plain FloatArray — not Compose state, so onFrame updates don't trigger recomposition.
    val rotationRef = remember(modelPath) { floatArrayOf(0f) }

    val modelNode = remember(modelPath) {
        val instance = try {
            modelLoader.createModelInstance(modelPath)
        } catch (e: Exception) {
            val fallback = getFallbackPath(modelPath)
            if (fallback != modelPath) {
                Log.w("PlanetView", "Model not found: $modelPath, falling back to $fallback")
                modelLoader.createModelInstance(fallback)
            } else {
                throw e  // re-throw if even the primary path should exist
            }
        }
        ModelNode(
            modelInstance = instance,
            scaleToUnits  = 1.8f,
        ).apply {
            isEditable = false
        }
    }

    // Release Filament resources when modelPath changes or composable leaves.
    DisposableEffect(modelPath) {
        onDispose { modelNode.destroy() }
    }

    // Re-keyed on modelNode so the list is rebuilt whenever the path changes.
    val nodes: SnapshotStateList<Node> = remember(modelNode) {
        mutableListOf<Node>(modelNode).toMutableStateList()
    }

    Box(modifier = modifier.size(size)) {
        Scene(
            modifier          = Modifier.fillMaxSize(),
            engine            = engine,
            modelLoader       = modelLoader,
            childNodes        = nodes,
            environmentLoader = environmentLoader,
            isOpaque          = false,
            onFrame           = { _ ->
                if (isLoading) isLoading = false
                // 0.3 deg/frame × 60 fps ≈ 18 deg/sec → ~20 sec full rotation
                rotationRef[0] = (rotationRef[0] + 0.3f) % 360f
                modelNode.rotation = Float3(0f, rotationRef[0], 0f)
            },
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.align(Alignment.Center),
                color       = Color.White,
                strokeWidth = 1.dp,
            )
        }
    }
}

/**
 * Returns the path to use when [modelPath] fails to load.
 *
 * - earth / mars  → these files are present; return the same path so the
 *   caller re-throws and the real error surfaces.
 * - all other skins (ocean, ice, lava, alien) → not yet shipped; fall back
 *   to earth_stage1.glb silently.
 */
private fun getFallbackPath(modelPath: String): String {
    val skin = modelPath
        .substringAfter("models/")
        .substringBefore("_stage")
    return when (skin) {
        "earth", "mars" -> modelPath                   // guaranteed present — let it throw
        else            -> "models/earth_stage1.glb"   // future skins not yet shipped
    }
}
