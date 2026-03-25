package com.focusfirst.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

    var isLoading     by remember { mutableStateOf(true) }
    // Accumulate rotation separately to avoid floating-point drift from read-back.
    var rotationAngle by remember(modelPath) { mutableFloatStateOf(0f) }

    val modelNode = remember(modelPath) {
        val instance = try {
            modelLoader.createModelInstance(modelPath)
        } catch (e: Exception) {
            Log.w("PlanetView", "Model not found: $modelPath, falling back to earth")
            modelLoader.createModelInstance("models/earth_stage1.glb")
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
                // Fixed increment per frame as intervalSeconds is not available in this version's onFrame
                rotationAngle += 0.005f
                modelNode.rotation = Float3(0f, rotationAngle, 0f)
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
