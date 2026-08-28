package com.neytron.sshcommander.sync

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GoogleDriveFile(val id: String, val name: String, val mimeType: String? = null)

@Serializable
data class GoogleDriveFileList(val files: List<GoogleDriveFile>)

class GoogleDriveService(
    private val httpClient: HttpClient,
    private val getAccessToken: suspend () -> String?
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val fileName = "ssh_commander_backup.enc"

    /**
     * Finds the backup file in the appDataFolder scope.
     */
    suspend fun findBackupFile(): String? {
        val token = getAccessToken() ?: return null
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("spaces", "appDataFolder")
            parameter("q", "name = '$fileName' and trashed = false")
            parameter("fields", "files(id, name)")
        }
        
        if (response.status == HttpStatusCode.OK) {
            val list = response.body<GoogleDriveFileList>()
            return list.files.firstOrNull()?.id
        }
        return null
    }

    /**
     * Uploads or updates the backup file.
     */
    suspend fun uploadBackup(content: String): Boolean {
        val token = getAccessToken() ?: return false
        val fileId = findBackupFile()
        
        return if (fileId == null) {
            createNewFile(token, content)
        } else {
            updateFile(token, fileId, content)
        }
    }

    private suspend fun createNewFile(token: String, content: String): Boolean {
        val metadata = """{"name": "$fileName", "parents": ["appDataFolder"]}"""
        
        val response = httpClient.post("https://www.googleapis.com/upload/drive/v3/files") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("uploadType", "multipart")
            
            // Proper multipart request for Google Drive
            val boundary = "foo_bar_baz"
            contentType(ContentType.parse("multipart/related; boundary=$boundary"))
            setBody(
                "--$boundary\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                "$metadata\r\n" +
                "--$boundary\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n" +
                "$content\r\n" +
                "--$boundary--"
            )
        }
        return response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created
    }

    private suspend fun updateFile(token: String, fileId: String, content: String): Boolean {
        val response = httpClient.patch("https://www.googleapis.com/upload/drive/v3/files/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("uploadType", "media")
            setBody(content)
        }
        return response.status == HttpStatusCode.OK
    }

    /**
     * Downloads the backup file content.
     */
    suspend fun downloadBackup(): String? {
        val token = getAccessToken() ?: return null
        val fileId = findBackupFile() ?: return null
        
        val response = httpClient.get("https://www.googleapis.com/drive/v3/files/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("alt", "media")
        }
        
        return if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
    }
}
