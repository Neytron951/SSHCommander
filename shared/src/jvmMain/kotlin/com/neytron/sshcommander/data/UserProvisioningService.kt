package com.neytron.sshcommander.data

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserProvisioningService {

    suspend fun provisionUser(
        session: Session,
        username: String,
        publicKey: String,
        password: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!session.isConnected) {
                return@withContext Result.failure(Exception("SSH Session is not connected"))
            }

            // 1. Create user if not exists
            // We use id command to check for existence and useradd only if needed.
            val createUserCmd = """
                if ! id "$username" >/dev/null 2>&1; then
                    sudo useradd -m -s /bin/bash "$username"
                fi
            """.trimIndent()
            executeCommand(session, createUserCmd)

            // 2. Set password if provided
            password?.let {
                val setPwCmd = "echo \"$username:$it\" | sudo chpasswd"
                executeCommand(session, setPwCmd)
            }

            // 3. Setup SSH directory (SOFT APPEND)
            val homeDir = "/home/$username"
            val setupSshCmd = """
                sudo mkdir -p $homeDir/.ssh
                sudo chmod 700 $homeDir/.ssh
                sudo touch $homeDir/.ssh/authorized_keys
                sudo chmod 600 $homeDir/.ssh/authorized_keys
                
                # Append key only if it doesn't already exist in the file
                if ! sudo grep -qF "$publicKey" $homeDir/.ssh/authorized_keys; then
                    echo "$publicKey" | sudo tee -a $homeDir/.ssh/authorized_keys > /dev/null
                fi
                
                sudo chown -R $username:$username $homeDir/.ssh
            """.trimIndent()
            
            executeCommand(session, setupSshCmd)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeCommand(session: Session, command: String): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(cmdWrapper(command))
        val errorStream = channel.errStream
        val inputStream = channel.inputStream
        
        channel.connect()
        
        val output = inputStream.bufferedReader().readText()
        val error = errorStream.bufferedReader().readText()
        
        val exitStatus = channel.exitStatus
        channel.disconnect()

        if (exitStatus != 0 && exitStatus != -1) {
            throw Exception("Command failed with exit code $exitStatus: $error")
        }
        
        return output
    }

    private fun cmdWrapper(cmd: String): String {
        // We use a subshell to execute multiple lines if needed
        return "bash -c '${cmd.replace("'", "'\\''")}'"
    }
}
