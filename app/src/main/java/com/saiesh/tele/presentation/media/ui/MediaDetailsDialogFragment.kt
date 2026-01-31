package com.saiesh.tele.presentation.media.ui

import android.os.Bundle
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.media.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaDetailsDialogFragment : DialogFragment(R.layout.dialog_media_details) {
    private val mediaItem: MediaItem by lazy {
        BundleCompat.getSerializable(requireArguments(), ARG_ITEM, MediaItem::class.java)
            ?: error("Missing media item")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Tele_ContextMenu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.details_overlay).setOnClickListener { dismiss() }
        view.findViewById<TextView>(R.id.details_title).text = mediaItem.title
        view.findViewById<TextView>(R.id.details_body).text = buildDetailsText()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setGravity(Gravity.END)
    }

    private fun buildDetailsText(): String {
        val sizeLabel = if (mediaItem.fileSizeBytes > 0) {
            Formatter.formatShortFileSize(requireContext(), mediaItem.fileSizeBytes)
        } else {
            "Unknown"
        }
        val durationLabel = if (mediaItem.durationSeconds > 0) {
            formatDuration(mediaItem.durationSeconds)
        } else {
            "Unknown"
        }
        val dateLabel = if (mediaItem.date > 0) {
            val date = Date(mediaItem.date * 1000L)
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)
        } else {
            "Unknown"
        }
        return buildString {
            append("Chat ID: ").append(mediaItem.chatId).append('\n')
            append("Message ID: ").append(mediaItem.messageId).append('\n')
            append("Type: ").append(mediaItem.type.name).append('\n')
            append("Duration: ").append(durationLabel).append('\n')
            append("Size: ").append(sizeLabel).append('\n')
            append("Date: ").append(dateLabel).append('\n')
            append("File ID: ").append(mediaItem.fileId ?: "Unknown").append('\n')
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02dh%02dm%02ds", hours, minutes, seconds)
    }

    companion object {
        private const val ARG_ITEM = "arg_media_item"

        fun newInstance(item: MediaItem): MediaDetailsDialogFragment {
            return MediaDetailsDialogFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_ITEM, item) }
            }
        }
    }
}
