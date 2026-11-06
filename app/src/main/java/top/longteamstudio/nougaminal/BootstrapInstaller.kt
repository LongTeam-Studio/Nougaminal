package top.longteamstudio.nougaminal

import android.content.Context
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object BootstrapInstaller {
    fun install(context: Context) {
        val prefixDir = File(context.filesDir, "usr")
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val expectedNativeBash = File(nativeLibDir, "libbash.so")
        val bashLink = File(prefixDir, "bin/bash")
        val prootLink = File(prefixDir, "bin/proot")

        if (bashLink.exists() && expectedNativeBash.exists() && prootLink.exists()) {
            try {
                val result = Runtime.getRuntime().exec(arrayOf(bashLink.absolutePath, "-c", "echo ok")).waitFor()
                if (result == 0) return
            } catch (e: Exception) {}
        }

        try { prefixDir.deleteRecursively() } catch (e: Exception) {}

        val stagingDir = File(context.filesDir, "usr-staging")
        try { stagingDir.deleteRecursively() } catch (e: Exception) {}
        stagingDir.mkdirs()

        var symlinksFile: File? = null

        try {
            context.assets.open("bootstrap.zip").use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newPath = entry.name.substringAfter("files/usr/")
                        val outFile = File(stagingDir, newPath)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else if (newPath.contains("/bin/")) {
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                            if (newPath.endsWith("SYMLINKS.txt")) symlinksFile = outFile
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            symlinksFile?.let { file ->
                file.forEachLine { line ->
                    if (line.isNotBlank() && line.contains("←")) {
                        val parts = line.split("←")
                        if (parts.size == 2) {
                            val target = parts[0].trim()
                            val linkPath = parts[1].trim().replace("com.termux", context.packageName)
                            val linkFile = File(stagingDir, linkPath)
                            val targetName = target.substringAfterLast("/")
                            val nativePath = nativeLibDir + "/lib" + targetName.replace("-", "_") + ".so"
                            linkFile.parentFile?.mkdirs()
                            try {
                                if (linkFile.exists()) linkFile.delete()
                                File(stagingDir.absolutePath + "/" + target).copyTo(linkFile, overwrite = true)
                            } catch (e: Exception) {}
                        }
                    }
                }
            }

            if (stagingDir.renameTo(prefixDir)) {
                Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", prefixDir.absolutePath)).waitFor()
            }
        } catch (e: Exception) {
            e.printStackTrace(); File(context.filesDir, "install_error.txt").writeText(e.stackTraceToString())
        }
    }
}
