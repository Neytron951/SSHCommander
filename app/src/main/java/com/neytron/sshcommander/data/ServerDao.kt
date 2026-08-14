package com.neytron.sshcommander.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers")
    fun getAllServers(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE showInWidget = 1")
    fun getWidgetServers(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: Int): Server?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: Server): Long

    @Update
    suspend fun updateServer(server: Server)

    @Delete
    suspend fun deleteServer(server: Server)

    // Custom Commands
    @Query("SELECT * FROM custom_commands ORDER BY orderIndex ASC")
    fun getAllCustomCommands(): Flow<List<CustomCommand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCommand(command: CustomCommand)

    @Update
    suspend fun updateCustomCommand(command: CustomCommand)

    @Delete
    suspend fun deleteCustomCommand(command: CustomCommand)

    // Command History
    @Query("SELECT * FROM command_history WHERE serverId = :serverId ORDER BY timestamp DESC")
    fun getHistoryForServer(serverId: Int): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CommandHistoryEntity): Long

    @Query("UPDATE command_history SET output = :output WHERE id = :id")
    suspend fun updateHistoryOutput(id: Long, output: String)

    @Query("DELETE FROM command_history WHERE serverId = :serverId")
    suspend fun clearHistoryForServer(serverId: Int)

    // Server Logins
    @Query("SELECT * FROM server_logins WHERE serverId = :serverId ORDER BY id ASC")
    fun getLoginsForServer(serverId: Int): Flow<List<ServerLogin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogin(login: ServerLogin): Long

    @Update
    suspend fun updateLogin(login: ServerLogin)

    @Delete
    suspend fun deleteLogin(login: ServerLogin)

    @Query("DELETE FROM server_logins WHERE serverId = :serverId")
    suspend fun deleteLoginsForServer(serverId: Int)

    @Query("UPDATE servers SET lastSftpPath = :path WHERE id = :serverId")
    suspend fun updateLastSftpPath(serverId: Int, path: String)

    @Query("UPDATE server_logins SET lastSftpPath = :path WHERE id = :loginId")
    suspend fun updateLoginLastSftpPath(loginId: Int, path: String)
}
