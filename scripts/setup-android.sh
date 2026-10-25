#!/usr/bin/env bash
# Sets up everything the Android tests need: SDK packages, an emulator with Chrome, and the pinned Appium.
# Safe to re-run. Asks you to accept Google's Android SDK licences.
set -euo pipefail

AVD_NAME="${ANDROID_AVD:-sg-pixel}"
API=35
ARCH=$([ "$(uname -m)" = "arm64" ] && echo arm64-v8a || echo x86_64)
IMAGE="system-images;android-${API};google_apis;${ARCH}"

# Find the SDK: ANDROID_HOME, then Homebrew, then Android Studio's default
for dir in "${ANDROID_HOME:-}" /opt/homebrew/share/android-commandlinetools /usr/local/share/android-commandlinetools "$HOME/Library/Android/sdk"; do
  if [ -n "$dir" ] && [ -d "$dir/cmdline-tools" ]; then export ANDROID_HOME="$dir"; break; fi
done
if [ -z "${ANDROID_HOME:-}" ] || ! command -v sdkmanager >/dev/null; then
  echo "Android command-line tools not found. On macOS: brew install --cask android-commandlinetools"
  exit 1
fi
echo "Android SDK: $ANDROID_HOME"

echo "== Android SDK licences (read and accept to continue)"
sdkmanager --licenses

echo "== SDK packages (emulator image is ~1.5 GB)"
sdkmanager "platform-tools" "emulator" "platforms;android-${API}" "$IMAGE"

if avdmanager list avd -c | grep -qx "$AVD_NAME"; then
  echo "== Emulator '$AVD_NAME' already exists"
else
  echo "== Creating emulator '$AVD_NAME'"
  # prints a harmless "Could not load devices ... devices.xml" error on some images
  echo no | avdmanager create avd -n "$AVD_NAME" -k "$IMAGE" -d pixel_7 || true
  avdmanager list avd -c | grep -qx "$AVD_NAME" || { echo "Emulator '$AVD_NAME' was not created"; exit 1; }
fi

echo "== Appium (pinned in package.json)"
cd "$(dirname "$0")/.."
npm ci --no-audit --no-fund
APPIUM_HOME="$PWD" npx appium driver list --installed

cat <<EOF

Done. Add this to your shell profile so the tests can find the SDK:
  export ANDROID_HOME="$ANDROID_HOME"

Run the Android tests (the emulator is started automatically):
  ./mvnw test -Pandroid
EOF
