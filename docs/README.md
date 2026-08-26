# Pixel Meter Documentation

This directory contains Pixel Meter's long-term architecture, UI, maintenance, and implementation documentation. Source code, the manifest, resource directories, and `gradle/libs.versions.toml` remain the final sources of truth. Documentation must be updated alongside relevant changes.

## Architecture

- [Architecture overview](architecture/overview.md): modules, dependencies, and primary data flows.
- [Network data source](architecture/network-data-source.md): physical-interface detection, VPN exclusion, and `TrafficStats` access.
- [Service lifecycle](architecture/service-lifecycle.md): Foreground Service behavior, notifications, screen-off handling, and boot startup.
- [Preferences](architecture/preferences.md): DataStore keys, defaults, and upgrade compatibility.
- [Localization](architecture/localization.md): Locale Config, resource constraints, and the Weblate workflow.

## UI

- [Design system](ui/design-system.md): Material 3, theming, and responsive settings layouts.
- [Onboarding](ui/onboarding.md): setup flow, permissions, and completion behavior.
- [Notifications](ui/notification.md): Bitmap icons, low-traffic mode, and Live Update.
- [Overlay](ui/overlay.md): the WindowManager and Compose host, plus interaction behavior.

## Designs and Implementation Plans

Future requirement designs and implementation plans belong in [`plans/`](plans/README.md).

- Design: `YYYY-MM-DD-topic-design.md`
- Implementation plan: `YYYY-MM-DD-topic-plan.md`

## Images

- `Screenshot_CN.png`: Chinese UI screenshot.
- `Screenshot_EN.png`: English UI screenshot.
- `Component.png`: notification or component preview.

## Maintenance Requirements

Review and update the relevant documentation when changing:

- Network-interface filtering or speed calculations.
- Foreground Service behavior, permissions, notifications, or background execution.
- DataStore keys, defaults, or migration behavior.
- Onboarding, settings, notifications, or Overlay interactions.
- Supported locales or translation workflows.
- SDK, Kotlin, AGP, Compose, or major dependency versions.
