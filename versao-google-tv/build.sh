#!/bin/bash
# Versão Google TV / Android TV.
# Compila, assina e (opcionalmente) instala o launcher na TV.
#   ./build.sh            -> só compila
#   ./build.sh install    -> compila e instala via adb
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
SDK="$HOME/Library/Android/sdk"
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-30/android.jar"

rm -rf build && mkdir -p build/classes build/dex build/gen

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml \
    --min-sdk-version 25 --target-sdk-version 30 -A assets \
    --java build/gen -o build/unsigned.apk build/res.zip

javac -nowarn --release 8 -classpath "$JAR" \
    -d build/classes $(find src build/gen -name '*.java')

"$BT/d8" --min-api 25 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk

KS="$HOME/.android/firelauncher.keystore"
if [ ! -f "$KS" ]; then
    mkdir -p "$HOME/.android"
    keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
        -alias launcher -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Inicio Launcher" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --ks-key-alias launcher \
    --out build/RinoStick-GoogleTV.apk build/aligned.apk
# Kit de instalação (Mac + Windows + guia + app) e o zip para distribuir
KIT=dist/kit-de-instalacao
cp build/RinoStick-GoogleTV.apk dist/RinoStick-GoogleTV.apk && cp build/RinoStick-GoogleTV.apk "$KIT/RinoStick-GoogleTV.apk"
rm -rf "build/RinoStick-GoogleTV-kit" && mkdir -p "build/RinoStick-GoogleTV-kit"
cp "$KIT/COMO-INSTALAR.md" "$KIT/instalar-mac.command" "$KIT/instalar-windows.bat" "$KIT/RinoStick-GoogleTV.apk" "build/RinoStick-GoogleTV-kit/"
(cd build && rm -f "../dist/RinoStick-GoogleTV-kit.zip" && zip -q -r -X "../dist/RinoStick-GoogleTV-kit.zip" "RinoStick-GoogleTV-kit")
echo "OK: build/RinoStick-GoogleTV.apk e dist/RinoStick-GoogleTV-kit.zip"

if [ "${1:-}" = "install" ]; then
    adb install -r build/RinoStick-GoogleTV.apk
    adb shell pm grant com.rinostick.app android.permission.WRITE_SECURE_SETTINGS
    SVC="com.rinostick.app/com.rinostick.app.HomeRedirectService"
    CUR=$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')
    if [[ "$CUR" != *"$SVC"* ]]; then
        if [ -z "$CUR" ] || [ "$CUR" = "null" ]; then NEW="$SVC"; else NEW="$CUR:$SVC"; fi
        adb shell settings put secure enabled_accessibility_services "$NEW"
        adb shell settings put secure accessibility_enabled 1
    fi
    # "Tocando agora": acesso às sessões de mídia
    adb shell cmd notification allow_listener com.rinostick.app/com.rinostick.app.NowPlayingListener || true
    adb shell am start -n com.rinostick.app/.MainActivity
fi
