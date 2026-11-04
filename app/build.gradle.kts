import java.util.zip.ZipFile
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "top.longteamstudio.nougaminal"
    compileSdk = 34
    defaultConfig {
        applicationId = "top.longteamstudio.nougaminal"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.8-PV"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += "zip" }
}
dependencies {
    implementation(files("libs/terminal-view-0.118.0.aar"))
    implementation(files("libs/terminal-emulator-0.118.0.aar"))
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
}
tasks.register("extractBootstrapBinaries") {
    doLast {
        val zipFile = file("src/main/assets/bootstrap.zip")
        if (!zipFile.exists()) {
            println("bootstrap.zip not found, skip")
            return@doLast
        }
        val jniLibsDir = file("src/main/jniLibs/arm64-v8a")
        jniLibsDir.mkdirs()
        var count = 0
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.contains("/bin/") }
                .forEach { entry ->
                    val fileName = entry.name.substringAfterLast("/")
                    if (fileName.endsWith(".so") || fileName.isEmpty()) return@forEach
                    val outFile = File(jniLibsDir, "lib" + fileName.replace("-", "_") + ".so")
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    count++
                }
        }
        println("Extracted " + count + " binaries to jniLibs")
    }
}
tasks.named("preBuild").configure { dependsOn("extractBootstrapBinaries") }
