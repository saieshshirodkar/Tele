package com.saiesh.tele.domain.model

import java.io.Serializable

data class MediaItem(
    val chatId: Long,
    val messageId: Long,
    val date: Int,
    val type: MediaType,
    val title: String,
    val fileId: Int?,
    val thumbnailFileId: Int? = null,
    val thumbnailPath: String? = null,
    val miniThumbnailBytes: ByteArray? = null,
    val thumbnailWidth: Int = 0,
    val thumbnailHeight: Int = 0,
    val durationSeconds: Int = 0,
    val fileSizeBytes: Long = 0,
) : Serializable

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02dh%02dm%02ds", hours, minutes, seconds)
}
