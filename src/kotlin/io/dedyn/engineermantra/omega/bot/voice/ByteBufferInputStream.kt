package io.dedyn.engineermantra.omega.bot.voice

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.min

//From: https://stackoverflow.com/a/6603018
//Converted to Kotlin on 2023-10-15, IDEA 2023.2.3, Kotlin 1.9.10
class ByteBufferInputStream(var buf: ByteBuffer) : InputStream() {
    @Throws(IOException::class)
    override fun read(): Int {
        return if (!buf.hasRemaining()) {
            -1
        } else buf.get().toInt() and 0xFF
    }

    @Throws(IOException::class)
    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        var len = len
        if (!buf.hasRemaining()) {
            return -1
        }
        len = min(len.toDouble(), buf.remaining().toDouble()).toInt()
        buf[bytes, off, len]
        return len
    }
}
