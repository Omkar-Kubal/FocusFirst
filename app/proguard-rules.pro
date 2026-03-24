# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ── SceneView / Filament ──────────────────────────────────────────────────────
# Keep Filament JNI entry points and native bindings
-keep class com.google.android.filament.** { *; }
-keep class io.github.sceneview.** { *; }
# Keep GLB/GLTF asset loader reflection classes
-keep class com.google.android.filament.gltfio.** { *; }
# Keep kotlin-math used for Float3 rotation
-keep class dev.romainguy.kotlin.math.** { *; }