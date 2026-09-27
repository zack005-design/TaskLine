package com.example.taskfoundation.data.backup

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream

class BackupFiles(private val resolver: ContentResolver) {
    fun read(uri: Uri): TaskBackup {
        val input = checkNotNull(resolver.openInputStream(uri)) { "Unable to open backup." }
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count == -1) break
                require(output.size() + count <= BackupJson.MAX_BYTES) { "Backup exceeds the 20 MB limit." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        return BackupJson.decode(bytes.toString(Charsets.UTF_8))
    }

    fun write(uri: Uri, backup: TaskBackup) {
        val bytes = BackupJson.encode(backup).toByteArray(Charsets.UTF_8)
        require(bytes.size <= BackupJson.MAX_BYTES) { "Backup exceeds the 20 MB limit." }
        checkNotNull(resolver.openOutputStream(uri, "wt")) { "Unable to create backup." }.use {
            it.write(bytes)
            it.flush()
        }
    }
}
