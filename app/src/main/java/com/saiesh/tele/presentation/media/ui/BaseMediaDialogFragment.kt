package com.saiesh.tele.presentation.media.ui

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.MediaItem

abstract class BaseMediaDialogFragment(layoutResId: Int) : DialogFragment(layoutResId) {

    protected val mediaItem: MediaItem by lazy {
        BundleCompat.getSerializable(requireArguments(), ARG_ITEM, MediaItem::class.java)
            ?: error("Missing media item")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Tele_ContextMenu)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setGravity(Gravity.END)
    }

    companion object {
        const val ARG_ITEM = "arg_media_item"
    }
}
