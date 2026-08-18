package com.neytron.sshcommander.ui

import androidx.compose.ui.Modifier

/**
 * Applies a platform-appropriate resize cursor while the pointer hovers a
 * draggable divider. Desktop shows the OS "left-right resize" cursor so users
 * see the strip is grabbable; Android has no hover cursor and does nothing.
 */
expect fun Modifier.resizeHoverCursor(): Modifier
