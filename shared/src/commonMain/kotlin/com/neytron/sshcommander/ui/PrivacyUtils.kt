package com.neytron.sshcommander.ui

/**
 * Hides parts of a host/IP so the full address is not visible when
 * anonymity (privacy) mode is enabled.
 */
object PrivacyUtils {

    fun maskHost(host: String): String {
        if (host.isBlank()) return host

        // IPv4 address
        val ipv4 = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")
        val match = ipv4.find(host)
        if (match != null) {
            val (a, b, c, d) = match.destructured
            return "$a.${maskSegment(b)}.${maskSegment(c)}.$d"
        }

        // IPv6 / hostname / anything else: keep the last two characters of the
        // last label, mask the rest.
        val parts = host.split('.')
        if (parts.size >= 2) {
            val last = parts.last()
            val maskedMiddle = parts.dropLast(1).joinToString(".") { maskSegment(it) }
            return "$maskedMiddle.$last"
        }

        return if (host.length <= 4) host else host.takeLast(4).let { last ->
            "*".repeat(host.length - last.length) + last
        }
    }

    private fun maskSegment(segment: String): String =
        if (segment.length <= 1) "*" else "*".repeat(segment.length - 1) + segment.takeLast(1)
}
