# Tele Android TV App - AI Context Documentation

**File:** `ai/llm.md`  
**Purpose:** Comprehensive documentation for AI systems to understand the Tele codebase

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Key Components](#key-components)
4. [Build System](#build-system)
5. [Security](#security)
6. [External Integrations](#external-integrations)
7. [Development Setup](#development-setup)
8. [Release Process](#release-process)
9. [File Structure](#file-structure)
10. [Quick Reference](#quick-reference)

---

## Project Overview

### App Details

| Property | Value |
|----------|-------|
| **App Name** | Tele |
| **Package** | `com.saiesh.tele` |
| **Version** | 0.0.1 (versionCode: 1) |
| **License** | MIT License (Copyright 2026 Syndicate) |

### What the App Does

Tele is an Android TV application that allows users to browse and play videos, photos, and other media from their Telegram chats directly on their TV. It connects to Telegram via the official TDLib (Telegram Database Library) and provides a TV-optimized interface.

### Main Features

1. **Authentication**: Multi-step auth flow with API keys, phone number, verification code, and 2FA support
2. **Media Browsing**: Browse videos from Saved Messages and other chats
3. **Video Playback**: Stream videos via fast download links using external players
4. **Search**: Search for media via Telegram bots (ProSearchM11Bot)
5. **Chat Navigation**: Switch between different chats with video content
6. **Media Management**: Delete messages from chats
7. **Thumbnail Caching**: Efficient image loading with Glide and LRU cache

### Target Platform

| Property | Value |
|----------|-------|
| **Primary Platform** | Android TV (Leanback interface) |
| **Minimum SDK** | 23 (Android 6.0) |
| **Target SDK** | 36 (Android 16) |
| **Compile SDK** | 36 |
| **Architecture** | armeabi-v7a only (32-bit ARM) |

### Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin (with some Java for TDLib) |
| **UI Framework** | Android Leanback (Android TV UI library) |
| **Async Programming** | Kotlin Coroutines and Flow |
| **Image Loading** | Glide 4.11.0 |
| **Telegram Integration** | TDLib (native library `libtdjni.so`) |
| **Build System** | Gradle with Kotlin DSL |
| **Dependency Management** | Version catalogs (`libs.versions.toml`) |

---

## Architecture

### Layered Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Presentation)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Fragments  │  │  ViewModels  │  │  Presenters  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   Domain Layer (Models)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  MediaItem   │  │   UI States  │  │  Auth Steps  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Data Layer (Repository)               │
│  ┌──────────────────────────────────────────────────┐  │
│  │         SavedMessagesRepository                  │  │
│  │  (Media, Search, Thumbnails, Chats, Delete)      │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   Core Layer (TDLib)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ TdLibClient  │  │TelegramAuth  │  │  ApiCredStore│  │
│  │   (Object)   │  │   Manager    │  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Package Structure

| Package | Path | Purpose |
|---------|------|---------|
| **app** | `/app/src/main/java/com/saiesh/tele/app/` | Application entry point (Application class, MainActivity) |
| **core/tdlib** | `/app/src/main/java/com/saiesh/tele/core/tdlib/` | TDLib integration (Client wrapper, Auth manager) |
| **data/cache** | `/app/src/main/java/com/saiesh/tele/data/cache/` | Caching layer (ImageCache) |
| **data/repository** | `/app/src/main/java/com/saiesh/tele/data/repository/` | Data access (SavedMessagesRepository) |
| **data/store** | `/app/src/main/java/com/saiesh/tele/data/store/` | Local storage (SharedPreferences wrapper) |
| **domain/model** | `/app/src/main/java/com/saiesh/tele/domain/model/` | Domain models (MediaItem, AuthState, Search models) |
| **presentation/auth** | `/app/src/main/java/com/saiesh/tele/presentation/auth/` | Auth UI (Fragment, ViewModel) |
| **presentation/media** | `/app/src/main/java/com/saiesh/tele/presentation/media/` | Media browsing UI (BrowseFragment, Presenters, Dialogs) |
| **presentation/search** | `/app/src/main/java/com/saiesh/tele/presentation/search/` | Search UI (Fragment, Adapter, ViewModel) |

### Key Classes and Responsibilities

#### Application Layer

- **TeleApp** (`/app/src/main/java/com/saiesh/tele/app/TeleApp.kt`): Application class that loads the native TDLib library (`System.loadLibrary("tdjni")`)
- **MainActivity** (`/app/src/main/java/com/saiesh/tele/app/MainActivity.kt`): Single Activity that hosts fragments and manages navigation between Auth and Browse screens

#### Core TDLib Layer

- **TdLibClient** (`/app/src/main/java/com/saiesh/tele/core/tdlib/client/TdLibClient.kt`): Singleton wrapper around TDLib's Client class, manages update/error handlers using CopyOnWriteArrayList for thread safety
- **TelegramAuthManager** (`/app/src/main/java/com/saiesh/tele/core/tdlib/auth/TelegramAuthManager.kt`): Handles entire authentication flow with TDLib, manages authorization state machine

#### Data Layer

- **SavedMessagesRepository** (`/app/src/main/java/com/saiesh/tele/data/repository/media/SavedMessagesRepository.kt`): Main repository class that delegates to internal extension functions for different operations (paging, search, thumbnails, etc.)
- **ApiCredentialsStore** (`/app/src/main/java/com/saiesh/tele/data/store/ApiCredentialsStore.kt`): Simple SharedPreferences wrapper for storing API credentials securely

#### Presentation Layer

- **AuthViewModel** (`/app/src/main/java/com/saiesh/tele/presentation/auth/vm/AuthViewModel.kt`): Manages auth UI state and user input validation
- **MediaViewModel** (`/app/src/main/java/com/saiesh/tele/presentation/media/vm/MediaViewModel.kt`): Manages media browsing state, pagination, and thumbnail loading
- **SearchViewModel** (`/app/src/main/java/com/saiesh/tele/presentation/search/vm/SearchViewModel.kt`): Manages search state and bot interactions

### Data Flow

#### 1. Authentication Flow

```
User Input → AuthViewModel → TelegramAuthManager → TdLibClient → TDLib
TDLib Updates → TelegramAuthManager → AuthUiState → AuthViewModel → AuthFragment
```

#### 2. Media Loading Flow

```
BrowseFragment → MediaViewModel → SavedMessagesRepository → TdLibClient → TDLib
TDLib Response → Repository → Callback → ViewModel → MediaUiState → UI Update
```

#### 3. Search Flow

```
SearchFragment → SearchViewModel → Repository → TDLib Bot Interaction
Bot Response → Repository → SearchBotResponse → ViewModel → UI Update
```

---

## Key Components

### UI Components

#### Activities

- **MainActivity** (`/app/src/main/java/com/saiesh/tele/app/MainActivity.kt`):
  - Extends `FragmentActivity`
  - Hosts all fragments in `R.id.main_browse_fragment`
  - Manages navigation between AuthFragment and BrowseFragment
  - Handles back press for exit confirmation

#### Fragments

| Fragment | File | Purpose |
|----------|------|---------|
| **AuthFragment** | `presentation/auth/ui/AuthFragment.kt` | Multi-step authentication UI (API keys, phone, code, password) |
| **BrowseFragment** | `presentation/media/ui/BrowseFragment.kt` | Main media browsing with Leanback's BrowseSupportFragment |
| **SearchFragment** | `presentation/search/ui/SearchFragment.kt` | Search interface with RecyclerView |
| **MediaContextMenuDialogFragment** | `presentation/media/ui/MediaContextMenuDialogFragment.kt` | Context menu for media items (Play, Details, Delete) |
| **MediaDetailsDialogFragment** | `presentation/media/ui/MediaDetailsDialogFragment.kt` | Shows detailed media information |
| **ConfirmDeleteDialogFragment** | `presentation/media/ui/ConfirmDeleteDialogFragment.kt` | Confirmation dialog for deletion |

#### Presenters (Leanback)

- **MediaCardPresenter** (`/app/src/main/java/com/saiesh/tele/presentation/media/presenter/MediaCardPresenter.kt`): Presents media items as cards with thumbnails using ImageCardView
- **VideoChatPresenter** (`/app/src/main/java/com/saiesh/tele/presentation/media/presenter/VideoChatPresenter.kt`): Presents chat list items as text views with focus styling

#### Adapters

- **SearchResultsAdapter** (`/app/src/main/java/com/saiesh/tele/presentation/search/adapter/SearchResultsAdapter.kt`): RecyclerView adapter for search results

### Core Functionality

#### TDLib Integration

The app integrates with Telegram through the official TDLib:

- **Native Library**: `/app/src/main/jniLibs/armeabi-v7a/libtdjni.so` (15.7 MB)
- **Java Wrapper**: 
  - `Client.java` (`/app/src/main/java/org/drinkless/tdlib/Client.java`): Main client class with ResultHandler interface
  - `TdApi.java` (`/app/src/main/java/org/drinkless/tdlib/TdApi.java`): Auto-generated API classes

#### Authentication System

The auth system follows TDLib's authorization state machine:

1. **EnterApiKeys**: User enters API ID and Hash
2. **WaitTdlibParameters**: Set database paths, device info
3. **WaitPhoneNumber**: User enters phone number
4. **WaitCode**: User enters verification code
5. **WaitPassword**: User enters 2FA password (if enabled)
6. **Ready**: Authorized state

**File**: `/app/src/main/java/com/saiesh/tele/core/tdlib/auth/TelegramAuthManager.kt`

### Data Layer (Repositories)

**SavedMessagesRepository** is split into multiple internal extension files:

| File | Purpose |
|------|---------|
| `SavedMessagesRepository.kt` | Main class with public API and constants |
| `SavedMessagesMediaMapper.kt` | Maps TDLib Message objects to MediaItem domain models |
| `SavedMessagesPaging.kt` | Paged loading of media from chat history |
| `SavedMessagesChats.kt` | Loading chat list and Saved Messages chat resolution |
| `SavedMessagesMediaSearch.kt` | Searching media within chats using filters |
| `SavedMessagesSearch.kt` | Bot-based search functionality (ProSearchM11Bot) |
| `SavedMessagesThumbnail.kt` | Thumbnail downloading and path resolution |
| `SavedMessagesFastLink.kt` | Fast download link generation via FileToLinkV5Bot |
| `SavedMessagesDelete.kt` | Message deletion functionality |

### Domain Layer (Models)

**MediaItem** (`/app/src/main/java/com/saiesh/tele/domain/model/media/MediaItem.kt`):

```kotlin
data class MediaItem(
    val chatId: Long,
    val messageId: Long,
    val date: Int,
    val type: MediaType,  // Photo or Video
    val title: String,
    val fileId: Int?,
    val thumbnailFileId: Int?,
    val thumbnailPath: String?,
    val miniThumbnailBytes: ByteArray?,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int,
    val durationSeconds: Int,
    val fileSizeBytes: Long
)
```

**VideoChatItem** (`/app/src/main/java/com/saiesh/tele/domain/model/media/VideoChatItem.kt`):

```kotlin
data class VideoChatItem(
    val chatId: Long,
    val title: String,
    val isSavedMessages: Boolean
)
```

**AuthUiState** (`/app/src/main/java/com/saiesh/tele/domain/model/auth/AuthUiState.kt`):

```kotlin
data class AuthUiState(
    val apiId: String = "",
    val apiHash: String = "",
    val phone: String = "",
    val code: String = "",
    val password: String = "",
    val step: AuthStep = AuthStep.Loading,
    val message: String? = null,
    val isLoading: Boolean = false
)
```

**Search Models** (`/app/src/main/java/com/saiesh/tele/domain/model/search/SearchModels.kt`):
- `SearchQueryResult`: Represents a search result with callback data
- `SearchUiState`: Search screen state
- `SearchBotResponse`: Sealed class for bot response types (Results, Media, Error)

---

## Build System

### Gradle Configuration

**Root build.gradle.kts** (`/home/syndicate/Tele/build.gradle.kts`):
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

**App build.gradle.kts** (`/home/syndicate/Tele/app/build.gradle.kts`):
- **Namespace**: `com.saiesh.tele`
- **Compile SDK**: 36
- **Min SDK**: 23
- **Target SDK**: 36
- **NDK ABI Filter**: armeabi-v7a only
- **Build Config**: Enabled for API keys injection

### Dependencies (libs.versions.toml)

**File**: `/home/syndicate/Tele/gradle/libs.versions.toml`

| Library | Version | Purpose |
|---------|---------|---------|
| AGP | 9.0.0 | Android Gradle Plugin |
| Kotlin | 2.0.21 | Programming language |
| Core KTX | 1.17.0 | Android core extensions |
| AppCompat | 1.6.1 | Backward compatibility |
| Lifecycle | 2.10.0 | ViewModel and lifecycle |
| Coroutines | 1.8.1 | Async programming |
| Activity KTX | 1.12.2 | Activity extensions |
| Fragment KTX | 1.8.2 | Fragment extensions |
| RecyclerView | 1.3.2 | List views |
| Leanback | 1.2.0 | Android TV UI library |
| Glide | 4.11.0 | Image loading |

### Build Variants

Only **debug** and **release** variants are configured. ProGuard is disabled (`isMinifyEnabled = false`).

### Signing Configuration

Release builds require signing configuration from `local.properties` or environment variables:
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

---

## Security

### API Key Handling

API credentials are handled through multiple sources (in priority order):

1. **SharedPreferences** (User-entered, stored via ApiCredentialsStore)
2. **BuildConfig** (Build-time from local.properties - for development only)
3. **Fallback**: Empty values trigger EnterApiKeys auth step

**ApiCredentialsStore** (`/app/src/main/java/com/saiesh/tele/data/store/ApiCredentialsStore.kt`):
```kotlin
class ApiCredentialsStore(context: Context) {
    private val prefs = context.getSharedPreferences("telegram_api_credentials", Context.MODE_PRIVATE)
    
    fun getApiId(): String? = prefs.getString("api_id", null)
    fun getApiHash(): String? = prefs.getString("api_hash", null)
    fun save(apiId: String, apiHash: String) { ... }
}
```

**BuildConfig Injection** (app/build.gradle.kts):
```kotlin
val apiId = properties.getProperty("TELEGRAM_API_ID") ?: ""
val apiHash = properties.getProperty("TELEGRAM_API_HASH") ?: ""
buildConfigField("String", "TELEGRAM_API_ID", ""$apiId"")
buildConfigField("String", "TELEGRAM_API_HASH", ""$apiHash"")
```

### Release APK Security

- **NO API keys** are baked into release APKs
- Users must enter their own API credentials
- APK is signed with release keystore for distribution
- **Verification**: APK strings contain field names but NOT actual key values

### Authentication Flow

- Uses TDLib's official authentication state machine
- Supports 2FA (two-factor authentication)
- Phone number, verification code, and password are never stored locally
- TDLib manages secure session storage in its database

### Data Storage

- **TDLib Database**: `/data/data/com.saiesh.tele/files/tdlib/` (encrypted by TDLib)
- **TDLib Files**: `/data/data/com.saiesh.tele/files/tdlib_files/` (downloaded files)
- **API Credentials**: Stored in SharedPreferences (MODE_PRIVATE)
- **Images**: LruCache for thumbnails (in-memory only)

---

## External Integrations

### TDLib (Telegram Database Library)

- **Version**: Git commit `cb863c1600082404428f1a84e407b866b9d412a8`
- **Native Library**: `libtdjni.so` (included for armeabi-v7a)
- **Java Bindings**: Client.java and TdApi.java (official TDLib Java bindings)

### Telegram Bots

The app integrates with two Telegram bots:

1. **FileToLinkV5Bot** (`BOT_USERNAME` in `SavedMessagesRepository.kt`):
   - Provides fast download links for media
   - Used when user wants to play a video

2. **ProSearchM11Bot** (`PRO_SEARCH_BOT_USERNAME`):
   - Provides search functionality for movies/media
   - Returns inline keyboard with search results
   - Supports pagination

### External Video Player

The app launches external video players for playback:
- **Primary**: MPV (`is.xyz.mpv.MPVActivity`) - preferred for best Android TV experience
- **Fallback**: Any app that can handle `video/*` Intent

---

## Development Setup

### Prerequisites

1. **Android Studio**: Latest stable version
2. **JDK**: 17 (as configured in GitHub Actions)
3. **Android SDK**: API 36
4. **Telegram API Credentials**: Get from https://my.telegram.org/apps

### Setup Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/saieshshirodkar/Tele.git
   cd Tele
   ```

2. **Create local.properties**:
   ```properties
   sdk.dir=/path/to/Android/Sdk
   TELEGRAM_API_ID=your_api_id_here
   TELEGRAM_API_HASH=your_api_hash_here
   ```

3. **Build and install**:
   ```bash
   ./gradlew installDebug
   ```

### Building Release

For release builds, add signing configuration to `local.properties`:
```properties
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=tele
RELEASE_KEY_PASSWORD=your_key_password
```

Then build:
```bash
./gradlew assembleRelease
```

### Testing Approach

- **Manual Testing**: Primary method via Android TV emulator or physical device
- **No Unit Tests**: The project currently has no automated test suite
- **Integration Testing**: Requires valid Telegram account and API credentials

---

## Release Process

### GitHub Actions Workflow

**File**: `.github/workflows/release-apk.yml`

**Triggers**:
- Manual dispatch (`workflow_dispatch`)
- Push of version tags (`v*`)

**Workflow Steps**:

1. **Checkout**: Uses `actions/checkout@v4`
2. **Set up JDK**: Temurin distribution, Java 17, Gradle caching
3. **Decode Keystore**: Decodes base64 keystore from `secrets.RELEASE_KEYSTORE_BASE64`
4. **Create local.properties**: Writes signing configuration from secrets
5. **Build APK**: `./gradlew assembleRelease`
6. **Rename APK**: Strips `v` prefix from tag name (e.g., `v1.0.0` → `Tele-1.0.0.apk`)
7. **Upload Release**: Uses `softprops/action-gh-release@v2` to attach APK to release

**Required Secrets**:
- `RELEASE_KEYSTORE_BASE64`: Base64-encoded release keystore
- `RELEASE_STORE_PASSWORD`: Keystore password
- `RELEASE_KEY_PASSWORD`: Key password

### Release Checklist

1. Update version in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2
   versionName = "0.0.2"
   ```

2. Commit and push changes

3. Create and push tag:
   ```bash
   git tag -a v0.0.2 -m "Release version 0.0.2"
   git push origin v0.0.2
   ```

4. GitHub Actions automatically builds and creates release

5. Download APK from GitHub Releases page

### Verification

The released APK:
- Is signed with release keystore
- Contains NO hardcoded API keys
- Users enter their own credentials on first launch
- Can be verified with: `apksigner verify --print-certs <apk>`

---

## File Structure

```
/home/syndicate/Tele/
├── .github/workflows/release-apk.yml    # CI/CD workflow
├── app/
│   ├── build.gradle.kts                  # App build configuration
│   └── src/main/
│       ├── AndroidManifest.xml           # App manifest
│       ├── java/com/saiesh/tele/
│       │   ├── app/
│       │   │   ├── MainActivity.kt       # Main activity
│       │   │   └── TeleApp.kt            # Application class
│       │   ├── core/tdlib/
│       │   │   ├── client/
│       │   │   │   └── TdLibClient.kt    # TDLib client wrapper
│       │   │   └── auth/
│       │   │       └── TelegramAuthManager.kt  # Auth manager
│       │   ├── data/
│       │   │   ├── cache/image/
│       │   │   │   └── ImageCache.kt     # LRU image cache
│       │   │   ├── repository/media/     # Repository implementations
│       │   │   │   ├── SavedMessagesRepository.kt
│       │   │   │   ├── SavedMessagesMediaMapper.kt
│       │   │   │   ├── SavedMessagesPaging.kt
│       │   │   │   ├── SavedMessagesChats.kt
│       │   │   │   ├── SavedMessagesMediaSearch.kt
│       │   │   │   ├── SavedMessagesSearch.kt
│       │   │   │   ├── SavedMessagesThumbnail.kt
│       │   │   │   ├── SavedMessagesFastLink.kt
│       │   │   │   └── SavedMessagesDelete.kt
│       │   │   └── store/
│       │   │       └── ApiCredentialsStore.kt  # SharedPreferences wrapper
│       │   ├── domain/model/
│       │   │   ├── auth/
│       │   │   │   ├── AuthStep.kt       # Auth state enum
│       │   │   │   └── AuthUiState.kt    # Auth UI state
│       │   │   ├── media/
│       │   │   │   ├── MediaItem.kt      # Media domain model
│       │   │   │   ├── MediaType.kt      # Photo/Video enum
│       │   │   │   ├── MediaUiState.kt   # Media UI state
│       │   │   │   └── VideoChatItem.kt  # Chat domain model
│       │   │   └── search/
│       │   │       └── SearchModels.kt   # Search models
│       │   └── presentation/
│       │       ├── auth/
│       │       │   ├── ui/
│       │       │   │   └── AuthFragment.kt
│       │       │   └── vm/
│       │       │       └── AuthViewModel.kt
│       │       ├── media/
│       │       │   ├── ui/
│       │       │   │   ├── BrowseFragment.kt
│       │       │   │   ├── MediaContextMenuDialogFragment.kt
│       │       │   │   ├── MediaDetailsDialogFragment.kt
│       │       │   │   └── ConfirmDeleteDialogFragment.kt
│       │       │   ├── presenter/
│       │       │   │   ├── MediaCardPresenter.kt
│       │       │   │   └── VideoChatPresenter.kt
│       │       │   └── vm/
│       │       │       └── MediaViewModel.kt
│       │       └── search/
│       │           ├── ui/
│       │           │   └── SearchFragment.kt
│       │           ├── adapter/
│       │           │   └── SearchResultsAdapter.kt
│       │           └── vm/
│       │               └── SearchViewModel.kt
│       ├── java/org/drinkless/tdlib/     # TDLib Java bindings
│       │   ├── Client.java
│       │   └── TdApi.java
│       ├── jniLibs/armeabi-v7a/
│       │   └── libtdjni.so               # TDLib native library
│       └── res/                          # Android resources
│           ├── layout/
│           ├── drawable/
│           ├── values/
│           └── drawable-nodpi/
├── ai/
│   ├── llm.txt                           # This file (plain text)
│   └── llm.md                            # Markdown version
├── gradle/libs.versions.toml            # Dependency version catalog
├── build.gradle.kts                      # Root build script
├── settings.gradle.kts                   # Project settings
├── gradle.properties                     # Gradle configuration
├── local.properties                      # Local SDK and API keys (NOT in git)
├── README.md                             # User documentation
├── LICENSE                               # MIT License
└── .gitignore                            # Git ignore rules
```

---

## Quick Reference

### Common Questions and Answers

| Question | Answer |
|----------|--------|
| **What is this app?** | Tele is an Android TV app for browsing and playing media from Telegram chats. |
| **How does authentication work?** | Multi-step flow: API keys → Phone number → Verification code → Optional 2FA → Ready state. |
| **Are API keys included in the APK?** | **NO**. Release APKs contain no hardcoded API keys. Users enter their own. |
| **What architecture pattern is used?** | Layered architecture: UI → Domain → Data → Core (TDLib). |
| **How is media loaded?** | Paged loading from chat history via SavedMessagesRepository and TDLib. |
| **What external services are used?** | Telegram (via TDLib), FileToLinkV5Bot for fast links, ProSearchM11Bot for search. |
| **How are releases made?** | GitHub Actions workflow triggered by git tags (v*). |
| **What is the entry point?** | TeleApp.kt loads TDLib, MainActivity.kt hosts fragments. |
| **How is data cached?** | LruCache for images, TDLib manages its own database. |
| **Can I modify this?** | Yes, MIT License. Add your API keys to local.properties and build. |

### Key File Locations

| Component | File Path |
|-----------|-----------|
| Application Entry | `app/src/main/java/com/saiesh/tele/app/TeleApp.kt` |
| Main Activity | `app/src/main/java/com/saiesh/tele/app/MainActivity.kt` |
| Authentication Manager | `app/src/main/java/com/saiesh/tele/core/tdlib/auth/TelegramAuthManager.kt` |
| TDLib Client | `app/src/main/java/com/saiesh/tele/core/tdlib/client/TdLibClient.kt` |
| Main Repository | `app/src/main/java/com/saiesh/tele/data/repository/media/SavedMessagesRepository.kt` |
| API Key Storage | `app/src/main/java/com/saiesh/tele/data/store/ApiCredentialsStore.kt` |
| Media UI | `app/src/main/java/com/saiesh/tele/presentation/media/ui/BrowseFragment.kt` |
| Auth UI | `app/src/main/java/com/saiesh/tele/presentation/auth/ui/AuthFragment.kt` |
| Build Config | `app/build.gradle.kts` |
| Dependencies | `gradle/libs.versions.toml` |
| CI/CD Workflow | `.github/workflows/release-apk.yml` |

### Tech Stack Summary

```
Language:           Kotlin (with Java for TDLib)
Platform:           Android TV (Leanback)
Min SDK:            23 (Android 6.0)
Target SDK:         36 (Android 16)
Architecture:       armeabi-v7a
Async:              Coroutines + Flow
Image Loading:      Glide 4.11.0
Telegram:           TDLib (native)
Build:              Gradle (Kotlin DSL)
License:            MIT
```

---

**Last Updated:** 2026-01-31

This documentation provides complete context about the Tele Android TV app. For questions or issues, refer to the README.md or contact the author.
