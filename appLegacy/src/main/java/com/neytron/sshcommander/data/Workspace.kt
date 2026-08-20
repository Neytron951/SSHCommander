package com.neytron.sshcommander.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class Workspace(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String? = null,
    val items: List<WorkspaceItem> = emptyList()
)

data class WorkspaceItem(
    val serverId: Int,
    val loginId: Int? = null,
    val type: WorkspaceItemType = WorkspaceItemType.TERMINAL,
    val isPinned: Boolean = false,
    val tabColorHex: String? = null
)

enum class WorkspaceItemType {
    TERMINAL,
    SFTP,
    DASHBOARD
}
