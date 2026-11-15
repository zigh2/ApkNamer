package io.github.apknamer.data.parse

import java.io.File
import java.io.IOException

internal object ZipMagic {
    /** True if the file starts with the local-file-header signature `PK\u0003\u0004`. */
    fun hasZipHeader(file: File): Boolean =
        try {
            file.inputStream().use { input ->
                val b = ByteArray(4)
                input.read(b) == 4 &&
                    b[0] == 0x50.toByte() && b[1] == 0x4B.toByte() &&
                    b[2] == 0x03.toByte() && b[3] == 0x04.toByte()
            }
        } catch (_: IOException) {
            false
        }
}
