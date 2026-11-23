package com.example.smartassistant.ui

enum class Author { USER, ASSISTANT }

data class ChatMessage(val id: Long, val text: String, val author: Author)

enum class Status { IDLE, LISTENING }

data class UiState(
    val messages: List<ChatMessage> = emptyList(),
    val status: Status = Status.IDLE,
)
