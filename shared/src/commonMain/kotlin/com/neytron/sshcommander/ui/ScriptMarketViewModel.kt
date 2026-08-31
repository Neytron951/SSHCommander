package com.neytron.sshcommander.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScriptMarketViewModel(
    private val repository: ServerRepository,
    private val marketService: ScriptMarketService
) : ViewModel() {

    private val _scripts = MutableStateFlow<List<MarketScript>>(emptyList())
    
    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf<String?>(null)

    val filteredScripts: StateFlow<List<MarketScript>> = combine(
        snapshotFlow { searchQuery },
        snapshotFlow { selectedCategory },
        _scripts
    ) { query, cat, allScripts ->
        allScripts.filter { script ->
            val matchesSearch = query.isBlank() || 
                                script.name.contains(query, ignoreCase = true) || 
                                script.description.contains(query, ignoreCase = true)
            val matchesCategory = cat == null || script.category == cat
            matchesSearch && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = _scripts.map { list ->
        list.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = _scripts.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    var isLoading by mutableStateOf(false)
    var isInstalling by mutableStateOf<String?>(null) // Stores script ID being installed
    var error by mutableStateOf<String?>(null)

    private val installedScriptIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        loadScripts()
    }

    fun loadScripts() {
        viewModelScope.launch {
            isLoading = true
            error = null
            marketService.fetchScripts()
                .onSuccess { _scripts.value = it }
                .onFailure { error = it.message ?: "Failed to load scripts" }
            isLoading = false
        }
    }

    suspend fun installScript(script: MarketScript): Boolean {
        isInstalling = script.id
        return try {
            val command = CustomCommand(
                name = script.name,
                command = script.command.trim(),
                iconName = "default",
                colorHex = "#7AA2F7",
                orderIndex = 100,
                isDangerous = script.isDangerous,
                categoryName = script.category,
                variables = script.extractVariables()
            )
            repository.insertCustomCommand(command)
            installedScriptIds.value += script.id
            // Artificial delay for visual feedback of "Success"
            kotlinx.coroutines.delay(1200)
            true
        } catch (e: Exception) {
            error = "Installation failed: ${e.message}"
            false
        } finally {
            isInstalling = null
        }
    }

    fun isInstalled(scriptId: String): Boolean = installedScriptIds.value.contains(scriptId)
}
