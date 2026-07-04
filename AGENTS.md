# Tele — Android TV Telegram Client

Android TV app to browse and play Telegram media (videos, photos) via TDLib. Uses Leanback UI, MPVTube external player, and StreamVaultProBot for streaming links.

## Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build + install on connected TV
./gradlew assembleRelease        # Build release APK
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

```
domain/                              # Pure Kotlin, no Android deps
  model/MediaModels.kt               # MediaItem, VideoChatItem, MediaUiState
  repository/TeleRepository.kt       # Interface

data/
  source/tdlib/
    TdLibChatSource.kt               # GetChats (ordered), GetChat, SearchMessages, resolve saved
    TdLibMediaSource.kt              # SearchChatMessages, GetChatHistory, GetMessage
    TdLibFileSource.kt               # GetFile, DownloadFile (thumbnails)
    TdLibMessageSource.kt            # DeleteMessages, requestFastLink (forward bot)
    TdLibConstants.kt                # Bot username, timeouts, page sizes
  mapper/MediaMapper.kt              # TdApi.Message → MediaItem, formatDuration
  repository/TeleRepositoryImpl.kt   # Orchestrates all sources

core/
  tdlib/client/TdLibClient.kt        # Singleton Client + update handler registry
  tdlib/auth/TelegramAuthManager.kt  # TDLib auth state machine
  cache/TeleCache.kt                 # Serialized disk cache (sidebar, items, titles)

presentation/media/
  vm/MediaViewModel.kt               # StateFlow<MediaUiState>, update handlers
  ui/BrowseFragment.kt               # Leanback grid + sidebar + focus management
  presenter/
    MediaCardPresenter.kt            # Card with TopRoundedCorners, Glide
    VideoChatPresenter.kt            # Chat sidebar item
```

## Data Flow

### Chat Sidebar
```
1. GetChats(ChatListMain, 200)       → ordered chat IDs (pinned first, mobile order)
2. 4× SearchMessages(Video|VideoNote|Animation|Document, 100)  → find chats with video
3. Intersect: keep ordered IDs that have video content
4. fetchChatTitles → GetChat for missing titles → save cache
5. Sort: Saved Messages first, then GetChats order
```

### Media Grid
```
Initial: 4× SearchChatMessages(chatId, 90 per filter)
Paged:   GetChatHistory(chatId, fromMessageId, 100) → filter client-side
```

### Playback
```
BrowseFragment.handleMediaClick()
  → MediaViewModel.requestFastLink()
    → Forward to StreamVaultProBot → wait for reply → extract URL
  → Intent → com.mpvtube.MainActivity
```

### Real-time Updates
```
UpdateNewMessage:
  → isMessageVideoType? → MediaMapper.mapMessage() → prepend to grid + focusVersion++
  → chat not in sidebar? → debounced 500ms → loadVideoChats()

UpdateDeleteMessages:
  → remove from grid + focusVersion++
  → always trigger sidebar refresh

UpdateNewChat | UpdateChatAddedToList | UpdateChatRemovedFromList | UpdateChatPosition:
  → debounced 500ms → loadVideoChats()
```

### Thumbnail Loading
```
TDLib thumbnail → downloadThumbnail → file path → MediaItem.thumbnailPath
  → MediaCardPresenter: Glide with CenterCrop + TopRoundedCorners(12dp)
    → DecodeFormat.PREFER_ARGB_8888, DiskCacheStrategy.DATA, ObjectKey signature
Fallback: miniThumbnailBytes → ImageCache → BitmapFactory
```

## Key Classes

| Class | File | Role |
|-------|------|------|
| `TeleRepositoryImpl` | `data/repository/` | Orchestrates all data sources |
| `TdLibChatSource` | `data/source/tdlib/` | GetChats order, SearchMessages, GetChat |
| `TdLibMediaSource` | `data/source/tdlib/` | Per-chat media search + history |
| `TdLibFileSource` | `data/source/tdlib/` | Thumbnail download with dedup |
| `TdLibMessageSource` | `data/source/tdlib/` | Delete + bot forward flow |
| `MediaMapper` | `data/mapper/` | TdApi.Message → domain model |
| `TeleCache` | `core/cache/` | Disk-persisted sidebar, items, titles |
| `MediaViewModel` | `presentation/media/vm/` | State machine, update handlers |
| `BrowseFragment` | `presentation/media/ui/` | Leanback grid, sidebar, focus |
| `MediaCardPresenter` | `presentation/media/presenter/` | Card rendering, Glide, rounding |

## Dependencies

- **Leanback**: SmartTube fork (`leanback-1.0.0/`) as local module
- **sharedutils**: Stub module — only `Helpers.describedBy()` needed by leanback
- **TDLib**: Java bindings in `core/tdlib/`
- **Glide**: Image loading with custom BitmapTransformation
- **Player**: MPVTube (`com.mpvtube`)

## Config

- Player: `com.mpvtube` / `com.mpvtube.MainActivity`
- Bot: `StreamVaultProBot` (in `TdLibConstants.kt`)
- Page size: 30, Sidebar: 15, Search limit: 100, History page: 100
- Thumbnail: 260×146 dp, 12dp corner radius, top-only rounded
- Cache dir: `cacheDir/tele_cache/`, 7-day TTL for media items

## Notes

- Sidebar uses GetChats order to match Telegram mobile (pinned first, then main list order)
- SearchMessages finds video-having chats efficiently (4 cross-chat queries instead of per-chat)
- No full media caching — TDLib local DB provides persistence; TeleCache only caches sidebar state + chat titles
- Real-time focus: new videos scroll to position 0 in the grid via focusVersion signal
- TopRoundedCorners applies to Glide-loaded thumbnails only (mini-thumbnail fallback bypasses it)
