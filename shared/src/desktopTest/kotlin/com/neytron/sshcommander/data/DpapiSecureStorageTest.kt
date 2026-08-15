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

class DpapiSecureStorageTest {

    private lateinit var file: File

    @BeforeTest
    fun setUp() {
        file = File(Files.createTempDirectory("sshcommander-dpapi-test").toFile(), "secrets.dpapi")
    }

    @AfterTest
    fun tearDown() {
        file.parentFile.deleteRecursively()
    }

    @Test
    fun `round-trips value and survives reopening`() = runBlocking {
        val storage = DpapiSecureStorage(file)
        assertTrue(storage.put("server-1", "hunter2"))

        val storage2 = DpapiSecureStorage(file)
        assertEquals("hunter2", storage2.get("server-1"))
    }

    @Test
    fun `returns null for missing key and removes entries`() = runBlocking {
        val storage = DpapiSecureStorage(file)
        assertNull(storage.get("missing"))
        storage.put("k", "v")
        storage.remove("k")
        assertNull(storage.get("k"))
    }

    @Test
    fun `stores nothing in plaintext`() = runBlocking {
        val storage = DpapiSecureStorage(file)
        storage.put("server-7", "supersecret")
        val raw = file.readText()
        assertTrue(raw.contains("supersecret").not(), "raw file must not contain the plaintext value")
        assertEquals("supersecret", storage.get("server-7"))
    }
}
