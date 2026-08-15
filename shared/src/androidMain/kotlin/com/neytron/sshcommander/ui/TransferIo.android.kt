package com.neytron.sshcommander.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream
import java.io.OutputStream

private class AndroidInputStream(private val stream: InputStream) : PlatformInputStream {
    override fun read(buffer: ByteArray, off: Int, len: Int): Int = stream.read(buffer, off, len)
    override fun close() = stream.close()
}

private class AndroidOutputStream(private val stream: OutputStream) : PlatformOutputStream {
    override fun write(buffer: ByteArray, off: Int, len: Int) = stream.write(buffer, off, len)
    override fun close() = stream.close()
}

private class AndroidTransferFile(
    private val context: Context,
    private val uri: Uri
) : PlatformTransferFile {
    override val name: String
        get() = uri.lastPathSegment ?: "file"
    override val size: Long
        get() = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            it.length
        } ?: 0L
    override fun openInput(): PlatformInputStream? =
        context.contentResolver.openInputStream(uri)?.let { AndroidInputStream(it) }
    override fun openOutput(): PlatformOutputStream? =
        context.contentResolver.openOutputStream(uri)?.let { AndroidOutputStream(it) }
}

private class AndroidTransferDir(
    private val context: Context,
    private val treeUri: Uri
) : PlatformTransferDir {
    override fun createFile(name: String): PlatformTransferFile? {
        val docFile = android.provider.DocumentsContract.buildDocumentUriUsingTree(
            treeUri, android.provider.DocumentsContract.getTreeDocumentId(treeUri)
        )
        val created = try {
            context.contentResolver.insert(
                docFile.buildUpon().appendPath(name).build(),
                android.content.ContentValues().apply {
                    put(android.provider.OpenableColumns.DISPLAY_NAME, name)
                    put("mime_type", "application/octet-stream")
                }
            )
        } catch (e: Exception) { null }
        return created?.let { AndroidTransferFile(context, it) }
    }
}

@Composable
actual fun rememberUploadPicker(onPicked: (List<PlatformTransferFile>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        onPicked(uris.map { AndroidTransferFile(context, it) })
    }
    return { launcher.launch(arrayOf("*/*")) }
}

@Composable
actual fun rememberSavePicker(onPicked: (PlatformTransferFile?) -> Unit): (String) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        onPicked(uri?.let { AndroidTransferFile(context, it) })
    }
    return { defaultName -> launcher.launch(defaultName) }
}

@Composable
actual fun rememberDirectoryPicker(onPicked: (PlatformTransferDir?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        onPicked(uri?.let { AndroidTransferDir(context, it) })
    }
    return { launcher.launch(null) }
}

