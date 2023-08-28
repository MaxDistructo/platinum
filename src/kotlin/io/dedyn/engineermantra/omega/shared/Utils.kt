package io.dedyn.engineermantra.omega.shared

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths
import kotlin.math.pow

object Utils {
    fun readFile(filePath: String): InputStream{
        return FileInputStream(File(filePath))
    }
    fun getRunningDir(): String{
        return Paths.get("").toAbsolutePath().toString()
    }

    fun writeFile(data: String, filePath: String) {
        val outFile = FileOutputStream(File(filePath), false)
        outFile.write(data.toByteArray())
    }
    fun calculateLevel(n: Double): Int{
        //We implement the Medium Slow leveling curve from Pokémon here.
        //This leveling is fairly good since users are able to level up fast early, but it slows down as they get up
        //in level.
        //We calculate the double of the level then output an Int
        return (6/5 * n.pow(3) - 15 * n.pow(2) + 100 * n - 140).toInt();
    }
}