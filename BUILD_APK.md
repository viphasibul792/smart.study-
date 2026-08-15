# 📦 APK কীভাবে তৈরি করবেন — Session Tracks

তিনটি উপায় আছে। **উপায় ১ সবচেয়ে সহজ** — কোনো সফটওয়্যার ইনস্টল করতে হবে না,
শুধু ব্রাউজার থেকেই APK পেয়ে যাবেন।

---

## ✅ উপায় ১ — GitHub Actions (সুপারিশকৃত, কম্পিউটার লাগবে না)

GitHub-এর সার্ভারেই APK বিল্ড হবে এবং Release-এ যুক্ত হয়ে যাবে।
মোট সময়: **~২ মিনিট সেটআপ + ~৫ মিনিট বিল্ড**।

### ধাপ ১ — workflow ফাইলটি সঠিক জায়গায় নিন

ফাইলটি এখন আছে `github-workflow/android.yml`-এ। এটিকে
`.github/workflows/android.yml`-এ নিতে হবে।

> **কেন আমি নিজে করতে পারিনি:** আমার GitHub credential-এ `workflow` scope নেই,
> তাই `.github/workflows/` ফোল্ডারে ফাইল push করার অনুমতি ছিল না।

**ব্রাউজার থেকে (সবচেয়ে সহজ):**

1. এই লিংকে যান:
   https://github.com/viphasibul792/smart.study-/blob/arena/01a0012c-smart-study/github-workflow/android.yml
2. ডান পাশের **পেন্সিল আইকন** (✏️ Edit) এ ক্লিক করুন।
3. একদম উপরে ফাইলের নামের ঘরে পুরো পাথটি বদলে দিন:
   ```
   .github/workflows/android.yml
   ```
   *(শুধু নামের ঘরে `.github/workflows/` লিখলেই GitHub নিজে থেকে ফোল্ডার বানিয়ে নেবে)*
4. নিচে **Commit changes** → branch হিসেবে `arena/01a0012c-smart-study` রেখে
   **Commit changes** চাপুন।

**অথবা টার্মিনাল থেকে:**

```bash
git clone https://github.com/viphasibul792/smart.study-.git
cd smart.study-
git checkout arena/01a0012c-smart-study

git mv github-workflow/android.yml .github/workflows/android.yml
git commit -m "ci: enable Android workflow"
git push origin arena/01a0012c-smart-study
```

### ধাপ ২ — বিল্ড চালু হবে

Commit করার সাথে সাথেই বিল্ড শুরু হয়ে যাবে। দেখতে পারেন এখানে:
https://github.com/viphasibul792/smart.study-/actions

হাতে চালাতে চাইলে: **Actions** ট্যাব → বাঁ পাশে **Android CI** → ডানে
**Run workflow** → branch `arena/01a0012c-smart-study` → **Run workflow**।

### ধাপ ৩ — APK ডাউনলোড করুন

বিল্ড সবুজ (✅) হলে APK দুই জায়গায় পাবেন:

| কোথায় | কী পাবেন |
|---|---|
| **Releases** → https://github.com/viphasibul792/smart.study-/releases/tag/v1.0.0 | `session-tracks-v1.0.0-release.apk` (সাইন করা, ফোনে ইনস্টলযোগ্য) |
| **Actions** → বিল্ডে ঢুকে নিচে **Artifacts** | debug + release দুটোই |

> ফোনে ইনস্টল করার সময় *"Install from unknown sources"* চাইলে অনুমতি দিন।

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
