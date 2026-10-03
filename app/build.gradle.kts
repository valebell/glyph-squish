import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.room)
}

ktlint {
    android.set(true)
    version.set(libs.versions.ktlint)
}

detekt {
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
    config.setFrom(rootProject.file("config/detekt.yml"))
    // Keep the curated correctness rules explicit; ktlint owns formatting.
    buildUponDefaultConfig = false
}

// Correctness and parameter-count rules need type resolution for app and test code.
tasks.named("detekt") {
    dependsOn("detektDebug", "detektDebugUnitTest", "detektDebugAndroidTest")
}

// Include generated Java classes (notably BuildConfig) in Kotlin type resolution.
tasks.withType<Detekt>().configureEach {
    if (name == "detektDebug" || name == "detektRelease") {
        val variant = name.removePrefix("detekt")
        // An explicit classpath replaces Detekt's convention, so retain Kotlin's libraries too.
        classpath.setFrom(
            tasks.named<KotlinCompile>("compile${variant}Kotlin").map { it.libraries },
            tasks.named<JavaCompile>("compile${variant}JavaWithJavac").flatMap { it.destinationDirectory },
        )
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

val appVersion =
    Properties().apply {
        rootProject.file("version.properties").inputStream().use { load(it) }
    }

android {
    namespace = "com.valebell.glyphsquish"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.valebell.glyphsquish"
        minSdk = 34
        targetSdk = 37
        versionCode = appVersion.getProperty("versionCode").toInt()
        versionName = appVersion.getProperty("versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    androidResources {
        // SoundPool opens resources by file descriptor; FLAC already compresses the audio.
        noCompress += "flac"
    }
    lint {
        warningsAsErrors = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Reuse the tested Kotlin synthesizer to regenerate the checked-in SoundPool samples.
tasks.register<JavaExec>("generateSoundAssets") {
    group = "assets"
    description = "Regenerates and verifies lossless FLAC effects from GlyphSynthesizer (requires FFmpeg)."
    val compilation = tasks.named<KotlinCompile>("compileDebugUnitTestKotlin")
    dependsOn(compilation)
    classpath(compilation.map { it.libraries }, compilation.flatMap { it.destinationDirectory })
    mainClass.set("com.valebell.glyphsquish.ui.audio.GenerateSoundAssetsKt")
    args(
        layout.projectDirectory
            .dir("src/main/res/raw")
            .asFile.absolutePath,
    )
}

// Resource coverage and sound checksum tests read source files directly.
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/res").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/AndroidManifest.xml").withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    constraints {
        // AGP locks test dependencies to the app; align the transitive futures dependency.
        implementation(libs.androidx.concurrent.futures)
    }
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
