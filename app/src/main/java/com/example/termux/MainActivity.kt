package com.example.termux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView

class MainActivity : ComponentActivity() {
    private lateinit var terminalView: TerminalView
    private var terminalSession: TerminalSession? = null

    private val sessionClient = object : TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) {}
        override fun onTitleChanged(changedSession: TerminalSession) {}
        override fun onSessionFinished(finishedSession: TerminalSession) {}
        override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {}
        override fun onPasteTextFromClipboard(session: TerminalSession?) {}
        override fun onBell(session: TerminalSession) {}
        override fun onColorsChanged(session: TerminalSession) {}
        override fun onTerminalCursorStateChange(state: Boolean) {}
        override fun getTerminalCursorStyle(): Int? = null
        override fun logError(tag: String?, message: String?) {}
        override fun logWarn(tag: String?, message: String?) {}
        override fun logInfo(tag: String?, message: String?) {}
        override fun logDebug(tag: String?, message: String?) {}
        override fun logVerbose(tag: String?, message: String?) {}
        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
        override fun logStackTrace(tag: String?, e: Exception?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    terminalView = TerminalView(context).apply {
                        setTerminalViewClient(object : TerminalView.TerminalViewClient {
                            override fun onScale(scale: Float): Boolean = false
                            override fun onSingleTapUp(e: android.view.MotionEvent?) {}
                            override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                            override fun shouldEnforceCharBasedInput(): Boolean = false
                            override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                            override fun isTerminalViewSelected(): Boolean = true
                            override fun copyModeChanged(copyMode: Boolean) {}
                            override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent, session: TerminalSession?): Boolean = false
                            override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent): Boolean = false
                            override fun onLongPress(event: android.view.MotionEvent?): Boolean = false
                            override fun readControlSequenceFromTerminal(session: TerminalSession?) {}
                            override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
                            override fun onEmulatorSet() {}
                            override fun logStderr(message: String?) {}
                            override fun logStackTrace(message: String?) {}
                        })
                    }
                }
            )
            startShellSession()
        }
    }

    private fun startShellSession() {
        val shell = "/system/bin/sh"
        val cwd = filesDir.absolutePath
        val args = arrayOf("-l")
        val env = arrayOf(
            "TERM=xterm-256color",
            "HOME=$cwd",
            "PATH=/sbin:/system/sbin:/system/bin:/system/xbin"
        )
        
        terminalSession = TerminalSession(shell, cwd, args, env, 1000, sessionClient)
        terminalView.attachSession(terminalSession)
        terminalSession?.updateSize(80, 24, 0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalSession?.finish()
        terminalSession = null
    }
}
