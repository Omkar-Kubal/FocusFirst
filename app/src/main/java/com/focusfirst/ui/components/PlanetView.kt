package com.focusfirst.ui.components

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlanetView(
    modelPath: String,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
) {
    key(modelPath) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    // Critical rendering flags
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    background = null

                    settings.apply {
                        javaScriptEnabled = true
                        allowFileAccess = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        mediaPlaybackRequiresUserGesture = false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest,
                        ): WebResourceResponse? {
                            val url = request.url.toString()

                            // Serve model-viewer from assets
                            if (url == "https://localhost/model-viewer.min.js") {
                                return try {
                                    WebResourceResponse(
                                        "application/javascript",
                                        "UTF-8", 200, "OK",
                                        mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Cache-Control" to "no-cache",
                                        ),
                                        context.assets.open("model-viewer.min.js"),
                                    )
                                } catch (e: Exception) { null }
                            }

                            // Serve GLB from assets
                            if (url.startsWith("https://localhost/models/")) {
                                return try {
                                    val fileName = url.substringAfter("https://localhost/")
                                    WebResourceResponse(
                                        "model/gltf-binary",
                                        null, 200, "OK",
                                        mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Access-Control-Allow-Methods" to "GET",
                                            "Cache-Control" to "no-cache",
                                        ),
                                        context.assets.open(fileName),
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("PlanetView", "Failed: $url", e)
                                    null
                                }
                            }
                            return null
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            android.util.Log.d("PlanetView", "Page loaded: $url")
                            view.evaluateJavascript(
                                """
                                (function() {
                                    var mv = document.querySelector('model-viewer');
                                    return mv ? 'found:' + mv.src : 'not-found';
                                })()
                                """.trimIndent(),
                            ) { result ->
                                android.util.Log.d("PlanetView", "model-viewer: $result")
                            }
                        }
                    }

                    val html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport"
  content="width=device-width,initial-scale=1">
<script type="module"
  src="https://localhost/model-viewer.min.js">
</script>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
html, body {
  width:100%; height:100%;
  background:transparent !important;
  overflow:hidden;
}
model-viewer {
  width:100%; height:100%;
  background-color:transparent !important;
  --poster-color:transparent;
  --progress-bar-color:transparent;
  --progress-mask:transparent;
}
</style>
</head>
<body>
<model-viewer
  src="https://localhost/$modelPath"
  auto-rotate
  auto-rotate-delay="0"
  rotation-per-second="20deg"
  camera-orbit="0deg 75deg 200%"
  min-camera-orbit="auto auto 150%"
  max-camera-orbit="auto auto 250%"
  field-of-view="30deg"
  disable-zoom
  disable-pan
  interaction-prompt="none"
  shadow-intensity="0.5"
  exposure="1.0"
  tone-mapping="commerce">
</model-viewer>
</body>
</html>
                    """.trimIndent()

                    loadDataWithBaseURL(
                        "https://localhost/",
                        html,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }
            },
        )
    }
}
