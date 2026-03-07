package com.saiesh.tele.domain.model.media

data class VideoChatItem(
    val chatId: Long,
    val title: String,
    val isSavedMessages: Boolean,
    val isSelected: Boolean = false
)
