package com.saiesh.tele.presentation.media.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.DiffCallback
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import com.saiesh.tele.R
import com.saiesh.tele.domain.model.MediaItem
import com.saiesh.tele.domain.model.MediaType
import com.saiesh.tele.domain.model.VideoChatItem
import com.saiesh.tele.presentation.media.presenter.MediaCardPresenter
import com.saiesh.tele.presentation.media.presenter.VideoChatPresenter
import com.saiesh.tele.presentation.media.vm.MediaViewModel
import kotlinx.coroutines.launch

class BrowseFragment : BrowseSupportFragment(),
    MediaContextMenuDialogFragment.Listener,
    ConfirmDeleteDialogFragment.Listener,
    MediaDetailsDialogFragment.Listener {
    private val mediaViewModel: MediaViewModel by activityViewModels()

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val mediaAdapter = ArrayObjectAdapter(MediaCardPresenter { item ->
        showContextMenu(item)
    })
    private val chatAdapter = ArrayObjectAdapter(VideoChatPresenter())
    private val mediaHeader = HeaderItem(0, "Videos")
    private val chatHeader = HeaderItem(1, "Chats")
    private var mediaRowViewHolder: ListRowPresenter.ViewHolder? = null
    private var pendingFocusFirstItem = false
    private var lastChatKey: Long? = null
    private var lastFocusVersion = 0
    private var lastContextItem: MediaItem? = null
    private val mediaDiff = object : DiffCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }
    private val chatDiff = object : DiffCallback<VideoChatItem>() {
        override fun areItemsTheSame(oldItem: VideoChatItem, newItem: VideoChatItem): Boolean {
            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: VideoChatItem, newItem: VideoChatItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        rowsAdapter.add(ListRow(mediaHeader, mediaAdapter))
        rowsAdapter.add(ListRow(chatHeader, chatAdapter))
        adapter = rowsAdapter
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        view?.post {
            if (isShowingHeaders) {
                startHeadersTransition(false)
            }
            getRowsSupportFragment()?.verticalGridView?.let { grid ->
                grid.invalidate()
                if (!grid.hasFocus()) {
                    grid.requestFocus()
                }
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaViewModel.initialize()
        observeState()
    }

    private fun setupListeners() {
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is MediaItem -> handleMediaClick(item)
                is VideoChatItem -> handleChatClick(item)
            }
        }

        onItemViewSelectedListener = OnItemViewSelectedListener { _, item, rowViewHolder, _ ->
            val listRowViewHolder = rowViewHolder as? ListRowPresenter.ViewHolder
            val listRow = listRowViewHolder?.row as? ListRow
            if (listRow?.adapter == mediaAdapter) {
                mediaRowViewHolder = listRowViewHolder
            }
            if (item is MediaItem) {
                mediaViewModel.onItemFocused(item)
                if (listRow?.adapter == mediaAdapter) {
                    val position = listRowViewHolder.gridView?.selectedPosition ?: 0
                    if (position >= mediaAdapter.size() - 4) {
                        mediaViewModel.loadMoreIfNeeded()
                    }
                }
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mediaViewModel.uiState.collect { state ->
                        val chatKey = if (state.isSavedMessagesSelected) -1L else state.selectedChatId ?: -1L
                        if (lastChatKey != chatKey) {
                            lastChatKey = chatKey
                            pendingFocusFirstItem = true
                        }
                        val rawTitle = state.selectedChatTitle ?: ""
                        val boldTitle = SpannableString(rawTitle).apply {
                            setSpan(StyleSpan(Typeface.BOLD), 0, rawTitle.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        title = boldTitle
                        updateMediaItems(state.items, state.focusVersion)
                        updateChatItems(state.videoChats)
                        if (state.error != null) {
                            Toast.makeText(requireContext(), state.error, Toast.LENGTH_LONG).show()
                        }
                        if (state.sidebarError != null) {
                            Toast.makeText(requireContext(), state.sidebarError, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateMediaItems(items: List<MediaItem>, focusVersion: Int) {
        mediaAdapter.setItems(items, mediaDiff)
        val shouldFocus = pendingFocusFirstItem || (focusVersion != lastFocusVersion).also {
            lastFocusVersion = focusVersion
        }
        if (shouldFocus) {
            pendingFocusFirstItem = false
            focusMediaGrid()
        }
    }

    private fun focusMediaGrid() {
        val rowsGrid = getRowsSupportFragment()?.verticalGridView ?: return
        rowsGrid.setSelectedPosition(0)
        rowsGrid.post {
            val vh = rowsGrid.findViewHolderForAdapterPosition(0) as? ListRowPresenter.ViewHolder
            val mediaGrid = vh?.gridView ?: return@post
            mediaGrid.layoutManager?.scrollToPosition(0)
            mediaGrid.setSelectedPosition(0)
            mediaGrid.requestFocus()
        }
    }

    private fun updateChatItems(chats: List<VideoChatItem>) {
        chatAdapter.setItems(chats, chatDiff)
    }

    private fun handleChatClick(chat: VideoChatItem) {
        if (chat.isSavedMessages) {
            pendingFocusFirstItem = true
            mediaViewModel.load()
        } else {
            pendingFocusFirstItem = true
            mediaViewModel.loadChat(chat.chatId, chat.title)
        }
    }

    private fun handleMediaClick(item: MediaItem) {
        if (item.type != MediaType.Video || item.fileId == null) return
        if (!isAdded) return
        Toast.makeText(requireContext(), "Fetching fast link...", Toast.LENGTH_SHORT).show()
        mediaViewModel.requestFastLink(item) { url, error ->
            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread
                if (url.isNullOrBlank()) {
                    Toast.makeText(
                        requireContext(),
                        error ?: "Fast link not found",
                        Toast.LENGTH_LONG
                    ).show()
                    return@runOnUiThread
                }
                Log.d("Tele", "Launching MPVTube with fast link url=$url")
                val uri = android.net.Uri.parse(url)
                val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                val playerPackage = "com.mpvtube"
                val hasPlayer = try {
                    requireContext().packageManager.getPackageInfo(playerPackage, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
                if (hasPlayer) {
                    intent.setClassName(playerPackage, "com.mpvtube.MainActivity")
                    intent.putExtra(Intent.EXTRA_TITLE, item.title)
                } else {
                    Log.w("Tele", "MPVTube package not found: $playerPackage")
                }

                try {
                    requireContext().startActivity(intent)
                } catch (e: Exception) {
                    Log.e("Tele", "Failed to launch external player", e)
                    Toast.makeText(requireContext(), "No player found", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showContextMenu(item: MediaItem) {
        if (childFragmentManager.findFragmentByTag(TAG_CONTEXT_MENU) != null) return
        lastContextItem = item
        MediaContextMenuDialogFragment
            .newInstance(item)
            .show(childFragmentManager, TAG_CONTEXT_MENU)
    }

    override fun onContextPlay(item: MediaItem) {
        handleMediaClick(item)
    }

    override fun onContextDetails(item: MediaItem) {
        if (childFragmentManager.findFragmentByTag(TAG_DETAILS) != null) return
        MediaDetailsDialogFragment
            .newInstance(item)
            .show(childFragmentManager, TAG_DETAILS)
    }

    override fun onContextDelete(item: MediaItem) {
        if (childFragmentManager.findFragmentByTag(TAG_CONFIRM_DELETE) != null) return
        ConfirmDeleteDialogFragment
            .newInstance(item)
            .show(childFragmentManager, TAG_CONFIRM_DELETE)
    }

    override fun onConfirmDelete(item: MediaItem) {
        mediaViewModel.deleteMediaItem(item) { error ->
            if (error != null) {
                Toast.makeText(requireActivity(), error, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireActivity(), "Deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDetailsDismiss() {
    }

    override fun onDeleteDismiss() {
    }

    companion object {
        private const val TAG_CONTEXT_MENU = "media_context_menu"
        private const val TAG_CONFIRM_DELETE = "media_confirm_delete"
        private const val TAG_DETAILS = "media_details"
    }
}
