package com.saiesh.tele.presentation.media.presenter

import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import java.io.File
import com.saiesh.tele.data.cache.image.ImageCache
import com.saiesh.tele.domain.model.media.MediaItem
import com.saiesh.tele.domain.model.media.MediaType

class MediaCardPresenter(
    private val onLongPress: (MediaItem) -> Unit
) : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val context = parent.context
        val cardView = ImageCardView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            val widthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                260f,
                resources.displayMetrics
            ).toInt()
            val heightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                146f,
                resources.displayMetrics
            ).toInt()
            setMainImageDimensions(widthPx, heightPx)
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val media = item as? MediaItem ?: return
        val cardView = viewHolder.view as ImageCardView
        cardView.setOnLongClickListener {
            onLongPress(media)
            true
        }
        cardView.titleText = media.title
        cardView.contentText = if (media.type == MediaType.Video && media.durationSeconds > 0) {
            formatDuration(media.durationSeconds)
        } else {
            ""
        }
        val imageView = cardView.mainImageView
        when {
            !media.thumbnailPath.isNullOrBlank() -> {
                imageView?.let { target ->
                    Glide.with(cardView)
                        .load(File(media.thumbnailPath!!))
                        .centerCrop()
                        .into(target)
                }
            }
            media.miniThumbnailBytes != null -> {
                val bitmap = ImageCache.getMini(media.messageId)
                    ?: BitmapFactory.decodeByteArray(media.miniThumbnailBytes, 0, media.miniThumbnailBytes.size)
                        ?.also { decoded -> ImageCache.putMini(media.messageId, decoded) }
                imageView?.setImageBitmap(bitmap)
            }
            else -> imageView?.setImageDrawable(null)
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView?.let { imageView ->
            Glide.with(cardView).clear(imageView)
        }
        cardView.mainImage = null
    }

    private fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02dh%02dm%02ds", hours, minutes, seconds)
    }
}
