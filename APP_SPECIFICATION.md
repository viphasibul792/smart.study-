# APP_SPECIFICATION.md — Session Tracks

> **This file is the source of truth for future development.**
> A new session should read this first, then `HANDOFF.md`, then the code.

---

## 1. Original requirements (as supplied, verbatim)

App Name: **Session Tracks**
Package Name: `com.sessiontracks.app`
GitHub Repository: `viphasibul792/smart.study-`

### 📱 স্টাডি রিমাইন্ডার অ্যাপ - ফিচার আর্কিটেকচার

**১. মূল ড্যাশবোর্ড ও সেশন ম্যানেজমেন্ট (Main Dashboard)**
- **ডিফল্ট সেশন:** মূল পেজে প্রাথমিকভাবে ৩টি সেশন সেট করা থাকবে
  (যেমন: সেশন ১: রাত ৭:০০ - ৮:০০, সেশন ২: রাত ৯:০০ - ১০:০০, সেশন ৩: রাত ১১:০০ - ১২:০০)।
- **ডাইনামিক সেশন এডিটর:** ব্যবহারকারী যেকোনো সময় "Add New Session" বাটনে চাপ দিয়ে
  নতুন সেশন তৈরি, নাম পরিবর্তন বা সময় আপডেট করতে পারবেন।

**২. বিষয় ব্যবস্থাপনা (Subject Management)**
- **সাবজেক্ট কার্ড:** প্রতিটি সেশনের ভেতরে বিষয় (যেমন: Physics, Higher Math) যুক্ত বা
  মুছে ফেলা যাবে।
- **কাস্টম সময় সূচি:** প্রতি বিষয়ের পাশে পড়ার নির্দিষ্ট সময় সম্পাদনা (Edit) করা যাবে
  (যেমন: Physics: ৮:০০ PM - ৯:০০ PM)।

**৩. অধ্যায় ও লেকচার ম্যানেজমেন্ট (Chapter & Lecture Flow)**
- **অধ্যায় শিরোনাম:** বিষয়টিতে ক্লিক করলে একটি ড্রপডাউন বা নতুন পেজ খুলবে যেখানে
  অধ্যায়ের নাম দেখাবে (যেমন: অধ্যায় ০২: ভেক্টর)।
- **লেকচার সংখ্যা ইনপুট:** "Total Lectures" ঘরে সংখ্যা দিলে (যেমন: ২০) অ্যাপটি
  স্বয়ংক্রিয়ভাবে Lecture 1, Lecture 2, ... Lecture 20 বাটন তৈরি করবে।
- **স্মার্ট লিঙ্ক ম্যাপিং (Bulk Link Importer):** নিচে দেয়া বড় টেক্সট বক্সে একসাথে
  সবকটি লিঙ্ক পেস্ট করলে অ্যাপ স্বয়ংক্রিয়ভাবে টেক্সট পার্স করে প্রতিটি লিঙ্ককে তার
  নিজস্ব লেকচার বাটনের সাথে যুক্ত করে নেবে।

**৪. স্টাডি ট্র্যাকিং ও ইন্টারঅ্যাকশন (Study Tracking)**
- **এক ক্লিকে প্লে:** লেকচার বাটনে চাপ দিলে সরাসরি YouTube App বা ইন-অ্যাপ ব্রাউজারে
  ক্লাসটি চালু হবে।
- **কমপ্লিশন চেকবক্স (Progress Check):** ক্লাস দেখা শেষ হলে বাটনের পাশের চেকবক্সে
  ক্লিক করলে টিকচিহ্ন সবুজ হবে এবং বোতামটির রঙ পরিবর্তিত হয়ে 'সম্পন্ন' নির্দেশ করবে।
- **প্রোগ্রেস বার (Overall Progress Bar):** একটি অধ্যায়ের ২০টির মধ্যে কয়টি ক্লাস শেষ
  হলো তার ভিজ্যুয়াল প্রোগ্রেস বার (যেমন: 60% Completed) দেখাবে।
- **কুইক নোটস (Quick Notes):** প্রতিটি অধ্যায় বা লেকচারের সাথে ছোট একটি "Notes" বক্স
  রাখা, যাতে ক্লাসের গুরুত্বপূর্ণ সূত্র বা পয়েন্ট লিখে রাখা যায়।
- **ডেটা ব্যাকআপ ও ক্লাউড সিঙ্ক:** অ্যাপ রিফ্রেশ বা অন্য ডিভাইসে লগইন করলে যেন পেস্ট
  করা লিঙ্ক ও প্রোগ্রেস মুছে না যায়, তার জন্য ব্যাকআপ ফাইল ডাউনলোড করুন (.json) এবং
  ব্যাকআপ ফাইল সিলেক্ট করুন (.json) এ ব্যাকআপ রাখা।

### Mandated technical constraints
- 100% **Native Android**: **Java + XML View System**
- **No WebView** as the primary implementation, no HTML/CSS/JS core
- Modern Android architecture, production-quality code, responsive UI
- Proper state management, offline support, error/loading/empty states
- Accessibility support, proper permissions, no hardcoded strings/colors/dimens
- Debug APK, Release APK, public GitHub repository, GitHub Release with APK,
  `README.md`, `HANDOFF.md`, `APP_SPECIFICATION.md`

---

## 2. Derived product decisions

Ambiguities in the brief were resolved as follows (all preserve the intent):

| Ambiguity | Decision | Rationale |
|---|---|---|
| "ড্রপডাউন বা নতুন পেজ" for chapters | **New page** (`SubjectDetailActivity` → `ChapterDetailActivity`) | Chapters hold 20+ lectures; a dropdown inside a card would be unusable. Sessions/subjects still expand inline on the dashboard. |
| "ক্লাউড সিঙ্ক" with no backend specified | **File-based `.json` backup via SAF** + Android auto-backup | The brief's own concrete requirement is the two `.json` buttons. A real cloud sync would need accounts/servers and credential storage, which the security rules forbid. Users can save the file into Drive from the picker. |
| Login/Authentication | **None** | Not required by any feature; avoids storing credentials. |
| Notifications | **Session reminders** (the app is a "স্টাডি রিমাইন্ডার অ্যাপ") | The product name and purpose imply reminding the user when a session starts. |
| Search/filter/sort | **Search** across sessions and subjects; sessions sorted by position/time; lectures by number | Needed once several sessions exist. |
| Language | **Bengali default**, English in `values-en`, Bengali numeral toggle | Requirements were written in Bengali. |
| Lecture ceiling | **500 per chapter** | Prevents pathological input while far exceeding the 20-lecture example. |

---

## 3. Data model

```
StudySession (sessions)
 ├── id, name, start_minute, end_minute, color, position,
 │   reminder_enabled, created_at, updated_at
 └── 1..N Subject (subjects)  [FK session_id, ON DELETE CASCADE]
      ├── id, session_id, name, start_minute, end_minute (-1 = inherit),
      │   color, position, created_at, updated_at
      └── 1..N Chapter (chapters)  [FK subject_id, ON DELETE CASCADE]
           ├── id, subject_id, number, name, total_lectures, notes,
           │   position, created_at, updated_at
           └── 1..N Lecture (lectures)  [FK chapter_id, ON DELETE CASCADE]
                └── id, chapter_id, number, title, video_url, completed,
                    completed_at, notes, updated_at
                    UNIQUE(chapter_id, number)
```

Times are stored as **minutes from midnight** (0–1439) so they sort trivially and
are timezone/locale independent; a session may wrap past midnight.

### Backup JSON format (`session-tracks-backup`, version 1)
```json
{
  "format": "session-tracks-backup",
  "version": 1,
  "exportedAt": 1765700000000,
  "appVersion": "1.0.0",
  "sessions": [
    { "id": "...", "name": "সেশন ১", "startMinute": 1140, "endMinute": 1200,
      "color": -13154481, "position": 0, "reminderEnabled": true,
      "subjects": [
        { "id": "...", "name": "Physics", "startMinute": 1200, "endMinute": 1260,
          "chapters": [
            { "id": "...", "number": "02", "name": "ভেক্টর", "totalLectures": 20,
              "notes": "...",
              "lectures": [
                { "id": "...", "number": 1, "title": "", "videoUrl": "https://youtu.be/…",
                  "completed": true, "completedAt": 1765700000000, "notes": "" }
              ] } ] } ] }
  ]
}
```

---

## 4. Navigation map

```
MainActivity (Dashboard)
 ├─ FAB / empty action ........... SessionEditorDialog (create)
 ├─ session overflow ............. SessionEditorDialog (edit) | delete+undo | SubjectEditorDialog
 ├─ subject card tap ............. SubjectDetailActivity
 ├─ subject overflow ............. SubjectEditorDialog | delete
 ├─ toolbar: search .............. inline search field
 ├─ toolbar: backup .............. BackupSheet
 ├─ toolbar: settings ............ SettingsSheet ─→ BackupSheet
 └─ deep link .................... sessiontracks://dashboard?session=<id>

SubjectDetailActivity
 ├─ FAB / empty action ........... ChapterEditorDialog (create)
 ├─ chapter card tap ............. ChapterDetailActivity
 ├─ chapter overflow ............. ChapterEditorDialog | NotesSheet | delete
 └─ toolbar ...................... SubjectEditorDialog | delete

ChapterDetailActivity
 ├─ Total Lectures field ......... grow/shrink lecture grid
 ├─ Bulk Link Importer button .... BulkImportSheet
 ├─ lecture tap .................. YouTube app / Custom Tab
 ├─ lecture checkbox ............. toggle completion
 ├─ lecture long-press ........... play | toggle | edit link | NotesSheet | copy
 ├─ Notes button ................. NotesSheet (chapter)
 ├─ toolbar ...................... share progress | edit | mark all | reset | delete
 └─ deep link .................... sessiontracks://chapter/<chapterId>
```

---

## 5. Feature → implementation map

| # | Requirement | Native implementation |
|---|---|---|
| 1.1 | ৩টি ডিফল্ট সেশন | `StudyRepository.seedDefaultsIfEmpty()` on first launch |
| 1.2 | ডাইনামিক সেশন এডিটর | `SessionEditorDialog` + `MaterialTimePicker` |
| 2.1 | সাবজেক্ট কার্ড add/delete | `SubjectAdapter`, `SubjectEditorDialog`, cascade delete |
| 2.2 | কাস্টম সময় সূচি | `Subject.startMinute/endMinute` (-1 = inherit) |
| 3.1 | অধ্যায় শিরোনাম / নতুন পেজ | `SubjectDetailActivity` → `ChapterDetailActivity` |
| 3.2 | Total Lectures → auto buttons | `createChapter`/`syncLectureRows` + `LectureAdapter` grid |
| 3.3 | Bulk Link Importer | `LinkParser` + `BulkImportSheet` + `applyBulkLinks` |
| 4.1 | এক ক্লিকে প্লে | `IntentUtils.openVideo()` — YouTube intent or Custom Tab |
| 4.2 | কমপ্লিশন চেকবক্স | `MaterialCheckBox` + `state_activated` tile background |
| 4.3 | প্রোগ্রেস বার | `ProgressDao` SQL aggregates + `LinearProgressIndicator` |
| 4.4 | কুইক নোটস | `NotesSheet` for chapters and lectures |
| 4.5 | `.json` ব্যাকআপ ও রিস্টোর | `BackupManager` + Storage Access Framework |

---

## 6. Non-functional requirements

- No main-thread disk/network I/O (StrictMode enabled in debug builds).
- Configuration changes and process death preserve all state (Room + LiveData +
  `onSaveInstanceState` in dialogs).
- Every destructive action is confirmed; session deletion is undoable.
- All user-facing text lives in `strings.xml` / `values-en/strings.xml`.
- Colours, dimensions, styles and themes are centralised resources.
- Minimum 48dp touch targets and content descriptions on interactive views.
- Light and dark themes, `sw600dp` tablet and landscape layout variants.
