package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable

/**
 * Platform back navigation handler.
 * - Android: uses the system back dispatcher.
 * - Desktop: no-op (Esc/arrow keys handled elsewhere).
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/**
 * Platform ad banner.
 * - Android: renders the Yandex native ad banner.
 * - Desktop: renders nothing.
 */
@Composable
expect fun PlatformAdBanner(blockId: String)

/**
 * Whether the current layout is landscape orientation.
 * - Android: from LocalConfiguration.
 * - Desktop: width > height.
 */
@Composable
expect fun isLandscapeLayout(): Boolean

/** Shows a transient platform message (Toast on Android, console on desktop). */
expect fun platformToast(message: String)
