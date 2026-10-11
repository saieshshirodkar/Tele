package com.saiesh.tele.presentation.media.dialog

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.MediaItem

class ConfirmDeleteDialogFragment : BaseMediaDialogFragment(R.layout.dialog_confirm_delete) {
    interface Listener {
        fun onConfirmDelete(item: MediaItem)

        fun onDeleteDismiss()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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

    companion object {
        fun newInstance(item: MediaItem): ConfirmDeleteDialogFragment =
            ConfirmDeleteDialogFragment().apply {
                arguments = Bundle().apply { putSerializable(ARG_ITEM, item) }
            }
    }
}
