package io.github.m_vollan.omega.shared

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Paths

object Utils {
    fun readFile(filePath: String): InputStream{
        return FileInputStream(File(filePath))
    }
    fun getRunningDir(): String{
        return Paths.get("").toAbsolutePath().toString()
    }
}