package top.longteamstudio.nougaminal

import android.content.Context
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object BootstrapInstaller {
    fun install(context: Context) {
        val prefixDir = File(context.filesDir.parentFile, "usr")
        if (File(prefixDir, "bin/bash").exists()) return

        try {
            // 1. 解压所有文件到 staging 目录
            val stagingDir = File(context.filesDir.parentFile, "usr-staging")
            if (stagingDir.exists()) stagingDir.deleteRecursively()

            var symlinksFile: File? = null

            context.assets.open("bootstrap.zip").use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newPath = entry.name.replace("com.termux", context.packageName)
                        val outFile = File(stagingDir, newPath)
                        
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                            // 记录 SYMLINKS.txt 的位置
                            if (newPath.endsWith("SYMLINKS.txt")) {
                                symlinksFile = outFile
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // 2. 解析 SYMLINKS.txt 并创建软链接
            symlinksFile?.let { file ->
                file.forEachLine { line ->
                    if (line.isNotBlank() && line.contains("←")) {
                        val parts = line.split("←")
                        if (parts.size == 2) {
                            val target = parts[0].trim()
                            val linkPath = parts[1].trim().replace("com.termux", context.packageName)
                            val linkFile = File(stagingDir, linkPath)
                            
                            linkFile.parentFile?.mkdirs()
                            // 使用 Android 原生 Os.symlink 创建软链接
                            if (!linkFile.exists()) {
                                try {
                                    Os.symlink(target, linkFile.absolutePath)
                                } catch (e: Exception) {
                                    // 降级方案：用系统 ln 命令[reference:4]
                                    Runtime.getRuntime().exec("ln -sf $target ${linkFile.absolutePath}").waitFor()
                                }
                            }
                        }
                    }
                }
            }

            // 3. 将 staging 目录重命名为正式的 usr 目录
            if (stagingDir.renameTo(prefixDir)) {
                // 设置基础执行权限
                Runtime.getRuntime().exec("chmod -R 755 ${prefixDir.absolutePath}/bin").waitFor()
                Runtime.getRuntime().exec("chmod -R 755 ${prefixDir.absolutePath}/lib").waitFor()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}