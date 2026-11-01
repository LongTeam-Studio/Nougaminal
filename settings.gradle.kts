pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Termux 官方仓库（解决 terminal-emulator 拉取报错）
        maven { url = uri("https://maven.termux.dev/") }
        // ConnectBot 的 termlib 依然在 JitPack
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "TermuxKotlin"
include(":app")
