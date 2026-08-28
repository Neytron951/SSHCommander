package com.neytron.sshcommander.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.data.SshKey
import kotlinx.coroutines.launch

class SshKeyManagerViewModel(private val repository: ServerRepository) : ViewModel() {
    val sshKeys = mutableStateListOf<SshKey>()
    var isLoading by mutableStateOf(false)

    init {
        loadKeys()
    }

    private fun loadKeys() {
        viewModelScope.launch {
            isLoading = true
            repository.allSshKeys.collect { list ->
                sshKeys.clear()
                sshKeys.addAll(list)
                isLoading = false
            }
        }
    }

    fun deleteKey(id: Int) {
        viewModelScope.launch {
            repository.deleteSshKey(id)
        }
    }

    fun addKey(key: SshKey) {
        viewModelScope.launch {
            repository.insertSshKey(key)
        }
    }
}
