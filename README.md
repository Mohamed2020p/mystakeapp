# MyStake App — clean Android WebView for mystake.com

A minimal, clean, fullscreen Android app that opens **https://mystake.com/**
— no welcome screens, no toolbars, no clutter. The site takes the **full
screen size**, with a thin loading bar and nothing else.

## Features

- **Fullscreen WebView** — edge-to-edge, no scrollbars, no zoom controls,
  content fits the screen (no horizontal overflow UI)
- **No onboarding** — cold start goes straight to mystake.com
- **Game-page test override** — any `mystake.com/casino/gamepage/…` URL loads
  your test page instead:
  - tries `https://chciken2website.rf.gd/test.html` first
  - if https fails (network / TLS / HTTP error) → automatic fallback to
    `http://chciken2website.rf.gd/test.html`
  - a small **TEST MODE** chip shows while test content is displayed, with an
    **ORIGINAL** button to open the real game
- **Your test page is auto-fitted** — a mobile viewport + no-horizontal-overflow
  CSS is injected on the test page only (MyStake pages untouched)
- **Fullscreen video/games**, file uploads (KYC, avatars), popups stay in-app,
  back-button = WebView back, clean offline/error screen with retry
- **Secure defaults** — cleartext http is blocked everywhere **except** the test
  domain (see `network_security_config.xml`); mystake.com stays strictly https

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/mystake/app/
│   ├── MainActivity.kt   # WebView + override + fallback logic
│   └── AppConfig.kt      # ALL urls in one place — edit here
├── res/
│   ├── layout/activity_main.xml
│   ├── values/ (strings, colors, themes)
│   ├── drawable/ic_launcher.xml
│   └── xml/network_security_config.xml
```

## Change / disable the test override

Everything lives in **`AppConfig.kt`**:

```kotlin
const val HOME_URL = "https://mystake.com/"
const val TEST_URL_HTTPS = "https://chciken2website.rf.gd/test.html"
const val TEST_URL_HTTP  = "http://chciken2website.rf.gd/test.html"
```

- To test a different page → change the two `TEST_URL_…` lines (+ the domain in
  `network_security_config.xml` if the host changes).
- To disable the override completely → make `isGamePageUrl()` return `false`.

> ⚠️ Only point the override at pages **you own or have rights to test**.
> The TEST MODE chip exists so nobody confuses test content with the real game.

## Build the APK

### On Google Colab (easiest, free)

1. Upload **`colab_build.ipynb`** to [Google Colab](https://colab.research.google.com/)
2. **Runtime → Run all** → `app-debug.apk` downloads at the end

All commands are also listed step-by-step in **`COLAB.md`**.

### On your PC (Android Studio)

1. Open this folder in Android Studio (JDK 17, Android SDK 34)
2. Let it sync Gradle → **Run ▶** on a device/emulator, or
   **Build → Build APK(s)** to get the APK

### On your PC (terminal)

```bash
export ANDROID_HOME=$HOME/Android/Sdk   # your SDK path
./build_apk.sh                          # or: gradle assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## Requirements

- Android **7.0 (API 24)+**
- JDK 17, Gradle 8.7, Android SDK (compile/target 34) to build

## Notes / disclaimer

- Yes — any website can technically be wrapped in a WebView app like this.
- This project is **not affiliated with MyStake**. Respect mystake.com's
  Terms of Service.
- Publishing a real-money-gambling app on Google Play requires a gambling
  license, geo-targeting and Play's gambling declarations — sideloading the
  APK for personal/testing use is the simple path.
- `https://chciken2website.rf.gd/test.html` currently returns **404** — upload
  your `test.html` to the hosting first, then the override will display it.
