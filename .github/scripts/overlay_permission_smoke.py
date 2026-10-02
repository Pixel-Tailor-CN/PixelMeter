#!/usr/bin/env python3
"""Issue #30 black-box UI smoke tests for an installed Pixel Meter debug APK.

Requires an unlocked API 33 x86_64 English emulator and Python 3.10+. No external
Python dependencies, instrumentation, root, service-start commands, preference
file edits, or native-library ABI assumptions are used. Every display choice and
monitor start/stop goes through the actual Compose UI. `pm`/`appops` establish
system permissions; `pm clear` isolates cases. Supply --apk to install the build,
or install it before running. --api-level permits a separately configured image.

Example: python3 .github/scripts/overlay_permission_smoke.py --output-dir artifacts
Parser/selector checks without a device: add --self-test.
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import traceback
import unittest
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from typing import Callable


PACKAGE = "vip.mystery0.pixel.meter"
NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"
OVERLAY_LABEL = "Enable Floating Window"
NOTIFICATION_LABEL = "Show on Notification"
FINISH = "Finish and start"
OVERLAY_ERROR = "Overlay permission required for Floating Window"
NOTIFICATION_ERROR = "Grant notification permission to display network speed in notifications."
BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


class SmokeFailure(RuntimeError):
    """An assertion, UI interaction, environment, or ADB command failed."""


def require(condition: object, message: str) -> None:
    if not condition:
        raise SmokeFailure(message)


def bounds(node: ET.Element) -> tuple[int, int, int, int]:
    match = BOUNDS.fullmatch(node.get("bounds", ""))
    return tuple(map(int, match.groups())) if match else (0, 0, 0, 0)


def has_label(node: ET.Element, label: str) -> bool:
    # Compose may merge semantics into a newline-separated content description.
    return any(label == part.strip() for key in ("text", "content-desc")
               for part in node.get(key, "").split("\n"))


def notification_granted(package_dump: str) -> bool:
    values = re.findall(re.escape(NOTIFICATION_PERMISSION) + r": granted=(true|false)",
                        package_dump)
    require(len(set(values)) == 1, "Cannot identify POST_NOTIFICATIONS runtime grant state")
    return values[0] == "true"


def foreground_service(service_dump: str, package: str) -> bool:
    blocks = re.split(r"(?m)(?=^\s*\* ServiceRecord\{)", service_dump)
    for block in blocks:
        header = next((line for line in block.splitlines() if line.strip()), "")
        if package + "/" not in header or "NetworkMonitorService" not in header:
            continue
        # A pending sticky restart / app=null is not a running Foreground Service.
        if (re.search(r"\bisForeground=true\b", block)
                and re.search(r"\bforegroundId=[1-9]\d*\b", block)
                and re.search(r"\bapp=ProcessRecord\{", block)):
            return True
    return False


def visible_overlays(window_dump: str, package: str, screen: tuple[int, int]) -> list[str]:
    matches = []
    for block in re.split(r"(?m)(?=^\s*Window #\d+ Window\{)", window_dump):
        first_line = next((line for line in block.splitlines() if line.strip()), "")
        if not re.match(r"\s*Window #\d+ Window\{", first_line):
            continue
        owned = (re.search(r"\bpackage=" + re.escape(package) + r"(?:\s|$)", block)
                 or re.search(r"\bu\d+ " + re.escape(package) + r"(?:[/ }])", first_line))
        # Android 13 renders the numeric TYPE_APPLICATION_OVERLAY (2038) symbolically.
        overlay_type = re.search(r"\b(?:ty|type)=(?:2038|APPLICATION_OVERLAY)\b", block)
        surface = "mHasSurface=true" in block
        visible = ("isOnScreen=true" in block and "isVisible=true" in block)
        visible = visible or ("mViewVisibility=0x0" in block and "mSurfaceShown=true" in block)
        hidden = ("mViewVisibility=0x8" in block or "mViewVisibility=0x4" in block
                  or "mObscured=true" in block or "isOnScreen=false" in block
                  or "isVisible=false" in block
                  or re.search(r"\bmShownAlpha=0(?:\.0+)?(?:\s|$)", block))
        frames = re.findall(r"\b(?:mFrame|frame)=(\[-?\d+,-?\d+\]\[-?\d+,-?\d+\])", block)
        on_display = any(
            min(right, screen[0]) > max(left, 0) and min(bottom, screen[1]) > max(top, 0)
            for left, top, right, bottom in
            (tuple(map(int, BOUNDS.fullmatch(frame).groups())) for frame in frames)
        )
        if owned and overlay_type and surface and visible and not hidden and on_display:
            matches.append(block)
    return matches


@dataclass
class UiTree:
    root: ET.Element
    package: str
    screen: tuple[int, int]

    def __post_init__(self) -> None:
        self.parents = {child: parent for parent in self.root.iter() for child in parent}

    def visible(self, node: ET.Element) -> bool:
        left, top, right, bottom = bounds(node)
        return (0 <= left < right <= self.screen[0] and 0 <= top < bottom <= self.screen[1]
                and node.get("package") == self.package)

    def labels(self, label: str) -> list[ET.Element]:
        return [node for node in self.root.iter("node")
                if has_label(node, label) and self.visible(node)]

    def button(self, label: str) -> ET.Element | None:
        for leaf in self.labels(label):
            node = leaf
            while node is not None:
                if (node.get("clickable") == "true"
                        or node.get("class", "").endswith("Button")) and self.visible(node):
                    return node
                node = self.parents.get(node)
        return None

    def switch(self, label: str) -> ET.Element | None:
        for leaf in self.labels(label):
            # Prefer the semantic row/card. Never use a global switch index: Home
            # and Onboarding order these controls differently, and one is disabled.
            node = leaf
            while node is not None:
                candidates = [candidate for candidate in node.iter("node")
                              if candidate.get("checkable") == "true" and self.visible(candidate)]
                if len(candidates) == 1:
                    return candidates[0]
                if len(candidates) > 1:
                    break
                node = self.parents.get(node)
            # Compose can flatten the non-semantic row out of the accessibility
            # tree. Associate only a right-hand switch beside this label's row.
            left, top, right, bottom = bounds(leaf)
            candidates = []
            other_titles = [bounds(n)[1] for n in self.root.iter("node")
                            if n is not leaf and any(has_label(n, title) for title in
                            (OVERLAY_LABEL, NOTIFICATION_LABEL, "Enable Live Update"))
                            and bounds(n)[1] > top]
            row_bottom = min(other_titles, default=top + int(self.screen[1] * .18))
            for candidate in self.root.iter("node"):
                if candidate.get("checkable") != "true" or not self.visible(candidate):
                    continue
                x1, y1, x2, y2 = bounds(candidate)
                center_y = (y1 + y2) / 2
                if x1 >= right and top <= center_y < row_bottom:
                    candidates.append((abs(center_y - (top + bottom) / 2), candidate))
            if candidates:
                candidates.sort(key=lambda item: item[0])
                return candidates[0][1]
        return None


class Harness:
    def __init__(self, args: argparse.Namespace):
        self.args = args
        self.package = args.package
        self.adb = [args.adb] + (["-s", args.serial] if args.serial else [])
        self.output = Path(args.output_dir)
        self.output.mkdir(parents=True, exist_ok=True)
        self.case_dir = self.output
        self.deadline = time.monotonic() + args.case_timeout
        self.screen = (1080, 1920)
        self.launcher = ""
        self.events: list[dict] = []
        self.checkpoint_index = 0

    def note(self, message: str, **details: object) -> None:
        event = {"time": time.time(), "message": message, **details}
        self.events.append(event)
        print(message, flush=True)

    def command(self, *args: str, timeout: float = 30, allowed: tuple[int, ...] = (0,),
                binary: bool = False) -> str | bytes:
        remaining = self.deadline - time.monotonic()
        require(remaining > 0, "Case deadline exceeded")
        command = self.adb + list(args)
        start = time.monotonic()
        entry: dict = {"command": command, "time": time.time()}
        try:
            result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                    timeout=min(timeout, remaining), check=False)
            entry.update(returncode=result.returncode,
                         stdout=f"<{len(result.stdout)} binary bytes>" if binary else
                         result.stdout.decode("utf-8", errors="replace"),
                         stderr=result.stderr.decode("utf-8", errors="replace"))
            require(result.returncode in allowed,
                    f"ADB command failed ({result.returncode}): {' '.join(command)}\n"
                    f"{entry['stdout']}\n{entry['stderr']}")
            return result.stdout if binary else entry["stdout"]
        except (OSError, subprocess.TimeoutExpired) as error:
            entry["error"] = str(error)
            raise SmokeFailure(f"ADB command could not complete: {' '.join(command)}: {error}") from error
        finally:
            entry["duration_seconds"] = round(time.monotonic() - start, 3)
            with (self.case_dir / "commands.jsonl").open("a", encoding="utf-8") as file:
                file.write(json.dumps(entry) + "\n")

    def shell(self, *args: str, **kwargs) -> str:
        return self.command("shell", *args, **kwargs)

    def wait(self, description: str, probe: Callable[[], object], timeout: float = 20) -> object:
        deadline = min(self.deadline, time.monotonic() + timeout)
        while time.monotonic() < deadline:
            result = probe()
            if result:
                return result
            time.sleep(.5)
        raise SmokeFailure(f"Timed out waiting for {description}")

    def tree(self) -> UiTree:
        # Unique destination prevents a failed dump from reading a stale hierarchy.
        remote = f"/sdcard/pixelmeter-smoke-{os.getpid()}.xml"
        last_error = ""
        for _ in range(3):
            self.shell("rm", "-f", remote)
            dump = self.shell("uiautomator", "dump", "--compressed", remote, timeout=25)
            if "dumped to" not in dump:
                last_error = dump
                time.sleep(.5)
                continue
            xml = self.command("exec-out", "cat", remote)
            try:
                root = ET.fromstring(xml)
                return UiTree(root, self.package, self.screen)
            except ET.ParseError as error:
                last_error = str(error)
        raise SmokeFailure(f"Cannot capture UI hierarchy after bounded retries: {last_error}")

    def swipe(self, tree: UiTree, down: bool) -> None:
        containers = [node for node in tree.root.iter("node")
                      if node.get("scrollable") == "true" and tree.visible(node)]
        require(containers, "No visible app scroll container while finding a UI control")
        node = max(containers, key=lambda item: (bounds(item)[2] - bounds(item)[0]) *
                   (bounds(item)[3] - bounds(item)[1]))
        left, top, right, bottom = bounds(node)
        x = int(left + .62 * (right - left))
        low, high = int(top + .78 * (bottom - top)), int(top + .25 * (bottom - top))
        start, end = (low, high) if down else (high, low)
        self.shell("input", "swipe", str(x), str(start), str(x), str(end), "350")
        time.sleep(.2)

    def find(self, label: str, kind: str = "label") -> tuple[UiTree, ET.Element]:
        # Both directions are bounded; transitions often preserve scroll offset.
        for direction in (True, False):
            for _ in range(9):
                tree = self.tree()
                if kind == "button":
                    node = tree.button(label)
                elif kind == "switch":
                    node = tree.switch(label)
                else:
                    matches = tree.labels(label)
                    node = matches[0] if matches else None
                if node is not None:
                    return tree, node
                self.swipe(tree, down=direction)
        raise SmokeFailure(f"UI {kind} not found: {label!r}")

    def tap(self, node: ET.Element) -> None:
        require(node.get("enabled") == "true", f"Refusing to tap disabled control: {node.attrib}")
        left, top, right, bottom = bounds(node)
        require(right > left and bottom > top, "Control has empty bounds")
        self.shell("input", "tap", str((left + right) // 2), str((top + bottom) // 2))
        time.sleep(.25)

    def button(self, label: str, enabled: bool | None = None) -> ET.Element:
        _, node = self.find(label, "button")
        if enabled is not None:
            require((node.get("enabled") == "true") == enabled,
                    f"Expected {label!r} enabled={enabled}; got {node.attrib}")
        return node

    def click(self, label: str) -> None:
        self.note(f"Tap UI button: {label}")
        self.tap(self.button(label, enabled=True))

    def set_switch(self, label: str, checked: bool) -> None:
        _, node = self.find(label, "switch")
        require(node.get("checked") in ("true", "false"), f"No checked semantics for {label}")
        if (node.get("checked") == "true") != checked:
            self.note(f"Tap UI switch: {label} -> {checked}")
            self.tap(node)
        self.assert_switch(label, checked)

    def assert_switch(self, label: str, checked: bool) -> None:
        _, node = self.find(label, "switch")
        require((node.get("checked") == "true") == checked,
                f"Expected {label!r} checked={checked}; got {node.attrib}")

    def package_dump(self) -> str:
        return self.shell("dumpsys", "package", self.package)

    def permissions(self, overlay: bool, notification: bool) -> None:
        self.shell("appops", "set", "--user", "0", self.package,
                   "SYSTEM_ALERT_WINDOW", "allow" if overlay else "deny")
        operation = self.shell("appops", "get", "--user", "0", self.package, "SYSTEM_ALERT_WINDOW")
        require(re.search(r"SYSTEM_ALERT_WINDOW: " + ("allow" if overlay else "deny") + r"\b",
                          operation), f"Overlay app-op was not applied: {operation}")
        self.notification_permission(notification)

    def notification_permission(self, granted: bool) -> None:
        self.shell("pm", "clear-permission-flags", "--user", "0", self.package,
                   NOTIFICATION_PERMISSION, "user-set", "user-fixed")
        self.shell("pm", "grant" if granted else "revoke", "--user", "0",
                   self.package, NOTIFICATION_PERMISSION)
        require(notification_granted(self.package_dump()) == granted,
                f"Expected notification permission granted={granted}")
        self.note(f"System notification permission granted={granted}")

    def launch(self) -> None:
        output = self.shell("am", "start", "-W", "-a", "android.intent.action.MAIN",
                            "-c", "android.intent.category.LAUNCHER", "-f", "0x10200000",
                            "-n", self.package + "/.MainActivity")
        require("Error:" not in output and "Status: ok" in output,
                f"MainActivity launch failed: {output}")
        time.sleep(.8)

    def reset(self, overlay: bool = True, notification: bool = False) -> None:
        self.shell("am", "force-stop", self.package)
        require("Success" in self.shell("pm", "clear", "--user", "0", self.package),
                "pm clear failed; cases cannot be isolated")
        self.permissions(overlay, notification)
        self.launch()
        self.find("Set up Pixel Meter")
        self.assert_runtime(False, False, duration=1)

    def onboarding(self, overlay: bool, notification: bool, allowed: bool = True) -> None:
        self.click("Start setup")
        self.set_switch(NOTIFICATION_LABEL, notification)
        self.set_switch(OVERLAY_LABEL, overlay)
        self.click("Next")
        self.button(FINISH, enabled=allowed)
        self.checkpoint("onboarding-permissions")
        self.click(FINISH if allowed else "Grant later")
        self.find("Monitor is Running" if allowed else "Monitor is Stopped")

    def service_dump(self) -> str:
        return self.shell("dumpsys", "activity", "services", self.package)

    def runtime(self) -> tuple[bool, bool]:
        foreground = foreground_service(self.service_dump(), self.package)
        overlays = visible_overlays(self.shell("dumpsys", "window", "windows"),
                                    self.package, self.screen)
        require(len(overlays) <= 1, f"Duplicate visible overlay windows: {len(overlays)}")
        return foreground, bool(overlays)

    def assert_runtime(self, foreground: bool, overlay: bool, duration: float = 3) -> None:
        expected = (foreground, overlay)
        self.wait(f"runtime state FGS={foreground}, overlay={overlay}",
                  lambda: self.runtime() == expected)
        # Multiple independent samples catch immediate shutdowns and reappearance.
        until = time.monotonic() + duration
        while time.monotonic() < until:
            actual = self.runtime()
            require(actual == expected, f"Runtime state changed: expected {expected}, got {actual}")
            time.sleep(.5)
        self.note(f"Runtime verified: Foreground Service={foreground}, visible overlay={overlay}")

    def home(self, foreground: bool = True, overlay: bool = True) -> None:
        self.shell("input", "keyevent", "KEYCODE_HOME")
        self.wait("launcher focus", lambda: any(
            self.launcher + "/" in line for line in
            self.shell("dumpsys", "window", "windows").splitlines()
            if "mCurrentFocus=" in line))
        self.assert_runtime(foreground, overlay)
        self.checkpoint("launcher")

    def pids(self) -> set[str]:
        return set(self.shell("pidof", self.package, allowed=(0, 1)).split())

    def capture(self, destination: Path) -> list[str]:
        destination.mkdir(parents=True, exist_ok=True)
        errors = []
        captures = {
            "services.txt": lambda: self.service_dump(),
            "windows.txt": lambda: self.shell("dumpsys", "window", "windows"),
            "package.txt": lambda: self.package_dump(),
            "appops.txt": lambda: self.shell("appops", "get", "--user", "0", self.package),
            "activities.txt": lambda: self.shell("dumpsys", "activity", "activities"),
            "notifications.txt": lambda: self.shell("dumpsys", "notification", "--noredact"),
            "logcat.txt": lambda: self.command("logcat", "-d", "-v", "threadtime"),
            "ui.xml": lambda: ET.tostring(self.tree().root, encoding="unicode"),
            "screenshot.png": lambda: self.command("exec-out", "screencap", "-p", binary=True),
        }
        for name, capture in captures.items():
            try:
                value = capture()
                if isinstance(value, bytes):
                    require(value.startswith(b"\x89PNG\r\n\x1a\n"), "screencap did not return PNG data")
                    (destination / name).write_bytes(value)
                else:
                    (destination / name).write_text(value, encoding="utf-8")
            except Exception as error:
                errors.append(f"{name}: {error}")
        (destination / "capture-errors.json").write_text(json.dumps(errors, indent=2), encoding="utf-8")
        return errors

    def checkpoint(self, name: str) -> None:
        self.checkpoint_index += 1
        destination = self.case_dir / f"{self.checkpoint_index:02d}-{name}"
        errors = self.capture(destination)
        require(not errors, "Checkpoint diagnostics failed: " + "; ".join(errors))

    def preflight(self) -> None:
        require(self.command("get-state").strip() == "device", "ADB device is not ready")
        sdk = self.shell("getprop", "ro.build.version.sdk").strip()
        abi = self.shell("getprop", "ro.product.cpu.abilist").strip()
        locale = self.shell("getprop", "persist.sys.locale").strip()
        if not locale:
            locale = self.shell("getprop", "ro.product.locale").strip()
        require(sdk == str(self.args.api_level),
                f"Expected API {self.args.api_level}, found {sdk}")
        require("x86_64" in abi.split(","), f"Expected x86_64 emulator, found {abi}")
        require(locale.lower().startswith("en"), f"English UI required; found locale {locale!r}")
        if self.args.apk:
            apk = Path(self.args.apk).resolve()
            require(apk.is_file(), f"APK does not exist: {apk}")
            installed = self.command("install", "-r", str(apk), timeout=120)
            require("Success" in installed, f"APK install did not succeed: {installed}")
        require("package:" in self.shell("pm", "path", self.package), "Test APK is not installed")
        sizes = re.findall(r"(?:Physical|Override) size: (\d+)x(\d+)", self.shell("wm", "size"))
        require(sizes, "Cannot read emulator display size")
        self.screen = tuple(map(int, sizes[-1]))
        resolved = self.shell("cmd", "package", "resolve-activity", "--brief", "--user", "0",
                              "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
        components = re.findall(r"(?m)^([\w.]+)/(\S+)\s*$", resolved)
        require(components, f"Cannot resolve launcher: {resolved}")
        self.launcher = components[-1][0]
        self.shell("input", "keyevent", "KEYCODE_WAKEUP")
        self.shell("wm", "dismiss-keyguard")
        (self.output / "device.json").write_text(json.dumps({
            "sdk": sdk, "abis": abi, "locale": locale, "screen": self.screen,
            "launcher": self.launcher, "package": self.package,
            "fingerprint": self.shell("getprop", "ro.build.fingerprint").strip(),
        }, indent=2), encoding="utf-8")


def onboarding_overlay_only(h: Harness) -> None:
    h.reset()
    h.onboarding(overlay=True, notification=False)
    h.home()


def onboarding_both(h: Harness) -> None:
    h.reset()
    h.onboarding(overlay=True, notification=True)
    h.home()


def main_start_both(h: Harness) -> None:
    h.reset()
    h.click("Skip")
    h.find("Monitor is Stopped")
    h.set_switch(OVERLAY_LABEL, True)
    h.set_switch(NOTIFICATION_LABEL, True)
    h.click("Start")
    h.find("Monitor is Running")
    h.home()


def toggle_notifications(h: Harness) -> None:
    h.reset()
    h.onboarding(overlay=True, notification=False)
    h.home()
    for iteration in range(3):
        for enabled in (True, False):
            h.launch()
            h.set_switch(NOTIFICATION_LABEL, enabled)
            if enabled:
                h.find(NOTIFICATION_ERROR)
            h.home()
            require(not notification_granted(h.package_dump()), "Notification grant changed unexpectedly")
            h.note(f"Notification toggle cycle {iteration + 1}, enabled={enabled}: overlay survived")


def force_stop_restart(h: Harness) -> None:
    h.reset()
    h.onboarding(overlay=True, notification=True)
    h.home()
    h.shell("am", "force-stop", h.package)
    h.assert_runtime(False, False)
    require(not h.pids(), "App process survived force-stop")
    h.checkpoint("force-stopped")
    h.launch()
    h.find("Monitor is Stopped")
    h.assert_switch(OVERLAY_LABEL, True)
    h.assert_switch(NOTIFICATION_LABEL, True)
    h.click("Start")
    h.find("Monitor is Running")
    h.home()


def grant_revoke_restart(h: Harness) -> None:
    h.reset()
    h.onboarding(overlay=True, notification=True)
    h.home()
    h.notification_permission(True)
    h.home()
    old_pids = h.pids()
    require(old_pids, "No app process before notification revocation")
    h.notification_permission(False)
    h.wait("OS to kill the previous process after runtime permission revocation",
           lambda: old_pids.isdisjoint(h.pids()))
    h.note("Expected Android permission-revocation process kill observed", previous_pids=sorted(old_pids))
    h.checkpoint("after-os-permission-revocation")
    h.launch()
    h.assert_switch(OVERLAY_LABEL, True)
    h.assert_switch(NOTIFICATION_LABEL, True)
    # START_STICKY may already have restarted the service. Prove an actual Main
    # Start either way, without falsely treating Android's permission kill as a bug.
    if foreground_service(h.service_dump(), h.package):
        h.note("Android restarted the sticky service; cycling it through Main UI")
        h.click("Stop")
        h.assert_runtime(False, False)
    h.find("Monitor is Stopped")
    h.click("Start")
    h.find("Monitor is Running")
    h.home()
    require(not notification_granted(h.package_dump()), "Notification permission unexpectedly restored")


def overlay_denied(h: Harness, notification: bool) -> None:
    h.reset(overlay=False)
    h.onboarding(overlay=True, notification=notification, allowed=False)
    h.assert_runtime(False, False)
    h.click("Start")
    h.find(OVERLAY_ERROR)
    h.find("Monitor is Stopped")
    h.home(foreground=False, overlay=False)


def notification_only_denied(h: Harness) -> None:
    h.reset(overlay=False)
    h.onboarding(overlay=False, notification=True, allowed=False)
    h.click("Start")
    h.find(NOTIFICATION_ERROR)
    h.find("Monitor is Stopped")
    h.home(foreground=False, overlay=False)
    h.notification_permission(True)
    h.launch()
    h.click("Start")
    h.find("Monitor is Running")
    h.home(foreground=True, overlay=False)


def notification_only_granted(h: Harness) -> None:
    h.reset(overlay=False, notification=True)
    h.onboarding(overlay=False, notification=True)
    h.home(foreground=True, overlay=False)


def stop_start_regression(h: Harness) -> None:
    h.reset()
    h.onboarding(overlay=True, notification=False)
    h.home()
    for _ in range(2):
        h.launch()
        h.click("Stop")
        h.find("Monitor is Stopped")
        h.home(foreground=False, overlay=False)
        h.launch()
        h.click("Start")
        h.find("Monitor is Running")
        h.home()


CASES: dict[str, Callable[[Harness], None]] = {
    "onboarding_overlay_only_notification_denied": onboarding_overlay_only,
    "onboarding_both_displays_notification_denied": onboarding_both,
    "main_start_both_displays_notification_denied": main_start_both,
    "repeated_notification_toggles_keep_overlay": toggle_notifications,
    "force_stop_then_main_ui_restart": force_stop_restart,
    "notification_grant_revoke_os_kill_then_ui_restart": grant_revoke_restart,
    "overlay_only_both_permissions_denied_blocks": lambda h: overlay_denied(h, False),
    "both_displays_both_permissions_denied_blocks": lambda h: overlay_denied(h, True),
    "notification_only_denied_blocks_then_granted_starts": notification_only_denied,
    "notification_only_granted_onboarding_starts": notification_only_granted,
    "overlay_stop_start_regression": stop_start_regression,
}


def crash_in_log(log: str, package: str) -> bool:
    return bool(re.search(r"FATAL EXCEPTION:[\s\S]{0,1600}?Process: " + re.escape(package)
                          + r"(?:,|\s)", log))


def application_errors(log: str, package: str) -> list[str]:
    errors = []
    if crash_in_log(log, package):
        errors.append("App FATAL EXCEPTION found in logcat")
    if re.search(r"\bANR in " + re.escape(package) + r"(?:\s|$|\()", log):
        errors.append("App ANR found in logcat")
    for line in log.splitlines():
        if "NetworkMonitorService" in line and any(message in line for message in (
            "onStartCommand: start foreground error", "startMonitoring: overlay window error",
        )):
            errors.append("Caught app service error: " + line.strip())
        if "MainViewModel" in line and "startService: start foreground service error" in line:
            errors.append("Caught app UI service-start error: " + line.strip())
    return errors


def save_reports(output: Path, results: list[dict]) -> None:
    failures = sum(result["status"] == "failed" for result in results)
    payload = {"suite": "PixelMeter issue #30 API 33 UI smoke", "cases": results,
               "tests": len(results), "failures": failures}
    (output / "results.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
    suite = ET.Element("testsuite", name=payload["suite"], tests=str(len(results)),
                       failures=str(failures), errors="0", skipped="0",
                       time=str(round(sum(result["duration_seconds"] for result in results), 3)))
    for result in results:
        case = ET.SubElement(suite, "testcase", classname="PixelMeter.OverlayPermissionSmoke",
                             name=result["name"], time=str(result["duration_seconds"]))
        if result["status"] == "failed":
            ET.SubElement(case, "failure", message=result["error"].splitlines()[0]).text = result["error"]
        ET.SubElement(case, "system-out").text = "Artifacts: " + result["artifacts"]
    ET.ElementTree(suite).write(output / "junit.xml", encoding="utf-8", xml_declaration=True)


def run(args: argparse.Namespace) -> int:
    h = Harness(args)
    results = []
    preflight_error = ""
    try:
        h.preflight()
    except Exception:
        preflight_error = traceback.format_exc()
    for name, case in CASES.items():
        if args.case and name not in args.case:
            continue
        h.case_dir = h.output / name
        h.case_dir.mkdir(parents=True, exist_ok=True)
        h.events, h.checkpoint_index = [], 0
        h.deadline = time.monotonic() + args.case_timeout
        started = time.monotonic()
        error = ""
        h.note(f"CASE {name}")
        try:
            require(not preflight_error, "Preflight failed:\n" + preflight_error)
            h.command("logcat", "-c")
            case(h)
        except Exception:
            error = traceback.format_exc()
        finally:
            # A failed assertion/deadline must not prevent final evidence or later cases.
            h.deadline = time.monotonic() + 90
            diagnostic_errors = h.capture(h.case_dir / "final")
            if diagnostic_errors:
                error += "\nDiagnostic capture errors: " + "; ".join(diagnostic_errors)
            log = h.case_dir / "final" / "logcat.txt"
            if log.exists():
                for problem in application_errors(log.read_text(encoding="utf-8"), h.package):
                    error += "\n" + problem + "; inspect final/logcat.txt"
            (h.case_dir / "events.json").write_text(json.dumps(h.events, indent=2), encoding="utf-8")
            result = {"name": name, "status": "failed" if error else "passed", "error": error.strip(),
                      "duration_seconds": round(time.monotonic() - started, 3),
                      "artifacts": str(h.case_dir)}
            results.append(result)
            save_reports(h.output, results)
            print(f"{result['status'].upper()}: {name}" + (f"\n{error}" if error else ""), flush=True)
    return 1 if any(result["status"] == "failed" for result in results) else 0


class ParserTests(unittest.TestCase):
    def test_running_service_is_not_a_pending_restart(self):
        dump = ("ACTIVITY MANAGER SERVICES\n\n  * ServiceRecord{abc u0 " + PACKAGE
                + "/.service.NetworkMonitorService}\n    isForeground=true foregroundId=1001\n"
                "    app=ProcessRecord{def 1234:" + PACKAGE + "/u0a123}\n")
        self.assertTrue(foreground_service(dump, PACKAGE))
        self.assertFalse(foreground_service(dump.replace("isForeground=true", "isForeground=false"), PACKAGE))
        self.assertFalse(foreground_service(dump.replace("app=ProcessRecord{", "app=null #"), PACKAGE))
        self.assertFalse(foreground_service(dump.replace("foregroundId=1001", "foregroundId=0"), PACKAGE))

    def test_overlay_requires_owned_visible_drawn_window_on_screen(self):
        dump = ("WINDOW MANAGER WINDOWS\n\n  Window #0 Window{abc u0 " + PACKAGE + "}:\n"
                "    mAttrs={(0,100)(wrapxwrap) ty=APPLICATION_OVERLAY}\n"
                "    mHasSurface=true mViewVisibility=0x0 mObscured=false\n"
                "    isOnScreen=true isVisible=true\n"
                "    mFrame=[0,100][120,160]\n")
        self.assertEqual(len(visible_overlays(dump, PACKAGE, (1080, 1920))), 1)
        self.assertEqual(len(visible_overlays(dump.replace("APPLICATION_OVERLAY", "2038"), PACKAGE, (1080, 1920))), 1)
        for old, new in ((PACKAGE, "another.app"), ("APPLICATION_OVERLAY", "BASE_APPLICATION"),
                         ("mHasSurface=true", "mHasSurface=false"), ("isVisible=true", "isVisible=false"),
                         ("mObscured=false", "mObscured=true"), ("[0,100][120,160]", "[0,2100][120,2160]")):
            self.assertFalse(visible_overlays(dump.replace(old, new), PACKAGE, (1080, 1920)))

    def test_permission_parser_does_not_match_requested_permissions(self):
        self.assertFalse(notification_granted(NOTIFICATION_PERMISSION + "\n  " + NOTIFICATION_PERMISSION
                                              + ": granted=false, flags=[USER_SET]"))
        self.assertTrue(notification_granted(NOTIFICATION_PERMISSION + ": granted=true, flags=[]"))
        with self.assertRaises(SmokeFailure):
            notification_granted(NOTIFICATION_PERMISSION)

    def test_compose_flattened_switches_and_button_parent(self):
        root = ET.fromstring('<hierarchy><node package="' + PACKAGE + '" bounds="[0,0][1080,1920]">'
            '<node text="Show on Notification" bounds="[40,400][700,445]" />'
            '<node checkable="true" checked="false" bounds="[900,415][1030,475]" />'
            '<node text="Enable Live Update" bounds="[40,500][700,545]" />'
            '<node checkable="true" checked="false" enabled="false" bounds="[900,515][1030,575]" />'
            '<node text="Enable Floating Window" bounds="[40,700][700,745]" />'
            '<node checkable="true" checked="true" bounds="[900,715][1030,775]" />'
            '<node clickable="true" enabled="false" bounds="[400,850][1030,950]">'
            '<node text="Finish and start" bounds="[450,870][1000,920]" /></node>'
            '</node></hierarchy>')
        for node in root.iter("node"):
            node.set("package", PACKAGE)
        tree = UiTree(root, PACKAGE, (1080, 1920))
        self.assertEqual(bounds(tree.switch(NOTIFICATION_LABEL)), (900, 415, 1030, 475))
        self.assertEqual(bounds(tree.switch(OVERLAY_LABEL)), (900, 715, 1030, 775))
        self.assertEqual(tree.button(FINISH).get("enabled"), "false")

    def test_crash_parser_ignores_other_apps(self):
        self.assertTrue(crash_in_log("FATAL EXCEPTION: main\nProcess: " + PACKAGE + ", PID: 1234", PACKAGE))
        self.assertFalse(crash_in_log("FATAL EXCEPTION: main\nProcess: other.app, PID: 1234", PACKAGE))

    def test_caught_errors_anr_and_expected_permission_kill(self):
        self.assertTrue(application_errors("E NetworkMonitorService: onStartCommand: start foreground error", PACKAGE))
        self.assertTrue(application_errors("W NetworkMonitorService: startMonitoring: overlay window error", PACKAGE))
        self.assertTrue(application_errors("E ActivityManager: ANR in " + PACKAGE + " (" + PACKAGE + ")", PACKAGE))
        self.assertFalse(application_errors("I ActivityManager: Killing 1234:" + PACKAGE
                                            + "/u0a123: permissions revoked", PACKAGE))
        self.assertFalse(application_errors("W NetworkMonitorService: Notification permission changed; keeping monitoring active", PACKAGE))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
    parser.add_argument("--package", default=PACKAGE)
    parser.add_argument("--apk", help="Install this APK with adb install -r before running")
    parser.add_argument("--api-level", type=int, default=33)
    parser.add_argument("--output-dir", default="artifacts/overlay-permission-smoke")
    parser.add_argument("--case-timeout", type=float, default=300)
    parser.add_argument("--case", action="append", choices=CASES, help="Run only named case(s)")
    parser.add_argument("--list-cases", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        return 0 if unittest.TextTestRunner(verbosity=2).run(
            unittest.defaultTestLoader.loadTestsFromTestCase(ParserTests)).wasSuccessful() else 1
    if args.list_cases:
        print("\n".join(CASES))
        return 0
    parser.error("--case-timeout must be positive") if args.case_timeout <= 0 else None
    return run(args)


if __name__ == "__main__":
    sys.exit(main())
