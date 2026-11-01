package com.example.termux
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.termux.terminal.PtyProcess
import com.example.termux.terminal.TerminalScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=${filesDir.absolutePath}",
            "PREFIX=${filesDir.parentFile}/usr",
            "PATH=${filesDir.parentFile}/usr/bin:/system/bin"
        )
        val pty = PtyProcess("/system/bin/sh", filesDir.absolutePath, env, scope)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) { TerminalScreen(pty = pty) }
        }
    }
}
