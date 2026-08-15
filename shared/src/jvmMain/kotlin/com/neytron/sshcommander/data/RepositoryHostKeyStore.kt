package com.neytron.sshcommander.data

/**
 * Persists captured host-key fingerprints through the [ServerRepository],
 * so [SshConnectionManager] can verify servers on subsequent connects.
 */
class RepositoryHostKeyStore(
    private val repository: ServerRepository
) : HostKeyStore {
    override suspend fun storeHostKey(serverId: Int, hostKey: String, hostKeyType: String) {
        val server = repository.getServerById(serverId) ?: return
        repository.updateServer(
            server.copy(hostKey = hostKey, hostKeyType = hostKeyType),
            password = null
        )
    }
}
