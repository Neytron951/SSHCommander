package com.neytron.sshcommander.ui

/**
 * Android placeholder: file upload via SAF arrives in a later phase.
 * For now this silently cancels (no-op).
 */
internal actual fun uploadFile(onFileSelected: (String) -> Unit) {
    // TODO(phase 5): ActivityResultContracts.GetContent → SAF
}
