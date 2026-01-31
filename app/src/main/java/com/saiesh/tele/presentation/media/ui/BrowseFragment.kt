package com.saiesh.tele.presentation.media.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import com.saiesh.tele.app.MainActivity
import com.saiesh.tele.domain.model.media.MediaItem
import com.saiesh.tele.domain.model.media.MediaType
import com.saiesh.tele.domain.model.media.VideoChatItem
import com.saiesh.tele.presentation.media.presenter.MediaCardPresenter
import com.saiesh.tele.presentation.media.presenter.VideoChatPresenter
import com.saiesh.tele.presentation.media.vm.MediaViewModel
import com.saiesh.tele.presentation.search.vm.SearchViewModel
import kotlinx.coroutines.launch

class BrowseFragment : BrowseSupportFragment(),
    MediaContextMenuDialogFragment.Listener,
    ConfirmDeleteDialogFragment.Listener {
    private val mediaViewModel: MediaViewModel by viewModels()
    private val searchViewModel: SearchViewModel by activityViewModels()

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
        setOnSearchClickedListener {
            (activity as? MainActivity)?.showSearch()
        }
        rowsAdapter.add(ListRow(mediaHeader, mediaAdapter))
        rowsAdapter.add(ListRow(chatHeader, chatAdapter))
        adapter = rowsAdapter
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        mediaViewModel.loadIfNeeded()
        mediaViewModel.loadVideoChatsIfNeeded()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                        title = state.selectedChatTitle
                        updateMediaItems(state.items)
                        updateChatItems(state.videoChats)
                        if (state.error != null) {
                            Toast.makeText(requireContext(), state.error, Toast.LENGTH_LONG).show()
                        }
                        if (state.sidebarError != null) {
                            Toast.makeText(requireContext(), state.sidebarError, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    searchViewModel.uiState.collect { state ->
                        if (state.refreshMedia) {
                            mediaViewModel.load()
                            searchViewModel.consumeRefreshMedia()
                        }
                    }
                }
            }
        }
    }

    private fun updateMediaItems(items: List<MediaItem>) {
        mediaAdapter.setItems(items, mediaDiff)
        if (pendingFocusFirstItem) {
            pendingFocusFirstItem = false
            mediaRowViewHolder?.gridView?.post {
                mediaRowViewHolder?.gridView?.setSelectedPosition(0)
                mediaRowViewHolder?.gridView?.requestFocus()
            }
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
        if (item.type != MediaType.Video || item.fileId == null) {
            return
        }
        Toast.makeText(requireContext(), "Fetching fast link...", Toast.LENGTH_SHORT).show()
        mediaViewModel.requestFastLink(item) { url, error ->
            fun launch() {
                if (url.isNullOrBlank()) {
                    Toast.makeText(
                        requireContext(),
                        error ?: "Fast link not found",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                Log.d("Tele", "Launching MPV with fast link url=$url")
                val uri = android.net.Uri.parse(url)
                val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "video/*")
                    .addCategory(Intent.CATEGORY_BROWSABLE)

                val mpvPackage = "is.xyz.mpv"
                val hasMpv = try {
                    requireContext().packageManager.getPackageInfo(mpvPackage, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
                if (hasMpv) {
                    intent.setClassName(mpvPackage, "is.xyz.mpv.MPVActivity")
                    intent.putExtra(Intent.EXTRA_TITLE, item.title)
                } else {
                    Log.w("Tele", "MPV package not found: $mpvPackage")
                }

                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    requireContext().startActivity(intent)
                } catch (e: Exception) {
                    Log.e("Tele", "Failed to launch external player", e)
                    Toast.makeText(requireContext(), "No player found", Toast.LENGTH_LONG).show()
                }
            }
            Handler(Looper.getMainLooper()).post { launch() }
        }
    }

    private fun showContextMenu(item: MediaItem) {
        if (childFragmentManager.findFragmentByTag(TAG_CONTEXT_MENU) != null) return
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
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG_CONTEXT_MENU = "media_context_menu"
        private const val TAG_CONFIRM_DELETE = "media_confirm_delete"
        private const val TAG_DETAILS = "media_details"
    }
}
