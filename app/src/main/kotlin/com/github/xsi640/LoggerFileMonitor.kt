package com.github.xsi640

import java.io.File
import java.io.RandomAccessFile
import java.lang.Thread.sleep
import kotlin.math.log

class LoggerFileMonitor(
    val logFile: File,
    val newLineAction: (line: String) -> Unit
) {
    private var raf: RandomAccessFile? = null

    fun start() {
        if (raf != null)
            return
        while (!logFile.exists()) {
            sleep(1)
        }
        raf = RandomAccessFile(logFile, "r")
        var len = 0L
        while (true) {
            val size = raf!!.length()
            if (size > len) {
                raf!!.seek(len)
                var line = raf!!.readLine()
                while (line != null) {
                    newLineAction(line)
                    line = raf!!.readLine()
                }
                len = size
            }
        }
    }
}