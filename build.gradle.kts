// Top-level build file — plugin declarations only, no configuration here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.hilt.android)        apply false
    alias(libs.plugins.ksp)                 apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.3" apply false
}
