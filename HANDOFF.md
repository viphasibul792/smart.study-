# HANDOFF.md — Session Tracks

---

## 1. Project Overview

| Field | Value |
|---|---|
| **App name** | Session Tracks (স্টাডি সেশন ও লেকচার ট্র্যাকার) |
| **App type** | Native Android application — offline-first study session & lecture tracker |
| **Package name** | `com.sessiontracks.app` (debug variant: `com.sessiontracks.app.debug`) |
| **Java version** | Java 17 (`sourceCompatibility` / `targetCompatibility` = `VERSION_17`) |
| **Android Gradle Plugin** | 8.6.1 |
| **Gradle version** | 8.7 (wrapper committed) |
| **Minimum SDK** | 24 (Android 7.0 Nougat) |
| **Target SDK / Compile SDK** | 35 (Android 15) |
| **Architecture** | MVVM + Repository (LiveData, Room, single-source-of-truth repository) |
| **UI system** | XML View System + Material Components 1.12.0 — **no Compose, no WebView** |
| **Version** | `versionCode 1`, `versionName 1.0.0` |
| **Repository** | https://github.com/viphasibul792/smart.study- |
| **Working branch** | `arena/01a0012c-smart-study` |

---

## 2. Repository Structure

```
smart.study-/
  ├── app/
  │   ├── src/
  │   │   ├── main/
  │   │   │   ├── java/
  │   │   │   │   └── com/
  │   │   │   │       └── sessiontracks/
  │   │   │   │           └── app/
  │   │   │   │               ├── adapter/
  │   │   │   │               │   ├── ChapterAdapter.java
  │   │   │   │               │   ├── ColorSwatchAdapter.java
  │   │   │   │               │   ├── LectureAdapter.java
  │   │   │   │               │   ├── LinkPreviewAdapter.java
  │   │   │   │               │   ├── SessionAdapter.java
  │   │   │   │               │   └── SubjectAdapter.java
  │   │   │   │               ├── data/
  │   │   │   │               │   ├── backup/
  │   │   │   │               │   │   ├── BackupManager.java
  │   │   │   │               │   │   └── BackupModels.java
  │   │   │   │               │   ├── dao/
  │   │   │   │               │   │   ├── ChapterDao.java
  │   │   │   │               │   │   ├── LectureDao.java
  │   │   │   │               │   │   ├── ProgressDao.java
  │   │   │   │               │   │   ├── SessionDao.java
  │   │   │   │               │   │   └── SubjectDao.java
  │   │   │   │               │   ├── entity/
  │   │   │   │               │   │   ├── Chapter.java
  │   │   │   │               │   │   ├── Lecture.java
  │   │   │   │               │   │   ├── StudySession.java
  │   │   │   │               │   │   └── Subject.java
  │   │   │   │               │   ├── model/
  │   │   │   │               │   │   ├── ChapterProgress.java
  │   │   │   │               │   │   ├── ChapterWithProgress.java
  │   │   │   │               │   │   ├── Event.java
  │   │   │   │               │   │   ├── OverallStats.java
  │   │   │   │               │   │   ├── Resource.java
  │   │   │   │               │   │   ├── SessionWithSubjects.java
  │   │   │   │               │   │   ├── SubjectProgress.java
  │   │   │   │               │   │   └── SubjectWithProgress.java
  │   │   │   │               │   ├── repository/
  │   │   │   │               │   │   ├── PreferenceManager.java
  │   │   │   │               │   │   └── StudyRepository.java
  │   │   │   │               │   └── AppDatabase.java
  │   │   │   │               ├── reminder/
  │   │   │   │               │   ├── BootReceiver.java
  │   │   │   │               │   ├── NotificationHelper.java
  │   │   │   │               │   ├── ReminderReceiver.java
  │   │   │   │               │   └── ReminderScheduler.java
  │   │   │   │               ├── ui/
  │   │   │   │               │   ├── chapter/
  │   │   │   │               │   │   └── ChapterDetailActivity.java
  │   │   │   │               │   ├── common/
  │   │   │   │               │   │   ├── BackupSheet.java
  │   │   │   │               │   │   ├── BulkImportSheet.java
  │   │   │   │               │   │   ├── ChapterEditorDialog.java
  │   │   │   │               │   │   ├── NotesSheet.java
  │   │   │   │               │   │   ├── SessionEditorDialog.java
  │   │   │   │               │   │   └── SubjectEditorDialog.java
  │   │   │   │               │   ├── main/
  │   │   │   │               │   │   └── MainActivity.java
  │   │   │   │               │   ├── settings/
  │   │   │   │               │   │   └── SettingsSheet.java
  │   │   │   │               │   └── subject/
  │   │   │   │               │       └── SubjectDetailActivity.java
  │   │   │   │               ├── util/
  │   │   │   │               │   ├── AppExecutors.java
  │   │   │   │               │   ├── BengaliNumerals.java
  │   │   │   │               │   ├── ColorUtils.java
  │   │   │   │               │   ├── IntentUtils.java
  │   │   │   │               │   ├── LinkParser.java
  │   │   │   │               │   ├── NetworkUtils.java
  │   │   │   │               │   ├── NumberFormatter.java
  │   │   │   │               │   ├── TimeUtils.java
  │   │   │   │               │   └── ValidationUtils.java
  │   │   │   │               ├── viewmodel/
  │   │   │   │               │   ├── BackupViewModel.java
  │   │   │   │               │   ├── ChapterViewModel.java
  │   │   │   │               │   ├── DashboardViewModel.java
  │   │   │   │               │   └── SubjectViewModel.java
  │   │   │   │               └── SessionTracksApp.java
  │   │   │   ├── res/
  │   │   │   │   ├── anim/
  │   │   │   │   │   ├── fade_in.xml
  │   │   │   │   │   ├── fade_out.xml
  │   │   │   │   │   ├── item_fall_down.xml
  │   │   │   │   │   ├── layout_animation_fall_down.xml
  │   │   │   │   │   ├── slide_in_left.xml
  │   │   │   │   │   ├── slide_in_right.xml
  │   │   │   │   │   ├── slide_out_left.xml
  │   │   │   │   │   └── slide_out_right.xml
  │   │   │   │   ├── color/
  │   │   │   │   │   ├── checkbox_tint.xml
  │   │   │   │   │   ├── lecture_tile_stroke.xml
  │   │   │   │   │   ├── lecture_tile_text.xml
  │   │   │   │   │   └── text_on_gradient.xml
  │   │   │   │   ├── drawable/
  │   │   │   │   │   ├── bg_bottom_sheet_handle.xml
  │   │   │   │   │   ├── bg_chip_soft.xml
  │   │   │   │   │   ├── bg_color_swatch.xml
  │   │   │   │   │   ├── bg_dashed_outline.xml
  │   │   │   │   │   ├── bg_header_gradient.xml
  │   │   │   │   │   ├── bg_lecture_tile.xml
  │   │   │   │   │   ├── bg_lecture_tile_done.xml
  │   │   │   │   │   ├── bg_lecture_tile_pending.xml
  │   │   │   │   │   ├── bg_offline_banner.xml
  │   │   │   │   │   ├── bg_progress_badge.xml
  │   │   │   │   │   ├── bg_rounded_surface.xml
  │   │   │   │   │   ├── bg_session_color_bar.xml
  │   │   │   │   │   ├── bg_skeleton.xml
  │   │   │   │   │   ├── bg_stat_pill.xml
  │   │   │   │   │   ├── ic_add.xml
  │   │   │   │   │   ├── ic_arrow_back.xml
  │   │   │   │   │   ├── ic_backup.xml
  │   │   │   │   │   ├── ic_book.xml
  │   │   │   │   │   ├── ic_check.xml
  │   │   │   │   │   ├── ic_check_circle.xml
  │   │   │   │   │   ├── ic_chevron_right.xml
  │   │   │   │   │   ├── ic_close.xml
  │   │   │   │   │   ├── ic_content_paste.xml
  │   │   │   │   │   ├── ic_copy.xml
  │   │   │   │   │   ├── ic_delete.xml
  │   │   │   │   │   ├── ic_download.xml
  │   │   │   │   │   ├── ic_edit.xml
  │   │   │   │   │   ├── ic_empty_chapters.xml
  │   │   │   │   │   ├── ic_empty_notes.xml
  │   │   │   │   │   ├── ic_empty_search.xml
  │   │   │   │   │   ├── ic_empty_sessions.xml
  │   │   │   │   │   ├── ic_error.xml
  │   │   │   │   │   ├── ic_expand_more.xml
  │   │   │   │   │   ├── ic_folder_open.xml
  │   │   │   │   │   ├── ic_launcher_background.xml
  │   │   │   │   │   ├── ic_launcher_foreground.xml
  │   │   │   │   │   ├── ic_link.xml
  │   │   │   │   │   ├── ic_list_numbered.xml
  │   │   │   │   │   ├── ic_more_vert.xml
  │   │   │   │   │   ├── ic_note.xml
  │   │   │   │   │   ├── ic_notification.xml
  │   │   │   │   │   ├── ic_notification_small.xml
  │   │   │   │   │   ├── ic_offline.xml
  │   │   │   │   │   ├── ic_play.xml
  │   │   │   │   │   ├── ic_refresh.xml
  │   │   │   │   │   ├── ic_school.xml
  │   │   │   │   │   ├── ic_search.xml
  │   │   │   │   │   ├── ic_settings.xml
  │   │   │   │   │   ├── ic_share.xml
  │   │   │   │   │   ├── ic_time.xml
  │   │   │   │   │   └── ic_upload.xml
  │   │   │   │   ├── layout/
  │   │   │   │   │   ├── activity_chapter_detail.xml
  │   │   │   │   │   ├── activity_main.xml
  │   │   │   │   │   ├── activity_subject_detail.xml
  │   │   │   │   │   ├── dialog_chapter_editor.xml
  │   │   │   │   │   ├── dialog_lecture_count.xml
  │   │   │   │   │   ├── dialog_lecture_link.xml
  │   │   │   │   │   ├── dialog_session_editor.xml
  │   │   │   │   │   ├── dialog_subject_editor.xml
  │   │   │   │   │   ├── item_chapter.xml
  │   │   │   │   │   ├── item_color_swatch.xml
  │   │   │   │   │   ├── item_lecture.xml
  │   │   │   │   │   ├── item_link_preview.xml
  │   │   │   │   │   ├── item_session.xml
  │   │   │   │   │   ├── item_subject.xml
  │   │   │   │   │   ├── partial_dashboard_header.xml
  │   │   │   │   │   ├── partial_empty_state.xml
  │   │   │   │   │   ├── partial_loading_skeleton.xml
  │   │   │   │   │   ├── partial_skeleton_row.xml
  │   │   │   │   │   ├── sheet_backup.xml
  │   │   │   │   │   ├── sheet_bulk_import.xml
  │   │   │   │   │   ├── sheet_notes.xml
  │   │   │   │   │   └── sheet_settings.xml
  │   │   │   │   ├── menu/
  │   │   │   │   │   ├── menu_chapter_detail.xml
  │   │   │   │   │   ├── menu_chapter_item.xml
  │   │   │   │   │   ├── menu_lecture_actions.xml
  │   │   │   │   │   ├── menu_main.xml
  │   │   │   │   │   ├── menu_session_item.xml
  │   │   │   │   │   ├── menu_subject_detail.xml
  │   │   │   │   │   └── menu_subject_item.xml
  │   │   │   │   ├── mipmap-anydpi-v26/
  │   │   │   │   │   ├── ic_launcher.xml
  │   │   │   │   │   └── ic_launcher_round.xml
  │   │   │   │   ├── mipmap-hdpi/
  │   │   │   │   │   ├── ic_launcher.png
  │   │   │   │   │   └── ic_launcher_round.png
  │   │   │   │   ├── mipmap-mdpi/
  │   │   │   │   │   ├── ic_launcher.png
  │   │   │   │   │   └── ic_launcher_round.png
  │   │   │   │   ├── mipmap-xhdpi/
  │   │   │   │   │   ├── ic_launcher.png
  │   │   │   │   │   └── ic_launcher_round.png
  │   │   │   │   ├── mipmap-xxhdpi/
  │   │   │   │   │   ├── ic_launcher.png
  │   │   │   │   │   └── ic_launcher_round.png
  │   │   │   │   ├── mipmap-xxxhdpi/
  │   │   │   │   │   ├── ic_launcher.png
  │   │   │   │   │   └── ic_launcher_round.png
  │   │   │   │   ├── values/
  │   │   │   │   │   ├── arrays.xml
  │   │   │   │   │   ├── colors.xml
  │   │   │   │   │   ├── dimens.xml
  │   │   │   │   │   ├── integers.xml
  │   │   │   │   │   ├── strings.xml
  │   │   │   │   │   ├── styles.xml
  │   │   │   │   │   └── themes.xml
  │   │   │   │   ├── values-en/
  │   │   │   │   │   └── strings.xml
  │   │   │   │   ├── values-land/
  │   │   │   │   │   └── dimens.xml
  │   │   │   │   ├── values-night/
  │   │   │   │   │   ├── colors.xml
  │   │   │   │   │   └── themes.xml
  │   │   │   │   ├── values-sw600dp/
  │   │   │   │   │   └── dimens.xml
  │   │   │   │   └── xml/
  │   │   │   │       ├── backup_rules.xml
  │   │   │   │       ├── data_extraction_rules.xml
  │   │   │   │       ├── file_paths.xml
  │   │   │   │       └── locales_config.xml
  │   │   │   └── AndroidManifest.xml
  │   │   └── test/
  │   │       └── java/
  │   │           └── com/
  │   │               └── sessiontracks/
  │   │                   └── app/
  │   │                       ├── BengaliNumeralsTest.java
  │   │                       ├── LinkParserTest.java
  │   │                       ├── ProgressCalculationTest.java
  │   │                       ├── TimeUtilsTest.java
  │   │                       └── ValidationUtilsTest.java
  │   ├── build.gradle
  │   └── proguard-rules.pro
  ├── github-workflow/
  │   └── android.yml
  ├── gradle/
  │   └── wrapper/
  │       ├── gradle-wrapper.jar
  │       └── gradle-wrapper.properties
  ├── .gitignore
  ├── APP_SPECIFICATION.md
  ├── HANDOFF.md
  ├── README.md
  ├── build.gradle
  ├── gradle.properties
  ├── gradlew
  ├── gradlew.bat
  └── settings.gradle
```

> The repository additionally contains the earlier **React/Vite web prototype**
> (`src/`, `index.html`, `package.json`, `vite.config.ts`, `bun.lock`,
> `metadata.json`, `tsconfig.json`). It is unrelated to the Android build and was
> kept at the owner's request; the native app does not reference it in any way.

### Java package layout

| Package | Contents |
|---|---|
| `com.sessiontracks.app` | `SessionTracksApp` (theme/locale/seed/reminder bootstrap) |
| `.adapter` | `SessionAdapter`, `SubjectAdapter`, `ChapterAdapter`, `LectureAdapter`, `ColorSwatchAdapter`, `LinkPreviewAdapter` (all `ListAdapter` + `DiffUtil`) |
| `.data` | `AppDatabase` (Room) |
| `.data.entity` | `StudySession`, `Subject`, `Chapter`, `Lecture` |
| `.data.dao` | `SessionDao`, `SubjectDao`, `ChapterDao`, `LectureDao`, `ProgressDao` |
| `.data.model` | `SessionWithSubjects`, `SubjectWithProgress`, `ChapterWithProgress`, `ChapterProgress`, `SubjectProgress`, `OverallStats`, `Resource`, `Event` |
| `.data.repository` | `StudyRepository`, `PreferenceManager` |
| `.data.backup` | `BackupManager`, `BackupModels` |
| `.viewmodel` | `DashboardViewModel`, `SubjectViewModel`, `ChapterViewModel`, `BackupViewModel` |
| `.ui.main` | `MainActivity` |
| `.ui.subject` | `SubjectDetailActivity` |
| `.ui.chapter` | `ChapterDetailActivity` |
| `.ui.common` | `SessionEditorDialog`, `SubjectEditorDialog`, `ChapterEditorDialog`, `BulkImportSheet`, `NotesSheet`, `BackupSheet` |
| `.ui.settings` | `SettingsSheet` |
| `.reminder` | `ReminderScheduler`, `ReminderReceiver`, `BootReceiver`, `NotificationHelper` |
| `.util` | `LinkParser`, `BengaliNumerals`, `NumberFormatter`, `TimeUtils`, `ValidationUtils`, `IntentUtils`, `NetworkUtils`, `ColorUtils`, `AppExecutors` |

---

## 3. Current State

### Current version
`1.0.0` (`versionCode 1`) — first complete implementation.

### Build status

| Check | Status | Notes |
|---|---|---|
| Java syntax / parse (56 main + 5 test files) | ✅ Verified | All files parsed with a Java grammar parser |
| XML well-formedness (111 files) | ✅ Verified | Manifest + all resources |
| Resource reference integrity | ✅ Verified | Every `@drawable/@string/@color/@dimen/@style/@layout/@menu/@anim/@array/@integer` and `R.*` reference resolves |
| `findViewById` ↔ layout IDs | ✅ Verified | All IDs resolve within each screen's layout (includes followed) |
| String format arity (132 `getString` sites) | ✅ Verified | Argument counts match placeholders in **both** locales |
| Locale parity (bn ↔ en) | ✅ Verified | 0 format-signature mismatches |
| DAO SQL (51 queries) | ✅ Verified | Executed against a real SQLite engine on the generated schema |
| Interface implementations | ✅ Verified | No missing overrides |
| Unused/dangling imports | ✅ Verified | 0 |
| Unused styles / dead resources | ✅ Verified | 0 orphaned styles remain |
| **Java 17 compilation** (framework-free classes) | ✅ **Verified** | `LinkParser`, `BengaliNumerals`, `NumberFormatter`, `StudySession`, all progress models + `Resource`/`Event` compiled with the Eclipse batch compiler at `-17`, zero errors |
| **Unit tests actually executed** | ✅ **23/23 passing** | The real `LinkParserTest`, `BengaliNumeralsTest` and `ProgressCalculationTest` sources were run unmodified against the compiled production classes |
| `./gradlew assembleDebug` | ⚠️ **Not run in this environment** | See below |
| `./gradlew assembleRelease` | ⚠️ **Not run in this environment** | See below |
| `./gradlew lint` | ⚠️ **Not run in this environment** | See below |

> **Why Gradle was not executed here.** The build sandbox has **no Android SDK**,
> and `dl.google.com`, `maven.google.com`, `services.gradle.org` and
> `repo1.maven.org` are all network-blocked, so the Android toolchain and the
> AndroidX/Material dependencies could not be downloaded.
>
> To get as close to a real build as possible, a JRE and the Eclipse batch Java
> compiler were obtained from reachable mirrors, and every class that does not
> depend on the Android framework was **genuinely compiled at Java 17 and its
> tests executed (23/23 green)**. The remaining classes (Activities, Room DAOs,
> adapters) were validated by full-parse, resource-resolution, `findViewById`,
> SQL-execution and format-arity analysis rather than compilation.
>
> **Therefore: the Gradle/AAPT/Room-annotation-processing build is still
> unverified.** Run the CI workflow (§5) and record the real result here before
> claiming a green build.

### What works
- Full session → subject → chapter → lecture CRUD with cascade deletes.
- Three default sessions seeded on first launch; restorable from Settings.
- Auto-generated lecture grid from a Total Lectures count, growing/shrinking
  without data loss.
- Bulk Link Importer with sequential and Lecture-N mapping, live preview,
  overwrite/expand toggles and clipboard paste.
- One-tap playback (YouTube app or Chrome Custom Tab), completion checkboxes,
  progress bars at chapter/subject/session/overall level.
- Quick Notes on chapters and individual lectures, saved on dismiss.
- `.json` backup export/import/share through the Storage Access Framework.
- Session reminder notifications, rescheduled after boot/update/clock change.
- Search, light/dark themes, বাংলা/English, Bengali numerals, tablet/landscape
  layouts, loading/empty/error/offline states.

### What does not work / not implemented
- **No account-based cloud sync** — backup is file-based by design (see §7).
- **No embedded video player** — playback is delegated to YouTube/Custom Tabs.
- **No Play Store signing key** — CI generates an ephemeral keystore.
- **Instrumented (Espresso) tests are not written**; only JVM unit tests exist.
- The GitHub Actions workflow currently lives at `github-workflow/android.yml`
  and must be moved to `.github/workflows/` once (see §5).

---

## 4. Feature Checklist

Legend: ✅ Implemented · ⚠️ Partial / platform limitation · ❌ Not implemented

### 1. মূল ড্যাশবোর্ড ও সেশন ম্যানেজমেন্ট
| Requirement | Native implementation | Status |
|---|---|---|
| ডিফল্ট ৩টি সেশন (৭–৮, ৯–১০, ১১–১২ PM) | `StudyRepository.seedDefaultsIfEmpty()` seeds on first launch only | ✅ |
| "Add New Session" বাটন | Extended FAB + empty-state action → `SessionEditorDialog` | ✅ |
| নতুন সেশন তৈরি | `createSession()` → Room insert | ✅ |
| সেশনের নাম পরিবর্তন | `SessionEditorDialog` edit mode | ✅ |
| সেশনের সময় আপডেট | `MaterialTimePicker`, 12/24h aware, midnight-wrap safe | ✅ |
| সেশন মুছে ফেলা | Confirm dialog + cascade delete + **undo snackbar** | ✅ |

### 2. বিষয় ব্যবস্থাপনা
| Requirement | Native implementation | Status |
|---|---|---|
| সেশনের ভেতরে সাবজেক্ট কার্ড | Nested `RecyclerView` (`SubjectAdapter`) inside each session card | ✅ |
| বিষয় যুক্ত করা | `SubjectEditorDialog` from FAB, session menu or inline button | ✅ |
| বিষয় মুছে ফেলা | Overflow menu + confirmation, cascades to chapters/lectures | ✅ |
| কাস্টম সময় সূচি (Physics: ৮–৯ PM) | `Subject.startMinute/endMinute`, editable, `-1` inherits session | ✅ |

### 3. অধ্যায় ও লেকচার ম্যানেজমেন্ট
| Requirement | Native implementation | Status |
|---|---|---|
| বিষয়ে ক্লিক → অধ্যায় পেজ | `SubjectDetailActivity` with chapter list | ✅ |
| অধ্যায় শিরোনাম (অধ্যায় ০২: ভেক্টর) | `Chapter.number` + `Chapter.name`, `chapter_label_named` | ✅ |
| "Total Lectures" ইনপুট | Field in chapter editor **and** inline on the chapter screen | ✅ |
| স্বয়ংক্রিয় Lecture 1…20 বাটন | `syncLectureRows()` + `GridLayoutManager` (3/5 span by size) | ✅ |
| সংখ্যা বদলালে ডেটা না হারানো | Grow fills gaps only; shrink confirms and trims the tail | ✅ |
| বাল্ক লিঙ্ক পেস্ট বক্স | `BulkImportSheet` multiline input + clipboard paste | ✅ |
| স্বয়ংক্রিয় টেক্সট পার্স | `LinkParser` — URL extraction, junk trimming, `www.` normalisation | ✅ |
| প্রতিটি লিঙ্ক নিজ বাটনে ম্যাপ | Sequential **and** "Lecture N" numbered mapping (bn + en keywords, Bengali digits) with live preview | ✅ |

### 4. স্টাডি ট্র্যাকিং ও ইন্টারঅ্যাকশন
| Requirement | Native implementation | Status |
|---|---|---|
| এক ক্লিকে YouTube App-এ প্লে | `vnd.youtube:<id>` intent with browser fallback | ✅ |
| ইন-অ্যাপ ব্রাউজারে প্লে | Chrome Custom Tabs, toggled in Settings | ✅ |
| কমপ্লিশন চেকবক্স, সবুজ টিক | `MaterialCheckBox` tinted `success_600` | ✅ |
| বাটনের রঙ বদলে 'সম্পন্ন' | `state_activated` selector → green fill/stroke/text | ✅ |
| প্রোগ্রেস বার (60% Completed) | SQL aggregates → chapter, subject, session and overall bars | ✅ |
| কুইক নোটস (অধ্যায়) | `NotesSheet` `TARGET_CHAPTER` + indicator icon | ✅ |
| কুইক নোটস (লেকচার) | `NotesSheet` `TARGET_LECTURE` via long-press | ✅ |
| ব্যাকআপ ফাইল ডাউনলোড (.json) | `BackupManager.exportTo()` via SAF `CreateDocument` | ✅ |
| ব্যাকআপ ফাইল সিলেক্ট (.json) | `BackupManager.inspect()`/`restore()` via SAF `OpenDocument`, replace **or** merge | ✅ |
| রিফ্রেশে ডেটা না মোছা | Room persistence + Android auto-backup rules | ✅ |
| অন্য ডিভাইসে ডেটা নেওয়া | Via `.json` file (shareable to Drive etc.) — **no account sync** | ⚠️ |

### Mandated technical requirements
| Requirement | Status |
|---|---|
| 100% Native Java + XML, no WebView/HTML/CSS/JS core | ✅ |
| Modern architecture (MVVM + Repository) | ✅ |
| Room database / structured local storage | ✅ |
| SharedPreferences for settings | ✅ |
| RecyclerView lists and grids | ✅ |
| Material components, dialogs, bottom sheets, toolbar | ✅ |
| Loading / empty / error / offline states | ✅ |
| Skeleton loading placeholders | ✅ |
| Light + dark theme (`values-night`) | ✅ |
| Responsive: `sw600dp` tablet, `values-land` | ✅ |
| Accessibility: content descriptions, 48dp targets, sp text, font scaling | ✅ |
| Centralised colors/dimens/strings/styles/themes | ✅ |
| Runtime permissions handled with graceful denial | ✅ |
| No main-thread I/O (StrictMode in debug) | ✅ |
| Deep links (`sessiontracks://`) | ✅ |
| Android Sharesheet + Clipboard | ✅ |
| ProGuard/R8 rules for release | ✅ |
| Unit tests | ✅ (5 JVM test classes) |
| Instrumented UI tests | ❌ (dependencies declared, none written) |
| Debug + Release APK | ⚠️ Built by CI, not in this sandbox |
| GitHub repository, Release, README, HANDOFF, APP_SPECIFICATION | ✅ / ⚠️ Release created by CI |

**Required feature count: 21 product requirements → 20 ✅ implemented, 1 ⚠️
(cloud sync delivered as file-based backup, per §7). No feature was dropped.**

---

## 5. Build Commands

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK
./gradlew lint               # Android Lint
./gradlew testDebugUnitTest  # JVM unit tests
```

### APK output paths
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```
Lint report: `app/build/reports/lint-results-debug.html`
Test report: `app/build/reports/tests/testDebugUnitTest/index.html`

### Release signing
`app/build.gradle` reads the keystore from the environment and falls back to an
unsigned release APK when unset (no secret is ever committed):
```bash
export ANDROID_KEYSTORE_PATH=/path/to/release.keystore
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=...
export ANDROID_KEY_PASSWORD=...
```

### ⚠️ Enable CI (one manual step)
The credential used for this branch lacks GitHub's `workflow` scope, so the
workflow file could not be pushed into `.github/workflows/`. Move it once:
```bash
git mv github-workflow/android.yml .github/workflows/android.yml
git commit -m "ci: enable Android workflow"
git push origin arena/01a0012c-smart-study
```
Then every push builds both APKs, runs lint + tests and publishes the Release.
You can also trigger it manually from the **Actions** tab (`workflow_dispatch`).

---

## 6. How to Resume in a New Chat

```
My Android project is on GitHub (public repo):
https://github.com/viphasibul792/smart.study-

1. Fetch/clone the repository.
2. Read HANDOFF.md carefully.
3. Read README.md.
4. Recreate the project in the workspace.
5. Continue from exactly where the previous session stopped.
6. My task now: [WRITE NEW TASK HERE]
7. After completing the task, update HANDOFF.md.
8. Build and verify the project.
9. Upload all changes to the same GitHub repository.
10. If an APK is rebuilt, update the GitHub Release.
```

The native Android project lives at the repository root (`app/`, `gradle/`,
`build.gradle`, `settings.gradle`) on branch **`arena/01a0012c-smart-study`**.

---

## 7. Known Issues / Next Steps

### Platform limitations and design decisions
1. **"ক্লাউড সিঙ্ক" is file-based, not account-based.** The brief's concrete
   requirement — *ব্যাকআপ ফাইল ডাউনলোড করুন (.json)* and *ব্যাকআপ ফাইল সিলেক্ট
   করুন (.json)* — is fully implemented via the Storage Access Framework, and the
   picker can write straight into Google Drive/OneDrive, which covers moving data
   to another device. True background sync would require a backend, user accounts
   and stored credentials, which the security rules for this project forbid.
   *Next step if wanted:* add Google Drive App Data via the Drive REST API with
   OAuth, keeping the same `BackupModels` JSON format.
2. **Exact alarms** need the *Alarms & reminders* grant on Android 12+; without it
   `ReminderScheduler` silently degrades to an inexact alarm (a snackbar offers to
   open the settings screen).
3. **In-app playback** uses Chrome Custom Tabs, not an embedded player — a raw
   WebView player would violate YouTube's terms and was explicitly out of scope.
4. **Notification permission** (Android 13+) is requested on first launch; if
   denied, everything except reminders keeps working.

### Verification debt
5. **The Gradle build has not been executed** (no Android SDK; Google/Gradle
   mirrors blocked in the sandbox). Pure-Java logic *was* compiled and tested for
   real (23/23), but AAPT resource linking and Room's annotation processor have
   not run. Run the CI workflow or a local `./gradlew assembleDebug` and record
   the real result here before claiming a verified build.

   Verification during this session found and fixed several genuine defects:
   duplicate toolbar menu inflation (`setSupportActionBar` + `app:menu` +
   `onCreateOptionsMenu`), Room POJO constructor ambiguity on the progress
   models, `%d`↔`%s` format crashes when rendering Bengali numerals, an unused
   `@Transaction` default DAO method, orphaned styles and unused imports.

### Suggested next steps
6. Move the CI workflow into `.github/workflows/` and run it (§5).
7. Add Espresso instrumented tests for the dashboard → chapter → bulk-import flow.
8. Add drag-and-drop reordering for sessions/subjects (the `position` column and
   ordering queries already exist).
9. Add a stable release keystore stored as GitHub secrets before any Play Store
   submission (`KEYSTORE_PASSWORD` / `KEY_PASSWORD` are already wired up).
10. Consider a home-screen widget showing today's next session and progress.
