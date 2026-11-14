# Dark Fusion Launcher

A customizable Android launcher built with Kotlin and Jetpack Compose, featuring a dark cosmic interface and a floating-style app drawer.

## Features

### Home screen
- Swipeable home pages with page indicators and configurable page count
- Custom dock with adjustable icon count, visibility, and transparency
- App shortcuts, folders, favorites, and drag-to-reorder
- Home clock and date
- Native Android widget hosting, widget sizing, reordering, and visibility controls
- Custom wallpaper with dimming, blur, and optional parallax
- Home background themes and page transition effects

### App drawer
- Floating 3D-inspired app drawer with search and category filters
- App sorting, custom folders, and hidden-app category
- Icon shape, background, and animation preferences
- Icon pack support with fallback to installed app icons
- Optional app labels and adjustable grid columns

### Personalization and gestures
- Home icon size, shape, label size, and label color controls
- Icon shadow and glow settings
- Configurable swipe-up, swipe-down, and double-tap actions
- Persistent preferences for launcher customization
- App context actions for favorites, hide/show, and Android app info

## Build

The project uses JDK 17, Gradle 8.10.2, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, and Jetpack Compose.

GitHub Actions builds a debug APK for pushes to `main` and `feature/pro-launcher-workspace`, and for pull requests targeting `main`.

## Download

Open the repository's **Actions** tab, select a successful **Android Build** run, and download the `dark-fusion-debug-apk` artifact. Extract the ZIP to get `app-debug.apk`.

The debug APK is suitable for testing and personal installation. A separately signed release build should be prepared before public distribution.

## Android launcher setup

Install the APK, open Dark Fusion, and use Android's default Home app settings to select it as the default launcher.
