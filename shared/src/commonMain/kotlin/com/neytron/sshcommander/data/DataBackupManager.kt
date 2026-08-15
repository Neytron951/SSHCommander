package com.neytron.sshcommander.data

/**
 * JSON backup/restore of all app data (servers, commands, logins).
 * Implemented by the JVM [ExportImportManager] and used from the shared UI
 * so the desktop File menu can export/import without depending on Gson.
 */
interface DataBackupManager {
    suspend fun exportJson(): String
    suspend fun importJson(json: String)
}
