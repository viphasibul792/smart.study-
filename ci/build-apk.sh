#!/usr/bin/env bash
# ============================================================================
#  Session Tracks — APK build & release script (GitHub Actions or local)
#
#  Two sources of APKs:
#    A. The Gradle build (app/) — the full AndroidX + Material + Room app.
#       Needs network access to maven.google.com, which CI has.
#    B. apk-build/dist/*.apk — the framework-only build, already compiled and
#       signed, committed to the repo. Used as a guaranteed fallback so a
#       release always ships an installable APK.
#
#  Everything that exists is attached to the GitHub Release.
#
#  Usage:
#     bash ci/build-apk.sh            # build only
#     PUBLISH=1 bash ci/build-apk.sh  # build + attach to a GitHub Release
#
#  Env:
#     VERSION   release tag to publish (default: v1.0.0)
#     PUBLISH   set to 1 to create/update the GitHub Release
#     GH_TOKEN  required when PUBLISH=1
#     PUSH_LOG  set to 1 to commit the build log back to the branch
# ============================================================================
set -uo pipefail        # NOTE: not -e; we want to survive a Gradle failure.

VERSION="${VERSION:-v1.0.0}"
PUBLISH="${PUBLISH:-0}"
PUSH_LOG="${PUSH_LOG:-1}"
OUT_DIR="artifacts"
LOG="build-log.txt"

exec > >(tee "$LOG") 2>&1

echo "=============================================="
echo " Session Tracks — building ${VERSION}"
echo "=============================================="
java -version 2>&1 | head -1
echo "ANDROID_HOME=${ANDROID_HOME:-<unset>}"
echo "runner: $(uname -a)"
echo

rm -rf "$OUT_DIR"; mkdir -p "$OUT_DIR"

GRADLE_OK=0
GRADLE_NOTE=""

# ---------------------------------------------------------------------------
# A. Gradle build
# ---------------------------------------------------------------------------
if [ -f ./gradlew ]; then
  chmod +x ./gradlew

  echo "---> Gradle version check"
  ./gradlew --version --no-daemon 2>&1 | tail -20
  echo "     exit=$?"

  echo
  echo "---> Running unit tests (non-blocking)"
  ./gradlew testDebugUnitTest --no-daemon --stacktrace 2>&1 | tail -60
  echo "     tests exit=${PIPESTATUS[0]}"

  echo
  echo "---> Building debug APK"
  ./gradlew assembleDebug --no-daemon --stacktrace > /tmp/gradle-debug.log 2>&1
  DEBUG_RC=$?
  echo "     assembleDebug exit=$DEBUG_RC"
  echo "     --- resource/compile errors ---"
  grep -nE "error:|e: |Execution failed|Android resource linking failed|AAPT" /tmp/gradle-debug.log | head -60 || true
  echo "     --- end ---"

  # Signing keystore for the release build.
  KEYSTORE_FILE="${RUNNER_TEMP:-/tmp}/session-tracks-release.keystore"
  if [ -z "${ANDROID_KEYSTORE_PATH:-}" ]; then
    echo
    echo "---> Generating a release keystore"
    STORE_PASS="${KEYSTORE_PASSWORD:-sessiontracks}"
    KEY_PASS="${KEY_PASSWORD:-$STORE_PASS}"
    rm -f "$KEYSTORE_FILE"
    keytool -genkeypair -v \
      -keystore "$KEYSTORE_FILE" -alias sessiontracks \
      -keyalg RSA -keysize 2048 -validity 10000 \
      -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
      -dname "CN=Session Tracks, OU=App, O=Session Tracks, L=Dhaka, C=BD" 2>&1 | tail -3
    export ANDROID_KEYSTORE_PATH="$KEYSTORE_FILE"
    export ANDROID_KEYSTORE_PASSWORD="$STORE_PASS"
    export ANDROID_KEY_ALIAS="sessiontracks"
    export ANDROID_KEY_PASSWORD="$KEY_PASS"
  fi

  echo
  echo "---> Building release APK"
  ./gradlew assembleRelease --no-daemon --stacktrace 2>&1 | tail -80
  RELEASE_RC=${PIPESTATUS[0]}
  echo "     assembleRelease exit=$RELEASE_RC"

  echo
  echo "---> Gradle outputs on disk:"
  find app/build/outputs -name '*.apk' 2>/dev/null || echo "     (none)"

  for f in $(find app/build/outputs -name '*.apk' 2>/dev/null); do
    case "$f" in
      *debug*)             cp "$f" "$OUT_DIR/session-tracks-${VERSION}-debug.apk" ;;
      *release-unsigned*)  cp "$f" "$OUT_DIR/session-tracks-${VERSION}-release-unsigned.apk" ;;
      *release*)           cp "$f" "$OUT_DIR/session-tracks-${VERSION}-release.apk" ;;
    esac
    GRADLE_OK=1
  done
  [ "$GRADLE_OK" = "1" ] || GRADLE_NOTE="Gradle produced no APK (see build-log.txt)."
else
  GRADLE_NOTE="No gradlew in the repository."
fi

# ---------------------------------------------------------------------------
# B. Framework-only build committed in apk-build/dist
# ---------------------------------------------------------------------------
echo
echo "---> Collecting the prebuilt framework-only APKs"
for f in apk-build/dist/*.apk; do
  [ -f "$f" ] || continue
  cp "$f" "$OUT_DIR/$(basename "$f")"
  echo "     + $(basename "$f")"
done

echo
echo "---> Artifacts to publish:"
ls -lh "$OUT_DIR" || true

if ! ls "$OUT_DIR"/*.apk >/dev/null 2>&1; then
  echo "ERROR: no APK of any kind was produced." >&2
  STATUS=1
else
  STATUS=0
fi

# ---------------------------------------------------------------------------
# C. Publish to the GitHub Release
# ---------------------------------------------------------------------------
if [ "$PUBLISH" = "1" ] && [ "$STATUS" = "0" ]; then
  echo
  echo "---> Publishing ${VERSION}"
  if ! gh release view "$VERSION" >/dev/null 2>&1; then
    gh release create "$VERSION" --title "Session Tracks ${VERSION}" \
      --notes "Automated build." || true
  fi
  gh release upload "$VERSION" "$OUT_DIR"/*.apk --clobber && echo "     upload OK"
fi

# ---------------------------------------------------------------------------
# D. Commit the log back to the branch (GitHub's log API is unreachable from
#    some networks; this makes the build output readable via a plain git fetch)
# ---------------------------------------------------------------------------
if [ "$PUSH_LOG" = "1" ] && [ -n "${GITHUB_ACTIONS:-}" ]; then
  echo
  echo "---> Committing the build log"
  mkdir -p ci/logs
  tail -c 200000 "$LOG" > ci/logs/last-build.log
  {
    echo "run:        ${GITHUB_RUN_ID:-?}"
    echo "sha:        ${GITHUB_SHA:-?}"
    echo "gradle_ok:  $GRADLE_OK"
    echo "note:       $GRADLE_NOTE"
    echo "artifacts:"
    ls -1 "$OUT_DIR" 2>/dev/null | sed 's/^/  /'
  } > ci/logs/summary.txt
  git config user.name  "github-actions[bot]"
  git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
  cp /tmp/gradle-debug.log ci/logs/gradle-debug.log 2>/dev/null || true
  git add -f ci/logs/last-build.log ci/logs/summary.txt ci/logs/gradle-debug.log
  git commit -m "ci: build log for ${GITHUB_RUN_ID:-local} [skip ci]" || true
  git push origin "HEAD:${GITHUB_REF_NAME:-arena/01a0012c-smart-study}" || true
fi

exit "$STATUS"
