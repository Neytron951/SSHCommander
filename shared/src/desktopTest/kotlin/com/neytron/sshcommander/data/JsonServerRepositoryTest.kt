package com.neytron.sshcommander.data

import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonServerRepositoryTest {

    private lateinit var dataDir: File

    @BeforeTest
    fun setUp() {
        dataDir = Files.createTempDirectory("sshcommander-repo-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        dataDir.deleteRecursively()
    }

    @Test
    fun `persists server across repository instances`() = runBlocking {
        val repo = JsonServerRepository(dataDir)
        assertTrue(repo.getServers().isEmpty(), "fresh storage should be empty")

        val id = repo.insertServer(
            Server(id = 0, name = "Test Server", host = "test.example.com", port = 2222, username = "root"),
            password = "secret"
        )
        assertEquals(1, id)
        assertEquals("secret", repo.getPassword(id))

        // Re-open = simulate app restart.
        val repo2 = JsonServerRepository(dataDir)
        val servers = repo2.getServers()
        assertEquals(1, servers.size)
        assertEquals("Test Server", servers[0].name)
        assertEquals("test.example.com", servers[0].host)
        assertEquals(2222, servers[0].port)
        assertEquals("secret", repo2.getPassword(id))
    }

    @Test
    fun `insert assigns sequential ids`() = runBlocking {
        val repo = JsonServerRepository(dataDir)
        val a = repo.insertServer(Server(0, "A", "a", 22, "u"), "p1")
        val b = repo.insertServer(Server(0, "B", "b", 22, "u"), "p2")
        assertEquals(1, a)
        assertEquals(2, b)
        assertEquals(2, repo.getServers().size)
    }

    @Test
    fun `update changes server and password`() = runBlocking {
        val repo = JsonServerRepository(dataDir)
        val id = repo.insertServer(Server(0, "Old", "old", 22, "u"), "oldpw")
        repo.updateServer(repo.getServers()[0].copy(name = "New"), password = "newpw")

        val servers = repo.getServers()
        assertEquals(1, servers.size)
        assertEquals("New", servers[0].name)
        assertEquals("newpw", repo.getPassword(id))

        // Password not provided → unchanged.
        repo.updateServer(servers[0].copy(name = "Newer"), password = null)
        assertEquals("newpw", repo.getPassword(id))
    }

    @Test
    fun `delete removes server and password`() = runBlocking {
        val repo = JsonServerRepository(dataDir)
        val a = repo.insertServer(Server(0, "A", "a", 22, "u"), "pa")
        repo.insertServer(Server(0, "B", "b", 22, "u"), "pb")
        repo.deleteServer(a)

        val servers = repo.getServers()
        assertEquals(1, servers.size)
        assertEquals("B", servers[0].name)
        assertNull(repo.getPassword(a))
    }

    @Test
    fun `folders persist and assign sequential ids`() = runBlocking {
        val repo = JsonServerRepository(dataDir)
        assertTrue(repo.getFolders().isEmpty())

        val a = repo.insertFolder("Prod")
        val b = repo.insertFolder("Dev")
        assertEquals(1, a)
        assertEquals(2, b)

        // Re-open = simulate app restart.
        val repo2 = JsonServerRepository(dataDir)
        val folders = repo2.getFolders()
        assertEquals(listOf("Prod", "Dev"), folders.map { it.name })
    }

    @Test
    fun `folder can be renamed`() = runBlocking {
        val repo = JsonServerRepository(dataDir)
        val id = repo.insertFolder("Old")
        repo.updateFolder(ServerFolder(id = id, name = "New"))

        assertEquals("New", repo.getFolders().single().name)
    }

    @Test
    fun `deleting a folder unfiles its servers`() = runBlocking {
        val repo = JsonServerRepository(dataDir)
        val folderId = repo.insertFolder("Prod")
        val serverId = repo.insertServer(
            Server(id = 0, name = "S", host = "h", port = 22, username = "u", folderId = folderId),
            "pw"
        )
        assertEquals(folderId, repo.getServers().single().folderId)

        repo.deleteFolder(folderId)
        assertTrue(repo.getFolders().isEmpty())
        assertNull(repo.getServers().single().folderId)
        assertEquals(serverId, repo.getServers().single().id)
    }
}
