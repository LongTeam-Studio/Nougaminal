package com.example.termux

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

class MainActivity : ComponentActivity() {
    private lateinit var terminalView: TerminalView
    private var terminalSession: TerminalSession? = null

    private val sessionClient = object : TerminalSessionClient {
        // 【核心修复】：内容变化时，强制 TerminalView 重绘
        override fun onTextChanged(changedSession: TerminalSession) {
            runOnUiThread {
                if (::terminalView.isInitialized) {
                    terminalView.invalidate()
                }
            }
        }
        override fun onTitleChanged(changedSession: TerminalSession) {}
        override fun onSessionFinished(finishedSession: TerminalSession) {}
        override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {}
        override fun onPasteTextFromClipboard(session: TerminalSession?) {}
        override fun onBell(session: TerminalSession) {}
        override fun onColorsChanged(session: TerminalSession) {}
        override fun onTerminalCursorStateChange(state: Boolean) {}
        override fun getTerminalCursorStyle(): Int? = null
        override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
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
                    terminalView = TerminalView(context, null).apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        setTextSize(20)
                        
                        setTerminalViewClient(object : TerminalViewClient {
                            override fun onScale(scale: Float): Float = 1.0f
                            override fun onSingleTapUp(e: MotionEvent?) {
                                requestFocus()
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(this@apply, InputMethodManager.SHOW_IMPLICIT)
                            }
                            override fun shouldBackButtonBeMappedToEscape(): Boolean = false
                            override fun shouldEnforceCharBasedInput(): Boolean = false
                            override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
                            override fun isTerminalViewSelected(): Boolean = true
                            override fun copyModeChanged(copyMode: Boolean) {}
                            override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession?): Boolean = false
                            override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
                            override fun onLongPress(event: MotionEvent?): Boolean = false
                            override fun readControlKey(): Boolean = false
                            override fun readAltKey(): Boolean = false
                            override fun readShiftKey(): Boolean = false
                            override fun readFnKey(): Boolean = false
                            override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
                            override fun onEmulatorSet() {}
                            override fun logError(tag: String?, message: String?) {}
                            override fun logWarn(tag: String?, message: String?) {}
                            override fun logInfo(tag: String?, message: String?) {}
                            override fun logDebug(tag: String?, message: String?) {}
                            override fun logVerbose(tag: String?, message: String?) {}
                            override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
                            override fun logStackTrace(tag: String?, e: Exception?) {}
                        })

                        addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                                if (right - left > 0 && bottom - top > 0) {
                                    v?.removeOnLayoutChangeListener(this)
                                    v?.post { startShellSession() }
                                }
                            }
                        })
                    }
                    terminalView
                }
            )
        }
    }

    private fun startShellSession() {
        if (::terminalView.isInitialized && terminalSession == null) {
            val shell = "/system/bin/sh"
            val cwd = filesDir.absolutePath
            val args = arrayOf("-l")
            val env = arrayOf(
                "TERM=xterm-256color",
                "HOME=$cwd",
                "PATH=/sbin:/system/sbin:/system/bin:/system/xbin",
                "LANG=en_US.UTF-8",
                "LC_ALL=en_US.UTF-8"
            )
            terminalSession = TerminalSession(shell, cwd, args, env, 1000, sessionClient)
            terminalView.attachSession(terminalSession)
            
            val banner = "\n--- Nougaminal Terminal v0.00.1-PV ---\n\n"
            terminalSession?.write(banner)
            
            terminalView.requestFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalSession = null
    }
}
