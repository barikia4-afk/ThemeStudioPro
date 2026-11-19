# Theme Studio Pro 🎨

A modern, production-grade Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **One UI Glassmorphism** aesthetics. Engineered specifically to bring external icon pack support, guaranteed 100% icon coverage, shape masking, and deep color grading to **Samsung Theme Park** and modern Android launchers.

---

## 🚀 Key Highlights & Architecture

1. **Automatic Icon Pack Discovery (`PackDiscovery.kt`)**
   - Automatically queries Android 11+ `<queries>` intents for installed icon packs (Nova, ADW, Go, Apex, Smart, etc.).
   - Live updates via `BroadcastReceiver` when packs are installed or uninstalled from Google Play while the app is running.

2. **Guaranteed 100% App Coverage (`Matcher.kt`)**
   - Priority 1: Per-app custom override (gallery or pack search).
   - Priority 2: Selected pack exact component match (`ComponentInfo{pkg/cls}`).
   - Priority 3: Selected pack package match (`pkg`).
   - Priority 4: Secondary user-ordered packs.
   - Priority 5: Fuzzy normalized token matching (`whatsapp`, `google_maps`, etc.).
   - Priority 6: **Generated Fallback** (Renders original icon into pack's `iconback`/`iconmask` plate or extracts dynamic dominant color). **Always succeeds with 0 missing icons.**

3. **Icon Pack Studio-Level Customizer (`Renderer.kt` & `IconEditorScreen.kt`)**
   - **Shapes:** Square, Rounded Square (0-50% radius), Circle, Squircle (Superellipse $|x|^n + |y|^n = 1$ with $n \in [2.0, 6.0]$), Teardrop.
   - **Live ColorMatrix Math:** Hue (-180°..180°), Saturation (-100..100), Brightness (-100..100), Contrast (-100..100), Opacity (0..100), Monochrome, Tint/Duotone, Grayscale, Invert.
   - **Hold to Compare:** Real-time preview against original unadjusted icons.

4. **Samsung Theme Park & Good Lock Automation (`ThemeParkPilotService.kt`)**
   - Generates installable icon-pack APKs matching Theme Park structure.
   - Safe, offline Accessibility pilot service to automate applying icon sets inside Theme Park without external network calls or fragile licensing checks.
   - Prevents "Plate inside plate" warnings by guiding users to keep Theme Park icon style on Default.

---

## 🛠️ Build & Development Instructions

### Requirements
- Android Studio Ladybug / Koala or CLI Gradle 8.x
- JDK 17
- `minSdk`: 26 (Android 8.0 Oreo)
- `compileSdk` / `targetSdk`: 35 (Android 15 & 16 / One UI 8 compatible)

### Compiling Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Permissions Breakdown

| Permission | Purpose |
|:---|:---|
| `QUERY_ALL_PACKAGES` | Enumerate all launchable installed apps on the device to guarantee 100% coverage. |
| `REQUEST_INSTALL_PACKAGES` | Allows prompt to install the generated icon pack APK directly into Android system. |
| `BIND_ACCESSIBILITY_SERVICE` | Optional, opt-in Theme Park Pilot service to automate Theme Park steps. |
| `RECEIVE_BOOT_COMPLETED` | Maintains background watcher consistency. |
