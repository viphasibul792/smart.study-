# 📦 APK কীভাবে তৈরি করবেন — Session Tracks

---

## 🔴 আগের বিল্ড কেন ফেল করেছিল

আপনি `.github/workflows/android.yml` ফাইলটি তৈরি করেছিলেন, কিন্তু **ফাইলটি
সম্পূর্ণ খালি সেভ হয়েছে (মাত্র ১ বাইট)** — কনটেন্ট পেস্ট হয়নি। তাই GitHub
ফাইলটি পড়তেই পারেনি এবং ০ সেকেন্ডে ফেল করেছে।

**সমাধান:** আমি বিল্ডের সব কাজ `ci/build-apk.sh` স্ক্রিপ্টে সরিয়ে নিয়েছি
(এটি ইতিমধ্যে GitHub-এ আছে ✅)। এখন workflow ফাইলটি মাত্র **২৫ লাইন** —
পেস্ট করা অনেক সহজ ও নিরাপদ।

---

## ✅ উপায় ১ — GitHub Actions (মাত্র ২ মিনিট, কম্পিউটার লাগবে না)

### ধাপ ১ — খালি ফাইলটি এডিট করুন

এই লিংকে যান (ফাইলটি ইতিমধ্যে আছে, শুধু খালি):

👉 https://github.com/viphasibul792/smart.study-/edit/arena/01a0012c-smart-study/.github/workflows/android.yml

*(লিংকটি সরাসরি এডিট মোডে খুলবে)*

### ধাপ ২ — ভেতরের সব মুছে নিচের লেখাটুকু হুবহু বসান

> 💡 **টিপ:** এডিটরে ক্লিক করে **Ctrl+A** (ম্যাকে Cmd+A) চেপে সব সিলেক্ট করুন,
> **Delete** চাপুন, তারপর নিচের কোডটুকু কপি করে পেস্ট করুন।

```yaml
name: Android CI
on:
  workflow_dispatch:
  push:
    branches: [arena/01a0012c-smart-study]
permissions:
  contents: write
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - run: PUBLISH=1 VERSION=v1.0.0 bash ci/build-apk.sh
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: apks
          path: artifacts/*.apk
```

### ধাপ ৩ — Commit করুন

সবুজ **Commit changes...** বোতাম → branch `arena/01a0012c-smart-study` ঠিক আছে
কিনা দেখুন → **Commit changes**।

### ধাপ ৪ — পেস্ট হয়েছে কিনা যাচাই করুন (গুরুত্বপূর্ণ!)

Commit করার পর ফাইলটি আবার দেখুন:
https://github.com/viphasibul792/smart.study-/blob/arena/01a0012c-smart-study/.github/workflows/android.yml

- ✅ **২৫ লাইন কোড দেখা গেলে** — ঠিক আছে, পরের ধাপে যান
- ❌ **খালি দেখালে বা "0 lines" লেখা থাকলে** — আবার ধাপ ২ করুন

### ধাপ ৫ — বিল্ড দেখুন

Commit করার সাথে সাথেই বিল্ড শুরু হবে:
👉 https://github.com/viphasibul792/smart.study-/actions

- 🟡 হলুদ চাকা = চলছে (৫–১০ মিনিট লাগবে)
- ✅ সবুজ টিক = সফল
- ❌ লাল ক্রস = এরর — লগটি আমাকে পাঠান, ঠিক করে দেবো

### ধাপ ৬ — APK ডাউনলোড করুন 🎉

বিল্ড সবুজ হলে এখানে APK পাবেন:

👉 **https://github.com/viphasibul792/smart.study-/releases/tag/v1.0.0**

**Assets** সেকশন থেকে নামান:

| ফাইল | কার জন্য |
|---|---|
| `session-tracks-v1.0.0-release.apk` | ⭐ **এটি নিন** — ফোনে ইনস্টলের জন্য |
| `session-tracks-v1.0.0-debug.apk` | শুধু ডেভেলপমেন্টের জন্য |

### ধাপ ৭ — ফোনে ইনস্টল করুন

1. ফোনের ব্রাউজার দিয়ে উপরের Releases লিংকে যান
2. `session-tracks-v1.0.0-release.apk` ডাউনলোড করুন
3. ডাউনলোড শেষে ফাইলটিতে ট্যাপ করুন
4. *"Install from unknown sources"* / *"এই উৎস থেকে ইনস্টল করার অনুমতি দিন"*
   চাইলে **Settings** → **Allow** করে ফিরে আসুন
5. **Install** চাপুন — হয়ে গেল! 🎉

---

## 💻 উপায় ২ — Android Studio (গ্রাফিক্যাল)

1. **Android Studio** ইনস্টল করুন (Ladybug বা নতুন): https://developer.android.com/studio
2. প্রজেক্ট নামান:
   ```bash
   git clone https://github.com/viphasibul792/smart.study-.git
   cd smart.study-
   git checkout arena/01a0012c-smart-study
   ```
3. Android Studio → **File → Open** → `smart.study-` ফোল্ডারটি সিলেক্ট করুন।
   *(প্রথমবার Gradle sync-এ ৫–১০ মিনিট লাগবে — লাইব্রেরি ডাউনলোড হবে)*
4. উপরের মেনু → **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. বিল্ড শেষ হলে নিচে ডানে **locate** লিংকে ক্লিক করুন। APK পাবেন:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⌨️ উপায় ৩ — কমান্ড লাইন

**যা লাগবে:** JDK 17 এবং Android SDK (API 35)।

```bash
git clone https://github.com/viphasibul792/smart.study-.git
cd smart.study-
git checkout arena/01a0012c-smart-study

# Android SDK কোথায় আছে তা জানিয়ে দিন
echo "sdk.dir=$HOME/Android/Sdk" > local.properties     # Linux
# echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS
# Windows: sdk.dir=C\:\\Users\\<নাম>\\AppData\\Local\\Android\\Sdk

chmod +x gradlew
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### অন্যান্য কমান্ড

```bash
./gradlew assembleRelease    # রিলিজ APK
./gradlew lint               # Android Lint রিপোর্ট
./gradlew testDebugUnitTest  # ইউনিট টেস্ট
```

### সাইন করা রিলিজ APK

সাইনিং তথ্য environment variable থেকে পড়া হয় (কোনো পাসওয়ার্ড কোডে রাখা নেই)।
আগে একটি keystore বানান:

```bash
keytool -genkeypair -v -keystore release.keystore \
  -alias sessiontracks -keyalg RSA -keysize 2048 -validity 10000

export ANDROID_KEYSTORE_PATH=$PWD/release.keystore
export ANDROID_KEYSTORE_PASSWORD=<আপনার পাসওয়ার্ড>
export ANDROID_KEY_ALIAS=sessiontracks
export ANDROID_KEY_PASSWORD=<আপনার পাসওয়ার্ড>

./gradlew assembleRelease
```

> ⚠️ `release.keystore` ফাইলটি **কখনো GitHub-এ push করবেন না** — এটি
> `.gitignore`-এ ইতিমধ্যে ব্লক করা আছে। Play Store-এ দিলে এই ফাইল ও পাসওয়ার্ড
> হারালে আর কখনো অ্যাপ আপডেট দিতে পারবেন না, তাই নিরাপদে রাখুন।

---

## 🩹 সমস্যা হলে

| সমস্যা | সমাধান |
|---|---|
| `SDK location not found` | `local.properties` ফাইলে `sdk.dir=...` ঠিকভাবে দিন (উপায় ৩ দেখুন) |
| `Unsupported class file major version` | JDK 17 ব্যবহার করুন: `java -version` দিয়ে যাচাই করুন |
| `Could not resolve androidx...` | ইন্টারনেট সংযোগ দরকার — প্রথম বিল্ডে লাইব্রেরি ডাউনলোড হয় |
| `permission denied: ./gradlew` | `chmod +x gradlew` চালান |
| Gradle sync আটকে আছে | Android Studio → **File → Invalidate Caches / Restart** |

---

## ℹ️ কেন আমি নিজে APK বানিয়ে দিতে পারিনি

আমার স্যান্ডবক্সে **Android SDK ছিল না**, এবং `dl.google.com`,
`maven.google.com`, `services.gradle.org`, `repo1.maven.org` — সবগুলো
নেটওয়ার্ক-ব্লকড ছিল। তাই Gradle বিল্ড চালানো সম্ভব হয়নি।

তবে যা যা যাচাই করা সম্ভব ছিল, সব করেছি:

- ৬১টি Java ফাইল ও ১১০টি XML রিসোর্স — সিনট্যাক্স ও রেফারেন্স সম্পূর্ণ যাচাই
- Android-নির্ভর নয় এমন ক্লাসগুলো **সত্যিকারের Java 17-এ কম্পাইল** করা হয়েছে
- **২৩/২৩ ইউনিট টেস্ট আসলেই চালিয়ে পাস** করানো হয়েছে
- ৫১টি Room DAO কোয়েরি সত্যিকারের SQLite ইঞ্জিনে চালানো হয়েছে

⚠️ তবু **Gradle/AAPT বিল্ড এখনো যাচাই হয়নি** — উপায় ১ চালিয়ে নিশ্চিত করুন।
বিল্ডে কোনো এরর এলে আমাকে লগটি পাঠাবেন, আমি ঠিক করে দেবো।
