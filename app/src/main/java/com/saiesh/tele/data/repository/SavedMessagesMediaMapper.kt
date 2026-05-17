package com.saiesh.tele.data.repository

import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.MediaType
import org.drinkless.tdlib.TdApi

internal fun SavedMessagesRepository.mapMessageToMediaInternal(message: TdApi.Message): MediaItem? {
    return when (val content = message.content) {
        is TdApi.MessageVideo -> {
            val video = content.video
            val title = content.caption?.text?.takeIf { it.isNotBlank() }
                ?: video?.fileName?.takeIf { it.isNotBlank() }
                ?: "Video"
            val file = video?.video
            val thumb = video?.thumbnail
            MediaItem(
                chatId = message.chatId,
                messageId = message.id,
                date = message.date,
                type = MediaType.Video,
                title = title,
                fileId = file?.id,
                thumbnailFileId = thumb?.file?.id,
                miniThumbnailBytes = video?.minithumbnail?.data,
                thumbnailWidth = thumb?.width ?: 0,
                thumbnailHeight = thumb?.height ?: 0,
                durationSeconds = video?.duration ?: 0,
                fileSizeBytes = file?.size ?: 0
            )
        }
        is TdApi.MessageVideoNote -> {
            val videoNote = content.videoNote
            val file = videoNote?.video
            val thumb = videoNote?.thumbnail
            MediaItem(
                chatId = message.chatId,
                messageId = message.id,
                date = message.date,
                type = MediaType.Video,
                title = "Video Note",
                fileId = file?.id,
                thumbnailFileId = thumb?.file?.id,
                miniThumbnailBytes = videoNote?.minithumbnail?.data,
                thumbnailWidth = thumb?.width ?: 0,
                thumbnailHeight = thumb?.height ?: 0,
                durationSeconds = videoNote?.duration ?: 0,
                fileSizeBytes = file?.size ?: 0
            )
        }
        is TdApi.MessageDocument -> {
            val document = content.document
            if (!isVideoDocumentInternal(document)) return null
            val title = document?.fileName?.takeIf { it.isNotBlank() } ?: "Video"
            val file = document?.document
            val thumb = document?.thumbnail
            MediaItem(
                chatId = message.chatId,
                messageId = message.id,
                date = message.date,
                type = MediaType.Video,
                title = title,
                fileId = file?.id,
                thumbnailFileId = thumb?.file?.id,
                miniThumbnailBytes = document?.minithumbnail?.data,
                thumbnailWidth = thumb?.width ?: 0,
                thumbnailHeight = thumb?.height ?: 0,
                durationSeconds = 0,
                fileSizeBytes = file?.size ?: 0
            )
        }
        is TdApi.MessageAnimation -> {
            val animation = content.animation
            val file = animation?.animation
            val thumb = animation?.thumbnail
            val title = content.caption?.text?.takeIf { it.isNotBlank() }
                ?: animation?.fileName?.takeIf { it.isNotBlank() }
                ?: "Animation"
            MediaItem(
                chatId = message.chatId,
                messageId = message.id,
                date = message.date,
                type = MediaType.Video,
                title = title,
                fileId = file?.id,
                thumbnailFileId = thumb?.file?.id,
                miniThumbnailBytes = animation?.minithumbnail?.data,
                thumbnailWidth = thumb?.width ?: 0,
                thumbnailHeight = thumb?.height ?: 0,
                durationSeconds = animation?.duration ?: 0,
                fileSizeBytes = file?.size ?: 0
            )
        }
        is TdApi.MessagePhoto -> {
            val photo = content.photo
            val bestSize = photo.sizes?.maxByOrNull { it.width * it.height }
            val title = if (content.caption?.text.isNullOrBlank()) "Photo" else content.caption.text
            MediaItem(
                chatId = message.chatId,
                messageId = message.id,
                date = message.date,
                type = MediaType.Photo,
                title = title,
                fileId = bestSize?.photo?.id,
                thumbnailFileId = bestSize?.photo?.id,
                miniThumbnailBytes = photo.minithumbnail?.data,
                thumbnailWidth = bestSize?.width ?: 0,
                thumbnailHeight = bestSize?.height ?: 0,
                durationSeconds = 0,
                fileSizeBytes = bestSize?.photo?.size?.toLong() ?: 0L
            )
        }
        else -> null
    }
}

private val VIDEO_EXTENSIONS = setOf(
    ".mkv", ".mp4", ".mov", ".webm", ".avi", ".m4v", ".mpeg", ".mpg",
    ".wmv", ".flv", ".3gp", ".3g2", ".ts", ".m2ts", ".mxf", ".f4v",
    ".vob", ".ogv", ".rm", ".rmvb"
)

internal fun isVideoDocumentInternal(document: TdApi.Document?): Boolean {
    val mime = document?.mimeType?.lowercase()?.trim().orEmpty()
    val fileName = document?.fileName?.lowercase()?.trim().orEmpty()
    return mime.startsWith("video/") || VIDEO_EXTENSIONS.any { fileName.endsWith(it) }
}
