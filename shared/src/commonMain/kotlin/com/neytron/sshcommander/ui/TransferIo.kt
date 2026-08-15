package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable

/**
 * Platform-neutral byte streams so SFTP transfers work on Android (SAF) and
 * Desktop (java.io) without pulling android.* into the shared view models.
 */
interface PlatformInputStream {
    fun read(buffer: ByteArray, off: Int, len: Int): Int
    fun close()
}

interface PlatformOutputStream {
    fun write(buffer: ByteArray, off: Int, len: Int)
    fun close()
}

/** A local file chosen by the user (upload source or download target). */
interface PlatformTransferFile {
    val name: String
    val size: Long
    fun openInput(): PlatformInputStream?
    fun openOutput(): PlatformOutputStream?
}

/** A local directory chosen by the user (used for multi-download). */
interface PlatformTransferDir {
    fun createFile(name: String): PlatformTransferFile?
}

/** Launcher that opens a multi-file picker (SAF on Android, AWT on desktop). */
@Composable
expect fun rememberUploadPicker(onPicked: (List<PlatformTransferFile>) -> Unit): () -> Unit

/** Launcher that asks where to save a single file (CreateDocument / save dialog). */
@Composable
expect fun rememberSavePicker(onPicked: (PlatformTransferFile?) -> Unit): (String) -> Unit

/** Launcher that asks for a directory (tree picker). */
@Composable
expect fun rememberDirectoryPicker(onPicked: (PlatformTransferDir?) -> Unit): () -> Unit
