# Tele

Tele is an Android TV app that lets you browse and play videos, photos, and other media from your Telegram chats right on your TV. It works with your Telegram Saved Messages and any other chats you have.

## What you need

- An Android TV device (or Android TV box)
- A Telegram account
- Your own Telegram API ID and API Hash (don't worry, I'll explain how to get these below)

## Getting your API credentials

Before you can use Tele, you need to get your own Telegram API ID and Hash. This is how Telegram knows the app is connecting on your behalf.

1. Go to https://my.telegram.org/apps
2. Log in with your Telegram phone number
3. Click "API development tools"
4. Fill in the form (App title: Tele, Short name: tele, Platform: Android, Description: Android TV client for Telegram)
5. Click "Create application"
6. You'll see your **api_id** and **api_hash** - save these somewhere safe

Keep these private. Don't share your API Hash with anyone.

## Download

Head over to the [Releases](https://github.com/saieshshirodkar/Tele/releases) page and download the latest APK file. Look for something like `Tele-0.0.1.apk`.

## Install

There are two ways to install:

**Option 1: USB or file manager**
- Copy the APK file to a USB drive
- Plug it into your Android TV
- Use a file manager app to find and install it

**Option 2: ADB (for tech-savvy users)**
- Connect your computer to the same network as your TV
- Find your TV's IP address in Settings > Network
- Run this command on your computer:
  ```
  adb connect <your-tv-ip>
  adb install Tele-0.0.1.apk
  ```

## Setting up the app

The first time you open Tele, you'll need to do a quick setup:

1. **Enter API credentials** - The app will ask for your API ID and API Hash. Type in the values you got from my.telegram.org and press Save.

2. **Sign in** - Enter your Telegram phone number (with country code, like +1234567890)

3. **Verify** - Check your Telegram app or phone for a login code and enter it

4. **Done** - You're in! Browse your chats and play media.

The app saves your API keys securely on your TV so you only need to enter them once.

## Things to know

- This app uses **your own** Telegram API credentials, not shared ones
- Picture messages are not fully supported yet (working on it)
- Videos, documents, and other files work great
- Contact me at https://t.me/Syndicate_74 for questions or suggestions
- Want to explore the code? Check it out at https://repogrep.com/saieshshirodkar/Tele

## How it's built

- Kotlin for the code
- Android TV Leanback library for the TV interface
- TDLib (Telegram's official client library)
- Coroutines and Flow for handling data
- RecyclerView for displaying chat lists

## Building it yourself

Want to modify the app or build from source?

1. Clone this repository:
   ```
   git clone https://github.com/saieshshirodkar/Tele.git
   cd Tele
   ```

2. Create a file called `local.properties` and add your API keys:
   ```
   TELEGRAM_API_ID=your_api_id_here
   TELEGRAM_API_HASH=your_api_hash_here
   ```

3. Build and install to your TV:
   ```
   ./gradlew installDebug
   ```

That's it. Happy watching!
