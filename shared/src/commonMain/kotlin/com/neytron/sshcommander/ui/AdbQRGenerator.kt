package com.neytron.sshcommander.ui

import kotlin.random.Random

object AdbQRGenerator {
    fun generateRandomService(): String {
        val id = Random.nextInt(1000, 9999)
        return "SSHCommander-$id"
    }

    fun generateRandomPassword(): String {
        return Random.nextInt(100000, 999999).toString()
    }
    
    fun formatAdbPayload(serviceName: String, password: String): String {
        return "WIFI:T:ADB;S:$serviceName;P:$password;;"
    }
}
