package com.saiesh.tele.presentation.media.presenter

import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.media.VideoChatItem

class VideoChatPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val context = parent.context
        val textView = object : TextView(context) {
            override fun setSelected(selected: Boolean) {
                super.setSelected(selected)
                applyStyle(this, selected)
            }
        }.apply {
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
        }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val chat = item as? VideoChatItem ?: return
        val textView = viewHolder.view as TextView
        textView.text = chat.title
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit

    private fun applyStyle(textView: TextView, isFocused: Boolean) {
        val context = textView.context
        val defaultColor = ContextCompat.getColor(context, R.color.default_background)
        val selectedColor = ContextCompat.getColor(context, R.color.selected_background)
        val defaultText = ContextCompat.getColor(context, android.R.color.darker_gray)
        val selectedText = ContextCompat.getColor(context, android.R.color.white)
        textView.setBackgroundColor(if (isFocused) selectedColor else defaultColor)
        textView.setTextColor(if (isFocused) selectedText else defaultText)
    }
}
