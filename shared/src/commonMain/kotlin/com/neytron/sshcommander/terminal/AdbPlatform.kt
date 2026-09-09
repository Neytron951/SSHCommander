package com.neytron.sshcommander.terminal

import com.neytron.sshcommander.data.DiscoveredDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Platform-specific ADB operations. */
expect object AdbPlatform {
    /** 
     * Pairs with an Android device using the 6-digit pairing code.
     * The host and port should be the ones shown in the "Pair device with pairing code" dialog.
     */
    suspend fun pair(host: String, port: Int, code: String): Result<Unit>

    fun startPairingServer(name: String, password: String, onDevicePaired: () -> Unit)

    fun stopPairingServer()

    /** Checks if the ADB binary is available on the system. */
    fun isAdbAvailable(): Boolean

    /** 
     * Starts downloading the ADB binary for the current platform.
     */
    fun startDownload()

    /** 
     * Progress of the current download (0.0 to 1.0).
     */
    fun getDownloadProgress(): StateFlow<Float?>

    /**
     * Scans for ADB devices on the network using mDNS.
     */
    fun scanDevices(): Flow<List<DiscoveredDevice>>
}
