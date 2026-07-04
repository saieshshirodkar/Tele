package com.saiesh.tele.presentation.media.ui

import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.TextView
import com.saiesh.tele.R
import com.saiesh.tele.data.mapper.formatDuration
import com.saiesh.tele.domain.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaDetailsDialogFragment : BaseMediaDialogFragment(R.layout.dialog_media_details) {
    interface Listener {
        fun onDetailsDismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.details_overlay).setOnClickListener { dismissAndNotify() }
        view.findViewById<TextView>(R.id.details_title).text = mediaItem.title
        view.findViewById<TextView>(R.id.details_body).text = buildDetailsText()
    }

    private fun dismissAndNotify() {
        dismiss()
        (parentFragment as? Listener)?.onDetailsDismiss()
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
            append("Type: ").append(mediaItem.type.name).append('\n')
            append("Duration: ").append(durationLabel).append('\n')
            append("Size: ").append(sizeLabel).append('\n')
            append("Date: ").append(dateLabel)
        }
    }

    companion object {
        fun newInstance(item: MediaItem): MediaDetailsDialogFragment {
            return MediaDetailsDialogFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_ITEM, item) }
            }
        }
    }
}
