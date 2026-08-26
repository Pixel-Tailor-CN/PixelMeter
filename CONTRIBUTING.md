# Contributing to Pixel Meter

Thank you for contributing to Pixel Meter.

## Communication Language

English is the primary engineering language for this repository:

- Write code comments, KDoc, and project documentation in English.
- Write new Git commit messages in English.
- English is recommended for Issues and Pull Requests so the wider community can participate, but other languages are welcome when they communicate the problem more clearly.
- User-facing application text remains localized through Android String Resources and Weblate.

## Before Opening an Issue

- Search existing Issues first.
- Include the Android version, device model, Pixel Meter version, and reproduction steps for bugs.
- Mention whether a VPN, notification speed, Live Update, or Overlay was enabled when relevant.
- Do not post private signing credentials, logs containing personal data, or other secrets.

## Development Setup

Pixel Meter is a single-module Android application with a minimum SDK of Android 12 / API 31. Version and dependency declarations live in `gradle/libs.versions.toml`.

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat lint
```

On Unix-like systems:

```bash
./gradlew :app:assembleDebug
./gradlew lint
```

Release builds require signing configuration and are not necessary for ordinary contributions.

## Pull Requests

- Keep each Pull Request focused on one change.
- Explain the motivation, implementation, and validation performed.
- Update architecture or UI documentation when behavior changes.
- Add all supported translations when introducing user-facing String Resources.
- Do not introduce Root, Shizuku, or persistent ADB requirements for core functionality.
- Follow Android background-execution and Foreground Service restrictions.

## Commit Messages

Write new commit messages in English. Prefer a concise imperative or conventional format, for example:

```text
fix: avoid duplicate VPN traffic counting

docs: document the Overlay lifecycle
```

## Validation

For code changes, run at least `:app:assembleDebug` and `lint`. Changes to network statistics, notifications, Live Update, Overlay, permissions, or Foreground Service behavior should also be validated on a Pixel device. Documentation-only changes may use the validation scope agreed for that change.

See `AGENTS.md` and `docs/README.md` for complete project guidance.
