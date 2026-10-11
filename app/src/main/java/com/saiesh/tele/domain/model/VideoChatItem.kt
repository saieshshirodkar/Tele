package com.saiesh.tele.domain.model

import java.io.Serializable

const val SAVED_MESSAGES_TITLE = "Saved Messages"

data class VideoChatItem(
    val chatId: Long,
    val title: String,
    val isSavedMessages: Boolean,
    val isSelected: Boolean = false,
    val order: Long = 0L,
) : Serializable
