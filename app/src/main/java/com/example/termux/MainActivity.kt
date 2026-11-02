package com.example.termux

import android.content.ClipData
import android.content.ClipboardManager
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
        override fun onTextChanged(changedSession: TerminalSession) {}
        override fun onTitleChanged(changedSession: TerminalSession) {}
        override fun onSessionFinished(finishedSession: TerminalSession) {}
        
        // 【修复】真正实现复制功能
        override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
            if (!text.isNullOrEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", text))
            }
        }
        
        // 【修复】真正实现粘贴功能
        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrEmpty() && session != null) {
                    session.write(text)
                }
            }
        }
        
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
                // 【修复】去掉了 imePadding，解决盲打问题
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    terminalView = TerminalView(context, null).apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        
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
                            
                            // 这里返回 false，让 TerminalView 自己去处理长按选词，不然我们就把系统的长按菜单拦截掉了
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
            terminalView.setTextSize(20)
            
            val shell = "/system/bin/sh"
            val cwd = filesDir.absolutePath
            val args = arrayOf("-l")
            val env = arrayOf(
                "TERM=xterm-256color",
                "HOME=$cwd",
                "PATH=/sbin:/system/sbin:/system/bin:/system/xbin"
                // LANG 等环境变量会在下一版加上，目前纯英文没影响
            )
            terminalSession = TerminalSession(shell, cwd, args, env, 1000, sessionClient)
            terminalView.attachSession(terminalSession)
            terminalView.requestFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalSession = null
    }
}
