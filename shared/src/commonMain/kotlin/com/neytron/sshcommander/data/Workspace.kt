package com.neytron.sshcommander.data

/**
 * A group of sessions that can be opened together.
 */
data class Workspace(
    val id: Int = 0,
    val name: String,
    val colorHex: String? = null,
    val items: List<WorkspaceItem> = emptyList()
)

data class WorkspaceItem(
    val serverId: Int,
    val loginId: Int? = null,
    val type: WorkspaceItemType = WorkspaceItemType.TERMINAL,
    val isPinned: Boolean = false,
    val tabColorHex: String? = null,
    val initialPath: String? = null,
    val lastCommand: String? = null
)

enum class WorkspaceItemType {
    TERMINAL,
    SFTP,
    DASHBOARD
}
