package com.saiesh.tele.presentation.media.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.MediaItem

class MediaContextMenuDialogFragment : DialogFragment(R.layout.dialog_media_context_menu) {
    interface Listener {
        fun onContextPlay(item: MediaItem)
        fun onContextDetails(item: MediaItem)
        fun onContextDelete(item: MediaItem)
    }

    private val mediaItem: MediaItem by lazy {
        BundleCompat.getSerializable(requireArguments(), ARG_ITEM, MediaItem::class.java)
            ?: error("Missing media item")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Tele_ContextMenu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setGravity(Gravity.END)
    }

    companion object {
        private const val ARG_ITEM = "arg_media_item"

        fun newInstance(item: MediaItem): MediaContextMenuDialogFragment {
            return MediaContextMenuDialogFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_ITEM, item) }
            }
        }
    }
}

class ConfirmDeleteDialogFragment : DialogFragment(R.layout.dialog_confirm_delete) {
    interface Listener {
        fun onConfirmDelete(item: MediaItem)
        fun onDeleteDismiss()
    }

    private val mediaItem: MediaItem by lazy {
        BundleCompat.getSerializable(requireArguments(), ARG_ITEM, MediaItem::class.java)
            ?: error("Missing media item")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Tele_ContextMenu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.confirm_overlay).setOnClickListener { dismissAndNotify() }
        view.findViewById<TextView>(R.id.confirm_title).text = getString(R.string.context_delete_title)
        view.findViewById<TextView>(R.id.confirm_message).text =
            getString(R.string.context_delete_message, mediaItem.title)

        val confirm = view.findViewById<TextView>(R.id.confirm_delete)
        val cancel = view.findViewById<TextView>(R.id.confirm_cancel)

        confirm.setOnClickListener {
            (parentFragment as? Listener)?.onConfirmDelete(mediaItem)
            dismiss()
        }
        cancel.setOnClickListener { dismissAndNotify() }

        cancel.requestFocus()
    }

    private fun dismissAndNotify() {
        dismiss()
        (parentFragment as? Listener)?.onDeleteDismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setGravity(Gravity.END)
    }

    companion object {
        private const val ARG_ITEM = "arg_media_item"

        fun newInstance(item: MediaItem): ConfirmDeleteDialogFragment {
            return ConfirmDeleteDialogFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_ITEM, item) }
            }
        }
    }
}
