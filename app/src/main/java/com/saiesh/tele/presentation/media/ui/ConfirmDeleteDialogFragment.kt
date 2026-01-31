package com.saiesh.tele.presentation.media.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.media.MediaItem

class ConfirmDeleteDialogFragment : DialogFragment(R.layout.dialog_confirm_delete) {
    interface Listener {
        fun onConfirmDelete(item: MediaItem)
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
        view.findViewById<View>(R.id.confirm_overlay).setOnClickListener { dismiss() }
        view.findViewById<TextView>(R.id.confirm_title).text = getString(R.string.context_delete_title)
        view.findViewById<TextView>(R.id.confirm_message).text =
            getString(R.string.context_delete_message, mediaItem.title)

        val confirm = view.findViewById<TextView>(R.id.confirm_delete)
        val cancel = view.findViewById<TextView>(R.id.confirm_cancel)

        confirm.setOnClickListener {
            (parentFragment as? Listener)?.onConfirmDelete(mediaItem)
            dismiss()
        }
        cancel.setOnClickListener { dismiss() }

        cancel.requestFocus()
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
