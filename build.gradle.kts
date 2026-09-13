plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.google.services) apply false
}

/** Single source of truth for app version (APK + generated [AppBuildInfo]). */
private fun releaseVersionName(): String =
    findProperty("versionName")?.toString()
        ?: System.getenv("FROMCHAT_VERSION_NAME")
        ?: "1.1.4"

private fun releaseVersionCode(): Int =
    findProperty("versionCode")?.toString()?.toIntOrNull()
        ?: releaseVersionName().replace(Regex("[^0-9]"), "").toIntOrNull()?.takeIf { it > 0 }
        ?: 114

extra["versionName"] = releaseVersionName()
extra["versionCode"] = releaseVersionCode()

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.google.services)
    }
}