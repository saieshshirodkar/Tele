package com.saiesh.tele.presentation.media.dialog

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.MediaItem

class MediaContextMenuDialogFragment : BaseMediaDialogFragment(R.layout.dialog_media_context_menu) {
    interface Listener {
        fun onContextPlay(item: MediaItem)

        fun onContextDetails(item: MediaItem)

        fun onContextDelete(item: MediaItem)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        view.findViewById<View>(R.id.context_menu_overlay).setOnClickListener { dismiss() }
        view.findViewById<TextView>(R.id.context_menu_title).text = mediaItem.title
        val playNow = view.findViewById<TextView>(R.id.context_menu_play)
        val details = view.findViewById<TextView>(R.id.context_menu_details)
        val delete = view.findViewById<TextView>(R.id.context_menu_delete)

        playNow.setOnClickListener {
            (parentFragment as? Listener)?.onContextPlay(mediaItem)
            dismiss()
        }
        details.setOnClickListener {
            dismiss()
            (parentFragment as? Listener)?.onContextDetails(mediaItem)
        }
        delete.setOnClickListener {
            dismiss()
            (parentFragment as? Listener)?.onContextDelete(mediaItem)
        }

        playNow.requestFocus()
    }

    companion object {
        fun newInstance(item: MediaItem): MediaContextMenuDialogFragment =
            MediaContextMenuDialogFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_ITEM, item) }
            }
    }
}
