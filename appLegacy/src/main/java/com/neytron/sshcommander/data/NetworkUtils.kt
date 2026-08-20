package com.neytron.sshcommander.data

import java.net.InetSocketAddress
import java.net.Socket

/**
 * Lightweight network helpers used for status checks.
 *
 * TCP checks are preferred over ICMP because Android often blocks ICMP
 * (InetAddress.isReachable) for apps, so a raw TCP connect to the SSH port
 * reports the server status far more reliably.
 */
object NetworkUtils {

    /**
     * Returns true if a TCP connection to [host]:[port] can be established
     * within [timeoutMs]. The socket is closed immediately after connecting.
     */
    fun isPortOpen(host: String, port: Int, timeoutMs: Int = 2000): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
