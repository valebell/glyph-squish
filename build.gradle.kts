// Override AGP's bundled KGP while retaining built-in Kotlin support.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}
plugins {
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.room) apply false
}
