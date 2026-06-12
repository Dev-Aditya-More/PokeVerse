package com.aditya1875.pokeverse.feature.inbox.data.model

data class UserMessage(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val emoji: String = "🏆",
    val type: String = "general",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
