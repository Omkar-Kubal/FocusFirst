package com.focusfirst.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
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
                    setBackgroundColor(Color.TRANSPARENT)
                    isClickable = false
                    isFocusable = false

                    settings.apply {
                        javaScriptEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        mediaPlaybackRequiresUserGesture = false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest,
                        ): WebResourceResponse? {
                            val url = request.url.toString()
                            if (url.startsWith("https://localhost/models/")) {
                                return try {
                                    val assetPath = url.substringAfter("https://localhost/")
                                    val stream = context.assets.open(assetPath)
                                    WebResourceResponse(
                                        "model/gltf-binary",
                                        null,
                                        200,
                                        "OK",
                                        mapOf(
                                            "Access-Control-Allow-Origin" to "*",
                                            "Access-Control-Allow-Methods" to "GET",
                                            "Cache-Control" to "no-cache",
                                        ),
                                        stream,
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("PlanetView", "Failed to serve: $url", e)
                                    null
                                }
                            }
                            return null
                        }
                    }

                    // modelPath is e.g. "models/earth_stage1.glb"
                    val glbUrl = "https://localhost/$modelPath"
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width">
                        <script type="module"
                          src="https://ajax.googleapis.com/ajax/libs/model-viewer/3.3.0/model-viewer.min.js">
                        </script>
                        <style>
                          * { margin:0; padding:0; }
                          body {
                            background: transparent !important;
                            width:100vw; height:100vh; overflow:hidden;
                          }
                          model-viewer {
                            width:100%; height:100%;
                            background-color: transparent !important;
                            --progress-bar-color: transparent;
                          }
                        </style>
                        </head>
                        <body>
                        <model-viewer
                          src="$glbUrl"
                          auto-rotate
                          auto-rotate-delay="0"
                          rotation-per-second="15deg"
                          camera-controls="false"
                          disable-zoom
                          disable-pan
                          interaction-prompt="none"
                          shadow-intensity="0"
                          exposure="1.2"
                          background-color="transparent"
                          style="background:transparent;">
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
