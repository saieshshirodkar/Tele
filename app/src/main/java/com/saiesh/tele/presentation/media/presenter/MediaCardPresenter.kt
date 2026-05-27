package com.saiesh.tele.presentation.media.presenter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Matrix
import android.util.TypedValue
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import java.io.File
import java.security.MessageDigest
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
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics
        ).toInt()
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
            setBackgroundResource(R.drawable.rounded_card_background)
            setInfoAreaBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = ViewGroup.MarginLayoutParams(
                widthPx, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = marginPx
                rightMargin = marginPx
            }
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

                    val cornerPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 12f, cardView.resources.displayMetrics
                    ).toInt()
                    Glide.with(cardView)
                        .load(File(media.thumbnailPath!!))
                        .format(DecodeFormat.PREFER_ARGB_8888)
                        .transform(CenterCrop(), TopRoundedCorners(cornerPx))
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

private class TopRoundedCorners(private val radius: Int) : BitmapTransformation() {
    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val result = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, outWidth.toFloat(), outHeight.toFloat())
        val radii = floatArrayOf(
            radius.toFloat(), radius.toFloat(),
            radius.toFloat(), radius.toFloat(),
            0f, 0f,
            0f, 0f
        )
        val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
        paint.shader = BitmapShader(toTransform, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply {
                setRectToRect(
                    RectF(0f, 0f, toTransform.width.toFloat(), toTransform.height.toFloat()),
                    RectF(0f, 0f, outWidth.toFloat(), outHeight.toFloat()),
                    Matrix.ScaleToFit.CENTER
                )
            })
        }
        canvas.drawPath(path, paint)
        return result
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("com.saiesh.tele.TopRoundedCorners$radius".toByteArray())
    }

    override fun equals(other: Any?): Boolean = other is TopRoundedCorners && other.radius == radius
    override fun hashCode(): Int = radius.hashCode()
}
