# 📚 Session Tracks

**স্টাডি সেশন ও লেকচার ট্র্যাকার** — a production-ready **native Android** application
(Java + XML View System) that turns a nightly study plan into a trackable,
offline-first workflow: sessions → subjects → chapters → lectures, with bulk
YouTube link import, per-lecture progress ticks, quick notes and `.json` backups.

> ⚠️ This repository also contains an earlier **React/Vite web prototype** of the
> same idea (`src/`, `index.html`, `vite.config.ts`). The native Android app in
> `app/` is the real product and is entirely independent of it — it is **not** a
> WebView wrapper and contains no HTML/CSS/JS.

---

## ✨ Features

### 1. Dashboard & session management
- Ships with **3 default study sessions** on first launch
  (৭:০০–৮:০০ PM, ৯:০০–১০:০০ PM, ১১:০০–১২:০০ AM).
- **Add New Session** creates sessions dynamically; every session can be renamed,
  re-timed, recoloured and deleted.
- Gradient header showing overall completion %, session/subject/lecture/completed
  counters and the next upcoming session.
- **Live now** badge on the session currently in progress (handles midnight wrap).
- Collapsible session cards with per-session progress bars.
- Undo snackbar restores a deleted session with its entire subtree intact.

### 2. Subject management
- Subject cards live inside a session and can be added/edited/deleted.
- Each subject has its **own study time slot** (e.g. `Physics: 8:00 PM – 9:00 PM`)
  independent of the parent session.
- Colour tag from a 10-colour palette, plus per-subject progress.

### 3. Chapter & lecture flow
- Tapping a subject opens its chapter list (`অধ্যায় ০২: ভেক্টর`).
- Entering **Total Lectures** (e.g. `20`) auto-generates `Lecture 1 … Lecture 20`
  buttons in a responsive grid.
- Changing the count later grows or shrinks the grid **without losing** existing
  links, notes or completion ticks.

### 4. Bulk Link Importer (smart link mapping)
Paste a whole block of links at once; the app parses it and maps each URL to its
lecture button. Supports:
- **Sequential mode** — Nth link becomes Lecture N.
- **Numbered mode** — honours explicit markers such as `Lecture 5`, `05)`,
  `ক্লাস ৭`, `লেকচার ৩`, `Ep 4`, bullets and messy prefixes.
- Bengali **and** ASCII digits, trailing-punctuation cleanup, bare `www.` links.
- Live preview before applying, plus toggles for *overwrite existing links* and
  *auto-expand lecture count*.
- One-tap **paste from clipboard**.

### 5. Study tracking
- **One-tap play** — opens the YouTube app directly (via video id) or an in-app
  Chrome Custom Tab, configurable in Settings.
- **Completion checkbox** — ticking turns the tile green and marks it 'সম্পন্ন'.
- **Progress bar** per chapter, subject, session and overall (`60% Completed`).
- Optional *mark complete when opened*, *mark all complete* and *reset progress*.
- Long-press a lecture for play / toggle / edit link / notes / copy link.

### 6. Quick Notes
- A notes box for **every chapter and every individual lecture**.
- Saved on explicit save *and* on swipe-dismiss, with a live character counter,
  copy-to-clipboard and an indicator icon on items that carry notes.

### 7. Backup & restore (`.json`)
- **ব্যাকআপ ফাইল ডাউনলোড করুন (.json)** — writes the complete tree (sessions,
  subjects, chapters, lectures, links, notes, progress) to a file you choose.
- **ব্যাকআপ ফাইল সিলেক্ট করুন (.json)** — restores it, with a confirmation dialog
  offering **replace everything** or **merge with current data**.
- Uses the **Storage Access Framework** → *no storage permission required*.
- Backups can also be shared through the Android Sharesheet.

### 8. Reminders, theming and locale
- Optional per-session reminder notifications with a configurable lead time,
  rescheduled automatically after reboot, app update or clock/timezone change.
- Light / dark / system theme, বাংলা / English / system language.
- Bengali numeral toggle (`২০` vs `20`) applied consistently across the app.

---

## 📱 Screens

| Screen | Purpose | States handled |
|---|---|---|
| **Dashboard** (`MainActivity`) | Session list, overall stats, search, entry to backup/settings | loading skeleton, empty, no-search-results, error, offline banner |
| **Subject detail** (`SubjectDetailActivity`) | Chapter list + subject progress summary | loading, empty, error |
| **Chapter detail** (`ChapterDetailActivity`) | Lecture grid, progress, lecture-count field, bulk import | empty, error, no-link, offline |
| **Session / Subject / Chapter editors** | Create & edit dialogs with Material time pickers and colour picker | inline field validation |
| **Bulk Link Importer** (bottom sheet) | Paste → parse → preview → apply | none-found, clipboard-empty |
| **Quick Notes** (bottom sheet) | Chapter & lecture notes | empty, char limit |
| **Backup & Restore** (bottom sheet) | `.json` export / import / share | busy, invalid file, failure |
| **Settings** (bottom sheet) | Theme, language, numerals, reminders, playback, data | confirmation dialogs |

---

## 🛠 Technology stack

| Area | Choice |
|---|---|
| Language | **Java 17** |
| UI | **XML View System** (no Compose, no WebView) |
| Architecture | **MVVM + Repository**, LiveData |
| Database | **Room 2.6.1** (SQLite) with foreign-key cascades |
| JSON | **Gson 2.11.0** (backup format only) |
| Design | **Material Components 1.12.0** (Material 3 DayNight) |
| Async | `ExecutorService` (single-threaded disk executor + main-thread dispatcher) |
| Files | Storage Access Framework + `FileProvider` |
| Notifications | `AlarmManager` + `NotificationManagerCompat` |
| Browser | AndroidX Browser (Chrome Custom Tabs) |
| Build | Android Gradle Plugin **8.6.1**, Gradle **8.7** |

**Min SDK 24** (Android 7.0) · **Target/Compile SDK 35** (Android 15) · `versionName 1.0.0`

No networking library is bundled: the app has **no backend**. All data is local,
so everything except video playback works fully offline.

---

## 🔐 Permissions

| Permission | Why it is needed | If denied |
|---|---|---|
| `POST_NOTIFICATIONS` (Android 13+, runtime) | Show study-session reminders | App works normally; a snackbar explains reminders are off |
| `SCHEDULE_EXACT_ALARM` (Android 12+) | Fire the reminder at the exact minute | Falls back to an inexact alarm automatically |
| `RECEIVE_BOOT_COMPLETED` | Re-arm reminders after reboot | Reminders resume next time the app opens |
| `VIBRATE` | Vibration on reminder notifications | Silent notification |

No storage, camera, location, contacts or account permissions are requested.
Backup file access goes through the system file picker instead.

---

## 🏗 Architecture

```
UI (Activities, DialogFragments, BottomSheets)
        │  observes LiveData / sends user intents
ViewModel (Dashboard, Subject, Chapter, Backup)
        │  no Android UI types, survives rotation
StudyRepository  ── single source of truth ──  BackupManager
        │  all writes on a serialized disk executor
Room DAOs (Session, Subject, Chapter, Lecture, Progress)
        │
SQLite (session_tracks.db)
```

- Progress percentages are computed **in SQL** (`ProgressDao`), so the UI never
  loads whole lecture tables just to draw a bar.
- `MediatorLiveData` joins sessions + subjects + progress so ticking one lecture
  updates every percentage above it instantly.
- One-shot UI signals (snackbars, undo) use an `Event<T>` wrapper so they are not
  redelivered after a configuration change.

---

## 🚀 Build instructions

```bash
git clone https://github.com/viphasibul792/smart.study-.git
cd smart.study-

./gradlew assembleDebug     # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
./gradlew lint              # → app/build/reports/lint-results-debug.html
./gradlew testDebugUnitTest # JUnit tests
```

Requirements: **JDK 17**, Android SDK **API 35**, Android Studio Ladybug or newer.

### Signing the release build
The release build type reads its keystore from environment variables and produces
an **unsigned** APK when they are absent:

```bash
export ANDROID_KEYSTORE_PATH=/path/to/release.keystore
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=...
export ANDROID_KEY_PASSWORD=...
./gradlew assembleRelease
```

No credential is ever committed to this repository.

---

## 📥 Installation

1. Go to the [Releases page](https://github.com/viphasibul792/smart.study-/releases/tag/v1.0.0)
   and download an APK:

   | Asset | Size | What it is |
   |---|---|---|
   | **`session-tracks-v1.0.0-release.apk`** | 2.9 MB | **Recommended.** Full Material 3 build (AndroidX + Room), signed |
   | `session-tracks-v1.0.0-debug.apk` | 6.9 MB | Same app, debuggable |
   | `SessionTracks-v1.0.0-release.apk` | 105 KB | Framework-only build — zero dependencies, tiny, plainer look |
   | `SessionTracks-v1.0.0-debug.apk` | 105 KB | Framework-only, debuggable |

2. Open the downloaded file on your Android phone.
3. Allow *Install from unknown sources* if prompted.
4. Launch **Session Tracks** — three study sessions are ready immediately.

> Both variants implement all seven features and share the same package name
> (`com.sessiontracks.app`), so **install only one** — Android will refuse to
> install the second over the first because they are signed with different keys.

---

## 🤖 Continuous integration

`.github/workflows/android.yml` runs `ci/build-apk.sh` on every push: unit
tests, lint, `assembleDebug`, `assembleRelease`, then it attaches every APK to
the `v1.0.0` GitHub Release. Latest run: **✅ success** (`31856346919`).

Each run also commits its own build log to `ci/logs/`, so the output can be read
with a plain `git fetch` without opening the Actions UI.

### Building without a network

`apk-build/` holds a second implementation that depends on **nothing but the
Android framework** — no AndroidX, no Material, no Room, no Gson. It builds with
`aapt2` + any Java compiler + `dx` + `apksigner`:

```bash
cd apk-build && ./build.sh     # -> dist/SessionTracks-v1.0.0-{debug,release}.apk
```

This is what makes an offline/air-gapped build possible; see the header of
`apk-build/build.sh` for the tool paths it expects.

---

## ⚠️ Known limitations

- **Cloud sync is file-based.** The requirement asked for backup *"so links and
  progress are not lost on refresh or another device"*. This is implemented as
  `.json` export/import through the system file picker (which can target Google
  Drive/OneDrive), plus Android auto-backup. There is no account system or
  automatic server sync, because the app deliberately has no backend and stores
  no credentials.
- **Exact-time reminders** need the *Alarms & reminders* permission on Android 12+.
  Without it, reminders still fire but may be delayed by system batching.
- **In-app playback** uses Chrome Custom Tabs rather than an embedded player;
  YouTube's terms do not permit a bare WebView player, and the YouTube app gives
  a better experience.
- The release APK published by CI is signed with a **build-time generated
  keystore**, which is fine for sideloading but must be replaced with a stable
  keystore before any Play Store submission.
- **The APKs have not been launched on a physical device or emulator** — none
  was available in the build environment. The builds, unit tests, resource
  linking and SQL are all verified automatically, but the on-device experience
  has not been eyeballed.
- The framework-only build (`SessionTracks-*.apk`) targets API 27 and uses plain
  `Theme.Material`, so it looks simpler than the Gradle build and skips edge-to-edge
  insets and Custom Tabs; it opens links in the YouTube app or the browser.

---

## 📄 Further documentation

- **[BUILD_APK.md](BUILD_APK.md)** — 📦 **APK কীভাবে তৈরি করবেন** (step-by-step, in Bengali).

- **[HANDOFF.md](HANDOFF.md)** — current state, feature checklist, build commands, how to resume.
- **[APP_SPECIFICATION.md](APP_SPECIFICATION.md)** — the original requirements, kept as the source of truth.

---

## 📜 License

Released for personal/educational use by the repository owner.
