# 📱 Smart Study — APK Release গাইড

এই repo-তে এখন **Capacitor Android প্রজেক্ট** যোগ করা হয়েছে (`android/` ফোল্ডার), যা আপনার React ওয়েব অ্যাপকে Android অ্যাপে রূপান্তর করে।

APK build ও Release **GitHub Actions** স্বয়ংক্রিয়ভাবে করবে। শুধু একটা workflow ফাইল যোগ করতে হবে — GitHub-এর নিয়ম অনুযায়ী এই ফাইলটা অ্যাকাউন্টের মালিককেই (আপনাকে) যোগ করতে হয়।

## ✅ যা করতে হবে (মাত্র ১টা ধাপ)

1. এই লিঙ্কে যান (workflow ফাইলের কন্টেন্ট আগে থেকেই বসানো থাকবে) — chat-এ দেওয়া "prefill" লিঙ্কটা ক্লিক করুন,
   **অথবা** ম্যানুয়ালি: repo → `arena/019fffef-smart-study` branch → **Add file → Create new file** → ফাইলের নাম দিন:

   ```
   .github/workflows/android-release.yml
   ```

2. `github-workflow/android-release.yml` ফাইলের পুরো কন্টেন্ট কপি করে পেস্ট করুন।

3. **Commit changes** চাপুন।

ব্যাস! Commit করার সাথে সাথে GitHub Actions চালু হবে:
- ✅ ওয়েব অ্যাপ build করবে (`npm run build`)
- ✅ Capacitor দিয়ে Android প্রজেক্টে sync করবে
- ✅ Signed release APK build করবে
- ✅ **`v1.0.0` নামে Release তৈরি করে APK আপলোড করবে**

## 📥 APK কোথায় পাবেন

Actions শেষ হলে (~৫-৮ মিনিট):
**https://github.com/viphasibul792/smart.study-/releases**

সেখান থেকে `smart-study-v1.0.0.apk` ডাউনলোড করে ফোনে ইনস্টল করুন।

## 🔁 পরবর্তীতে নতুন ভার্সন release করতে

Repo → **Actions** → **Build Android APK & Release** → **Run workflow** → ভার্সন লিখুন (যেমন `v1.1.0`) → Run।

## ⚠️ নিরাপত্তা নোট

আপনি চ্যাটে যে personal access token শেয়ার করেছিলেন সেটা **অবশ্যই revoke করুন**:
https://github.com/settings/tokens — পাবলিকলি শেয়ার হওয়া token দিয়ে যে কেউ আপনার অ্যাকাউন্টে ঢুকতে পারে।
