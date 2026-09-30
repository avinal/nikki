plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

fun gitVersionName(): String {
    return providers.exec {
        commandLine("git", "describe", "--tags", "--always", "--dirty")
    }.standardOutput.asText.get().trim().removePrefix("v")
}

extra["gitVersionName"] = gitVersionName()
extra["gitVersionCode"] = (property("VERSION_CODE") as String).toInt()
