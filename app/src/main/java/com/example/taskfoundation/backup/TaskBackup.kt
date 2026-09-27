package com.example.taskfoundation.backup

import com.example.taskfoundation.data.backup.BackupJson
import com.example.taskfoundation.data.backup.BackupRepository
import com.example.taskfoundation.data.local.TaskDatabase

// Compatibility facade: every backup entry point uses the same validated format and transaction.
typealias BackupSnapshot = com.example.taskfoundation.data.backup.TaskBackup

class TaskBackup(database: TaskDatabase) {
    private val repository = BackupRepository(database)
    suspend fun export(): String = BackupJson.encode(repository.snapshot())
    suspend fun restore(snapshot: BackupSnapshot) = repository.restore(snapshot)
    companion object {
        fun encode(snapshot: BackupSnapshot): String = BackupJson.encode(snapshot)
        fun decode(text: String): BackupSnapshot = BackupJson.decode(text)
        fun validate(snapshot: BackupSnapshot) = snapshot.validate()
    }
}
