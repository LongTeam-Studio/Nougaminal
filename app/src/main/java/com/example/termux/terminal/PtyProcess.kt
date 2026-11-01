package com.example.termux.terminal
import android.os.ParcelFileDescriptor
import com.termux.terminal.JNI
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
class PtyProcess(shell: String, cwd: String, env: Array<String>, private val scope: CoroutineScope) {
    private val writeFd: Int
    val output = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    init {
        val pidOut = IntArray(1); val fdOut = IntArray(1)
        writeFd = JNI.createSubprocess(shell, cwd, arrayOf("-l"), env, pidOut, 24, 80, 8, 16, fdOut)
        scope.launch(Dispatchers.IO) {
            val fis = ParcelFileDescriptor.adoptFd(fdOut[0]).fileInputStream
            val buf = ByteArray(8192)
            while (isActive) {
                val n = fis.read(buf); if (n <= 0) break
                output.emit(buf.copyOf(n))
            }
        }
    }
    fun write(data: ByteArray) {
        ParcelFileDescriptor.adoptFd(writeFd).fileOutputStream.use { it.write(data) }
    }
}
