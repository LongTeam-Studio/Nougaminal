package com.example.termux.terminal
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.connectbot.terminal.Terminal
import org.connectbot.terminal.TerminalEmulatorFactory
import kotlinx.coroutines.flow.collectLatest
@Composable
fun TerminalScreen(pty: PtyProcess, modifier: Modifier = Modifier) {
    val emulator = remember(pty) {
        TerminalEmulatorFactory.create { data, offset, length ->
            pty.write(data.copyOfRange(offset, offset + length))
        }
    }
    LaunchedEffect(pty, emulator) {
        pty.output.collectLatest { bytes -> emulator.writeInput(bytes, 0, bytes.size) }
    }
    Terminal(terminalEmulator = emulator, modifier = modifier, keyboardEnabled = true)
}
