package com.neytron.sshcommander.terminal

import dadb.AdbKeyPair
import java.io.File

object AdbKeyUtils {
    fun loadSystemKeyPair(): AdbKeyPair? {
        return try {
            AdbKeyPair.readDefault()
        } catch (e: Exception) {
            null
        }
    }
}
