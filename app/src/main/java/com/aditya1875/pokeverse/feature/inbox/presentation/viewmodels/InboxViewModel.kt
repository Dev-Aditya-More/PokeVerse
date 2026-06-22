package com.aditya1875.pokeverse.feature.inbox.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.inbox.data.model.UserMessage
import com.aditya1875.pokeverse.feature.inbox.data.repository.InboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InboxViewModel(
    private val repository: InboxRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<UserMessage>>(emptyList())
    val messages: StateFlow<List<UserMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val msgs = repository.getMessages()
                _messages.value = msgs
                _unreadCount.value = msgs.count { !it.isRead }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(messageId: String) {
        viewModelScope.launch {
            repository.markAsRead(messageId)
            _messages.value = _messages.value.map {
                if (it.id == messageId) it.copy(isRead = true) else it
            }
            _unreadCount.value = _messages.value.count { !it.isRead }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
            _messages.value = _messages.value.map { it.copy(isRead = true) }
            _unreadCount.value = 0
        }
    }

}
