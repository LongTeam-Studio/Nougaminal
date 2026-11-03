plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "top.longteamstudio.nougaminal"
    compileSdk = 34
    defaultConfig {
        applicationId = "top.longteamstudio.nougaminal"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    // 只使用本地的 Termux 官方 AAR
    implementation(files("libs/terminal-view-0.118.0.aar"))
    implementation(files("libs/terminal-emulator-0.118.0.aar"))
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
}

// 补全 ProfileInstaller 缺失的 ListenableFuture 依赖
dependencies {
    implementation("com.google.guava:guava:32.1.3-android")
    implementation("androidx.concurrent:concurrent-futures:1.2.0")
}

// 强制所有组件使用同一个版本的 ListenableFuture
configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:32.1.3-android")
        force("androidx.concurrent:concurrent-futures:1.2.0")
    }
}
