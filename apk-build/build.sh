#!/usr/bin/env bash
#
# Session Tracks — direct APK build (no Gradle, no AndroidX).
#
# This is the script that produced dist/SessionTracks-v1.0.0-{debug,release}.apk.
# It uses only the framework SDK: aapt2 + a Java compiler + dx + apksigner.
# The normal way to build this app is Gradle (see ../README.md); this exists
# because the build machine had no network access to the Maven repositories.
#
# Required tools (override with env vars):
#   AAPT2     aapt2 binary
#   ANDROID_JAR  android.jar (API 27 or newer that aapt2 accepts)
#   JAVAC_CMD compiler command; defaults to `javac`
#   DX_JAR    dx.jar (or set D8_JAR to use d8)
#   APKSIGNER apksigner.jar
#   KEYTOOL   keytool binary
#
set -euo pipefail
cd "$(dirname "$0")"

AAPT2="${AAPT2:-aapt2}"
ANDROID_JAR="${ANDROID_JAR:-$ANDROID_HOME/platforms/android-27/android.jar}"
DX_JAR="${DX_JAR:-$ANDROID_HOME/build-tools/28.0.3/lib/dx.jar}"
APKSIGNER="${APKSIGNER:-$ANDROID_HOME/build-tools/28.0.3/lib/apksigner.jar}"
JAVA_CMD="${JAVA_CMD:-java}"
KEYTOOL="${KEYTOOL:-keytool}"

VERSION="1.0.0"
BUILD=build
DIST=dist

rm -rf "$BUILD" "$DIST" out gen
mkdir -p "$BUILD" "$DIST" out gen

echo "[1/7] compiling resources"
"$AAPT2" compile --dir res -o "$BUILD/res.zip"

echo "[2/7] linking resources + generating R.java"
"$AAPT2" link -o "$BUILD/base.apk" -I "$ANDROID_JAR" \
    --manifest AndroidManifest.xml "$BUILD/res.zip" \
    --java gen --min-sdk-version 21 --target-sdk-version 27 --auto-add-overlay

echo "[3/7] compiling java (source/target 8 — dx cannot read newer bytecode)"
find src gen -name '*.java' > "$BUILD/sources.txt"
if [ -n "${JAVAC_CMD:-}" ]; then
    $JAVAC_CMD -source 8 -target 8 -nowarn -encoding UTF-8 \
        -bootclasspath "$ANDROID_JAR" -d out "@$BUILD/sources.txt"
elif [ -n "${ECJ_JAR:-}" ]; then
    "$JAVA_CMD" -cp "$ECJ_JAR" org.eclipse.jdt.internal.compiler.batch.Main \
        -8 -nowarn -proc:none -encoding UTF-8 -cp "$ANDROID_JAR" -d out "@$BUILD/sources.txt"
else
    javac -source 8 -target 8 -nowarn -encoding UTF-8 \
        -bootclasspath "$ANDROID_JAR" -d out "@$BUILD/sources.txt"
fi

echo "[4/7] dexing"
if [ -n "${D8_JAR:-}" ]; then
    "$JAVA_CMD" -cp "$D8_JAR" com.android.tools.r8.D8 --min-api 21 \
        --output "$BUILD" $(find out -name '*.class')
else
    "$JAVA_CMD" -Xmx1024m -cp "$DX_JAR" com.android.dx.command.Main \
        --dex --min-sdk-version=21 --output="$BUILD/classes.dex" out
fi

echo "[5/7] packaging"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
( cd "$BUILD" && zip -q -j unsigned.apk classes.dex )

echo "[6/7] zipaligning"
if command -v zipalign >/dev/null 2>&1; then
    zipalign -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
else
    python3 tools/zipalign.py "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
fi

echo "[7/7] signing"
[ -f "$BUILD/debug.jks" ] || "$KEYTOOL" -genkeypair -keystore "$BUILD/debug.jks" \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android -dname "CN=Android Debug, O=Android, C=US"
[ -f "$BUILD/release.jks" ] || "$KEYTOOL" -genkeypair -keystore "$BUILD/release.jks" \
    -alias sessiontracks -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass sessiontracks -keypass sessiontracks \
    -dname "CN=Session Tracks, OU=App, O=Session Tracks, L=Dhaka, C=BD"

cp "$BUILD/aligned.apk" "$DIST/SessionTracks-v$VERSION-debug.apk"
cp "$BUILD/aligned.apk" "$DIST/SessionTracks-v$VERSION-release.apk"

"$JAVA_CMD" -cp "$APKSIGNER" com.android.apksigner.ApkSignerTool sign \
    --ks "$BUILD/debug.jks" --ks-pass pass:android --key-pass pass:android \
    --v1-signing-enabled true --v2-signing-enabled true --min-sdk-version 21 \
    "$DIST/SessionTracks-v$VERSION-debug.apk"
"$JAVA_CMD" -cp "$APKSIGNER" com.android.apksigner.ApkSignerTool sign \
    --ks "$BUILD/release.jks" --ks-pass pass:sessiontracks --key-pass pass:sessiontracks \
    --v1-signing-enabled true --v2-signing-enabled true --min-sdk-version 21 \
    "$DIST/SessionTracks-v$VERSION-release.apk"

for f in "$DIST"/*.apk; do
    "$JAVA_CMD" -cp "$APKSIGNER" com.android.apksigner.ApkSignerTool verify \
        --verbose --min-sdk-version 21 "$f" | head -3
    ls -lh "$f"
done
echo "done -> $DIST/"
