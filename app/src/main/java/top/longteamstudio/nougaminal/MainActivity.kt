package top.longteamstudio.nougaminal

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

class MainActivity : ComponentActivity() {
    private lateinit var terminalView: TerminalView
    private var terminalSession: TerminalSession? = null

    private val sessionClient = object : TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) {
            runOnUiThread { if (::terminalView.isInitialized) terminalView.invalidate() }
        }
        override fun onTitleChanged(changedSession: TerminalSession) {}
        override fun onSessionFinished(finishedSession: TerminalSession) {}
        override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
            if (!text.isNullOrEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Nougaminal", text))
            }
        }
        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (!text.isNullOrEmpty() && session != null) session.write(text)
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
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    AndroidView(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        factory = { context ->
                            terminalView = TerminalView(context, null).apply {
                                isFocusable = true
                                isFocusableInTouchMode = true
                                setTextSize(18)
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
                    ExtraKeysBar(
                        onKey = { keyString -> terminalSession?.write(keyString) },
                        onPaste = { sessionClient.onPasteTextFromClipboard(terminalSession) }
                    )
                }
            }
        }
    }

    private fun startShellSession() {
        if (::terminalView.isInitialized && terminalSession == null) {
            val env = arrayOf(
                "TERM=xterm-256color",
                "HOME=${filesDir.absolutePath}",
                "PATH=/sbin:/system/sbin:/system/bin:/system/xbin",
                "LANG=en_US.UTF-8"
            )
            terminalSession = TerminalSession("/system/bin/sh", filesDir.absolutePath, arrayOf("-l"), env, 2000, sessionClient)
            terminalView.attachSession(terminalSession)
            terminalSession?.write("\n--- Nougaminal v0.0.4-PV ---\n\n")
            terminalView.requestFocus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalSession = null
    }
}

@Composable
fun ExtraKeysBar(onKey: (String) -> Unit, onPaste: () -> Unit) {
    val keys = listOf(
        "ESC" to "\u001b", "TAB" to "\t", "CTRL" to "\u0003", "|" to "|", "-" to "-", "/" to "/",
        "←" to "\u001b[D", "↑" to "\u001b[A", "↓" to "\u001b[B", "→" to "\u001b[C"
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF222222)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            TextButton(onClick = onPaste, modifier = Modifier.height(36.dp)) {
                Text("PASTE", color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        items(keys) { (label, value) ->
            TextButton(onClick = { onKey(value) }, modifier = Modifier.height(36.dp)) {
                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}