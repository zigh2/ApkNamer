package io.github.apknamer.data.storage

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/** Converts a folder picked with the system picker into a real path (needs all-files access). */
object TreeUriPath {
    fun toFile(treeUri: Uri): File? {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
        val parts = documentId.split(":", limit = 2)
        val volume = parts[0]
        val relative = parts.getOrElse(1) { "" }
        val base = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory()
        } else {
            File("/storage/$volume")
        }
        return if (relative.isEmpty()) base else File(base, relative)
    }
}
