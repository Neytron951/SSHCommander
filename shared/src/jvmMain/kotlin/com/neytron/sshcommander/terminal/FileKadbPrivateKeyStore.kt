package com.neytron.sshcommander.terminal

import com.flyfishxu.kadb.cert.KadbPrivateKeyStore
import java.io.File

class FileKadbPrivateKeyStore(private val file: File) : KadbPrivateKeyStore {
    override fun readPrivateKeyPem(): ByteArray? {
        return try {
            if (file.exists() && file.isFile) file.readBytes() else null
        } catch (e: Exception) {
            null
        }
    }

    override fun writePrivateKeyPemAtomic(privateKeyPem: ByteArray) {
        try {
            file.parentFile?.mkdirs()
            val tempFile = File(file.parent, "${file.name}.tmp")
            tempFile.writeBytes(privateKeyPem)
            if (file.exists()) {
                file.delete()
            }
            if (!tempFile.renameTo(file)) {
                file.writeBytes(privateKeyPem)
                tempFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun clear() {
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
