#!/usr/bin/env bash
# ============================================================================
#  Session Tracks — APK build script (runs on GitHub Actions or locally)
#
#  Builds the debug + release APKs, signs the release build, and (on CI)
#  publishes them to the GitHub Release for the given tag.
#
#  Usage:
#     bash ci/build-apk.sh            # build only
#     PUBLISH=1 bash ci/build-apk.sh  # build + attach to a GitHub Release
#
#  Env:
#     VERSION   release tag to publish (default: v1.0.0)
#     PUBLISH   set to 1 to create/update the GitHub Release
#     GH_TOKEN  required when PUBLISH=1
# ============================================================================
set -euo pipefail

VERSION="${VERSION:-v1.0.0}"
PUBLISH="${PUBLISH:-0}"
OUT_DIR="artifacts"

echo "=============================================="
echo " Session Tracks — building ${VERSION}"
echo "=============================================="
java -version 2>&1 | head -1
echo "ANDROID_HOME=${ANDROID_HOME:-<unset>}"
echo

chmod +x ./gradlew

# ---------------------------------------------------------------------------
# 1. Unit tests and lint (non-fatal so a lint warning never blocks the APK)
# ---------------------------------------------------------------------------
echo "---> Running unit tests"
./gradlew testDebugUnitTest --no-daemon --stacktrace

echo "---> Running Android Lint (non-blocking)"
./gradlew lint --no-daemon || echo "WARNING: lint reported issues; continuing."

# ---------------------------------------------------------------------------
# 2. Debug APK
# ---------------------------------------------------------------------------
echo "---> Building debug APK"
./gradlew assembleDebug --no-daemon --stacktrace

# ---------------------------------------------------------------------------
# 3. Signing keystore
#    A keystore is generated on the fly so the release APK installs cleanly on
#    a phone. Provide your own via the ANDROID_KEYSTORE_* variables (or the
#    KEYSTORE_PASSWORD / KEY_PASSWORD secrets) for a stable signing identity.
# ---------------------------------------------------------------------------
KEYSTORE_DIR="${RUNNER_TEMP:-/tmp}"
KEYSTORE_FILE="${KEYSTORE_DIR}/session-tracks-release.keystore"

if [ -z "${ANDROID_KEYSTORE_PATH:-}" ]; then
  echo "---> Generating a release keystore"
  STORE_PASS="${KEYSTORE_PASSWORD:-sessiontracks}"
  KEY_PASS="${KEY_PASSWORD:-$STORE_PASS}"
  rm -f "$KEYSTORE_FILE"
  keytool -genkeypair -v \
    -keystore "$KEYSTORE_FILE" \
    -alias sessiontracks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" \
    -dname "CN=Session Tracks, OU=App, O=Session Tracks, L=Dhaka, C=BD"

  export ANDROID_KEYSTORE_PATH="$KEYSTORE_FILE"
  export ANDROID_KEYSTORE_PASSWORD="$STORE_PASS"
  export ANDROID_KEY_ALIAS="sessiontracks"
  export ANDROID_KEY_PASSWORD="$KEY_PASS"
else
  echo "---> Using the supplied keystore at ANDROID_KEYSTORE_PATH"
fi

# ---------------------------------------------------------------------------
# 4. Release APK
# ---------------------------------------------------------------------------
echo "---> Building release APK"
./gradlew assembleRelease --no-daemon --stacktrace

# ---------------------------------------------------------------------------
# 5. Collect the outputs
# ---------------------------------------------------------------------------
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$DEBUG_APK" ]; then
  echo "ERROR: debug APK not found at $DEBUG_APK" >&2
  exit 1
fi
cp "$DEBUG_APK" "$OUT_DIR/session-tracks-${VERSION}-debug.apk"

if [ -f "$RELEASE_APK" ]; then
  cp "$RELEASE_APK" "$OUT_DIR/session-tracks-${VERSION}-release.apk"
else
  # An unsigned release build lands under a different name.
  UNSIGNED="app/build/outputs/apk/release/app-release-unsigned.apk"
  if [ -f "$UNSIGNED" ]; then
    echo "WARNING: release APK is unsigned."
    cp "$UNSIGNED" "$OUT_DIR/session-tracks-${VERSION}-release-unsigned.apk"
  else
    echo "ERROR: no release APK produced" >&2
    exit 1
  fi
fi

echo
echo "---> Build complete:"
ls -lh "$OUT_DIR"

# ---------------------------------------------------------------------------
# 6. Publish to a GitHub Release
# ---------------------------------------------------------------------------
if [ "$PUBLISH" = "1" ]; then
  echo
  echo "---> Publishing release ${VERSION}"

  NOTES_FILE="$(mktemp)"
  cat > "$NOTES_FILE" <<NOTES
## 📚 Session Tracks ${VERSION}

স্টাডি সেশন ও লেকচার ট্র্যাকার — সম্পূর্ণ **Native Android** অ্যাপ
(Java + XML View System, MVVM + Room)। কোনো WebView নেই।

### 📥 ফোনে ইনস্টল করুন
1. নিচের **Assets** থেকে \`session-tracks-${VERSION}-release.apk\` ডাউনলোড করুন
2. ফোনে ফাইলটি খুলুন
3. *"Install from unknown sources"* চাইলে অনুমতি দিন
4. ইনস্টল সম্পন্ন! 🎉

*(\`-debug.apk\` শুধু ডেভেলপমেন্টের জন্য — সাধারণ ব্যবহারে \`-release.apk\` নিন।)*

### ✨ ফিচার
- ৩টি ডিফল্ট সেশন (৭–৮, ৯–১০, ১১–১২ PM) + ডাইনামিক সেশন এডিটর
- বিষয় ব্যবস্থাপনা ও প্রতি বিষয়ের কাস্টম সময়সূচি
- "Total Lectures" দিলে স্বয়ংক্রিয় Lecture 1…N বাটন
- বাল্ক লিঙ্ক ইম্পোর্টার — একসাথে সব লিঙ্ক পেস্ট করলে নিজে নিজে ম্যাপ হয়
- এক ক্লিকে YouTube-এ প্লে, কমপ্লিশন চেকবক্স ও প্রোগ্রেস বার
- কুইক নোটস, \`.json\` ব্যাকআপ ও রিস্টোর, সেশন রিমাইন্ডার
- ডার্ক মোড, বাংলা/English, সম্পূর্ণ অফলাইন

**Min SDK 24 (Android 7.0) · Target SDK 35 · Java 17**

_GitHub Actions দ্বারা স্বয়ংক্রিয়ভাবে বিল্ড করা।_
NOTES

  if gh release view "$VERSION" >/dev/null 2>&1; then
    echo "Release exists — updating notes and replacing assets."
    gh release edit "$VERSION" --notes-file "$NOTES_FILE"
    gh release upload "$VERSION" "$OUT_DIR"/*.apk --clobber
  else
    gh release create "$VERSION" \
      --title "Session Tracks ${VERSION}" \
      --notes-file "$NOTES_FILE" \
      --target "${GITHUB_SHA:-HEAD}" \
      "$OUT_DIR"/*.apk
  fi

  echo "---> Release published: ${VERSION}"
fi

echo
echo "=============================================="
echo " DONE"
echo "=============================================="
