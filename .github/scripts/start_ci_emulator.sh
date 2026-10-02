#!/usr/bin/env bash
set -euo pipefail

mkdir -p artifacts
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"
# This ACL applies only to this disposable runner's KVM device and current user.
test -c /dev/kvm
if [[ ! -r /dev/kvm || ! -w /dev/kvm ]]; then
  sudo setfacl -m "u:$(id -un):rw" /dev/kvm
fi
emulator -accel-check 2>&1 | tee artifacts/emulator-acceleration.txt
grep -qi 'KVM.*usable' artifacts/emulator-acceleration.txt
printf 'no\n' | avdmanager create avd --force --name pixelmeter-ci \
  --package 'system-images;android-33;google_apis;x86_64' --device 'pixel_5'
# A tall, English UI avoids truncating onboarding permission cards.
echo 'hw.keyboard=yes' >> "$HOME/.android/avd/pixelmeter-ci.avd/config.ini"
nohup emulator -avd pixelmeter-ci -port 5554 -no-window -no-audio \
  -no-boot-anim -no-snapshot -wipe-data -accel on -gpu swiftshader \
  -cores 2 -memory 3072 -camera-back none -camera-front none \
  -prop persist.sys.locale=en-US > artifacts/emulator.log 2>&1 &
echo "$!" > artifacts/emulator.pid
timeout 240 adb wait-for-device
for attempt in $(seq 1 120); do
  if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
    adb shell input keyevent KEYCODE_WAKEUP
    adb shell wm dismiss-keyguard
    adb shell settings put global window_animation_scale 0
    adb shell settings put global transition_animation_scale 0
    adb shell settings put global animator_duration_scale 0
    adb shell settings put system screen_off_timeout 1800000
    adb shell wm size 1080x2400
    adb shell wm density 420
    adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
    adb shell getprop ro.build.fingerprint | tee artifacts/emulator-fingerprint.txt
    adb shell getprop ro.product.cpu.abi | tee artifacts/emulator-abi.txt
    exit 0
  fi
  kill -0 "$(cat artifacts/emulator.pid)"
  sleep 2
done
echo 'Emulator failed to complete boot within 240 seconds' >&2
exit 1
