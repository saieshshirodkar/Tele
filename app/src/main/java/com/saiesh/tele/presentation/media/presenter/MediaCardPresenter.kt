package com.saiesh.tele.presentation.media.presenter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import java.io.File
import com.saiesh.tele.R
import com.saiesh.tele.data.cache.ImageCache
import com.saiesh.tele.data.repository.formatDuration
import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.MediaType

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
                    val miniBitmap = getOrDecodeMiniThumbnail(media.messageId, media.miniThumbnailBytes)

                    Glide.with(cardView)
                        .load(File(media.thumbnailPath!!))
                        .centerCrop()
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .let { request ->
                            if (miniBitmap != null) {
                                request.thumbnail(Glide.with(cardView).load(miniBitmap))
                            } else {
                                request.placeholder(R.drawable.no_thumbnail)
                            }
                        }
                        .into(target)
                }
            }
            media.miniThumbnailBytes != null -> {
                val bitmap = getOrDecodeMiniThumbnail(media.messageId, media.miniThumbnailBytes)
                if (bitmap != null) {
                    imageView?.setImageBitmap(bitmap)
                } else {
                    imageView?.setImageResource(R.drawable.no_thumbnail)
                }
            }
            else -> {
                imageView?.setImageResource(R.drawable.no_thumbnail)
            }
        }
    }

    private fun getOrDecodeMiniThumbnail(messageId: Long, bytes: ByteArray?): Bitmap? {
        if (bytes == null) return null
        return ImageCache.getMini(messageId)
            ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.also { decoded -> ImageCache.putMini(messageId, decoded) }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView?.let { imageView ->
            Glide.with(cardView).clear(imageView)
        }
        cardView.mainImage = null
    }

}
