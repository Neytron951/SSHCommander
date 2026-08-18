package com.neytron.sshcommander.ui

/**
 * Android placeholder: file upload via SAF arrives in a later phase.
 * For now this silently cancels (no-op).
 */
internal actual fun uploadFile(onFileSelected: (String) -> Unit) {
    // TODO(phase 5): ActivityResultContracts.GetContent → SAF
}

/**
 * Android placeholder: download destination via SAF arrives in a later phase.
 * For now this silently cancels (no-op) — downloads continue to use the
 * controller's default destination.
 */
internal actual fun downloadFolder(onFolderSelected: (String) -> Unit) {
    // TODO(phase 5): ActivityResultContracts.OpenDocumentTree → SAF
}
