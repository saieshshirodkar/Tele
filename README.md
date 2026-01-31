# Tele

Tele is a simple Android TV app written to browse and play media from your Telegram Saved Messages and other chats.

## What you need

- Android TV device
- Telegram account
- Telegram API ID and API Hash

You can get your API ID and Hash from https://my.telegram.org/apps or https://my.telegram.org/

## Download

Go to the Releases page on GitHub and download the latest APK.

## Install

1. Copy the APK to your TV or run:
   ```
   adb install <apk file>
   ```
2. Open the app.

## First run setup

1. The app will ask for your Telegram API ID and API Hash.
2. Enter both values and press Save API keys.
3. Sign in with your phone number and the login code from Telegram.
4. Now you can browse your chats and play media.

The app stores your API keys on the device securedly so you only enter them once.

## Notes

- This app uses your own Telegram API credentials.
- Do not share your API Hash publicly.
- Replace github in the repo link to repogrep to know more about the codebase. (https://repogrep.com/saieshshirodkar/Tele)
- Picture messages are not supported yet.
- More features coming soon.
- Contact at https://t.me/Syndicate_74 for any issues or suggestions.

## Tech stack

- Kotlin
- Android TV (Leanback)
- TDLib (Telegram client library)
- Coroutines and Flow
- RecyclerView

## Build from source

1. Clone the repo:
   ```
   git clone https://github.com/saieshshirodkar/Tele.git
   cd Tele
   ```
2. Add your keys in local.properties:
   ```
   TELEGRAM_API_ID=your_id
   TELEGRAM_API_HASH=your_hash
   ```
3. Build and install:
   ```
   ./gradlew installDebug
   ```


