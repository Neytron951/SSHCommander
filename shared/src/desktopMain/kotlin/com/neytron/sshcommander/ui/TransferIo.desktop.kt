package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

private class DesktopInputStream(private val stream: InputStream) : PlatformInputStream {
    override fun read(buffer: ByteArray, off: Int, len: Int): Int = stream.read(buffer, off, len)
    override fun close() = stream.close()
}

private class DesktopOutputStream(private val stream: OutputStream) : PlatformOutputStream {
    override fun write(buffer: ByteArray, off: Int, len: Int) = stream.write(buffer, off, len)
    override fun close() = stream.close()
}

private class DesktopTransferFile(private val file: File) : PlatformTransferFile {
    override val name: String get() = file.name
    override val size: Long get() = file.length()
    override fun openInput(): PlatformInputStream? =
        if (file.isFile) DesktopInputStream(FileInputStream(file)) else null
    override fun openOutput(): PlatformOutputStream? =
        DesktopOutputStream(FileOutputStream(file))
}

private class DesktopTransferDir(private val dir: File) : PlatformTransferDir {
    override fun createFile(name: String): PlatformTransferFile? {
        val f = File(dir, name)
        return try {
            f.parentFile?.mkdirs()
            f.createNewFile()
            DesktopTransferFile(f)
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
actual fun rememberUploadPicker(onPicked: (List<PlatformTransferFile>) -> Unit): () -> Unit = {
    val dialog = FileDialog(Frame(), "Select files to upload", FileDialog.LOAD)
    dialog.isMultipleMode = true
    dialog.isVisible = true
    val files = dialog.files
    if (files != null && files.isNotEmpty()) {
        onPicked(files.map { DesktopTransferFile(it) })
    }
}

@Composable
actual fun rememberSavePicker(onPicked: (PlatformTransferFile?) -> Unit): (String) -> Unit = { defaultName ->
    val dialog = FileDialog(Frame(), "Save file", FileDialog.SAVE)
    dialog.file = defaultName
    dialog.isVisible = true
    val dir = dialog.directory
    val name = dialog.file
    if (dir != null && name != null) {
        onPicked(DesktopTransferFile(File(dir, name)))
    } else {
        onPicked(null)
    }
}

@Composable
actual fun rememberDirectoryPicker(onPicked: (PlatformTransferDir?) -> Unit): () -> Unit = {
    val chooser = javax.swing.JFileChooser()
    chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
    chooser.dialogTitle = "Select download folder"
    if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        onPicked(DesktopTransferDir(chooser.selectedFile))
    } else {
        onPicked(null)
    }
}
