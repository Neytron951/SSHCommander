package com.neytron.sshcommander.ui

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Opens a native file chooser (AWT FileDialog — light, modal, no extra deps)
 * and invokes [onFileSelected] with the selected path, or no-op on cancel.
 */
internal actual fun uploadFile(onFileSelected: (String) -> Unit) {
    val dialog = FileDialog(Frame(), "Select file to upload", FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory
    val file = dialog.file
    if (dir != null && file != null) {
        onFileSelected(File(dir, file).absolutePath)
    }
}

