package com.saiesh.tele.presentation.media.presenter

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.VideoChatItem

class VideoChatPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val context = parent.context
        val textView = TextView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(36, 26, 36, 26)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
            setOnFocusChangeListener { _, hasFocus ->
                setTextColor(if (hasFocus) Color.BLACK else Color.WHITE)
            }
        }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val chat = item as? VideoChatItem ?: return
        val textView = viewHolder.view as TextView
        textView.text = chat.title
        if (chat.isSelected) {
            textView.background = ContextCompat.getDrawable(textView.context, R.drawable.chat_item_selected_background)
        } else {
            textView.background = ContextCompat.getDrawable(textView.context, R.drawable.chat_item_background)
        }
        textView.setTextColor(if (textView.isFocused) Color.BLACK else Color.WHITE)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit
}
