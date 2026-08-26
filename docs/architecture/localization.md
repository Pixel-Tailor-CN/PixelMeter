# Localization

## 1. Supported Locales

The resource directories currently include:

- `values/`: default English resources
- `values-zh-rCN/`: Simplified Chinese
- `values-pt/`: Portuguese
- `values-pt-rBR/`: Brazilian Portuguese
- `values-ru/`: Russian

Brand resources such as `app_name` may be marked as non-translatable.

## 2. Default Language

`app/src/main/res/resources.properties` defines:

```properties
unqualifiedResLocale=en
```

Therefore, `values/strings.xml` must contain the complete English fallback resources. Do not place text intended only for Chinese locales in the default resource directory.

`translatable="false"` only excludes a string from translation workflows. It does not limit the string to a specific Locale.

## 3. Locale Config

`app/build.gradle.kts` performs the following steps:

1. Reads `unqualifiedResLocale`.
2. Scans `values-*` directories that contain `strings.xml`.
3. Validates Android Locale qualifiers.
4. Configures `localeFilters`.
5. Enables `generateLocaleConfig=true` to generate the App Locale Config.

Adding a language does not require a manually maintained Locale Config, but the directory name must use a valid Android qualifier.

## 4. Weblate

Application strings are managed through Weblate. Translation changes may arrive from a Weblate branch or pull request.

When adding text:

- Use stable, semantically precise String Resource keys.
- Express the complete meaning in the default English resource first.
- Preserve the number and types of format arguments such as `%1$s` and `%1$d`.
- Escape XML special characters correctly.
- Add translations for every current Locale, or Lint will report `MissingTranslation`.
- Do not hard-code user-facing or accessibility text in Kotlin or Compose.

## 5. Values and Units

`SpeedFormatter` uses the system Locale for decimal formatting, while speed units remain technical abbreviations:

- B/s
- KB/s
- MB/s
- GB/s
- Live Update uses the shorter K/s, M/s, and G/s forms.

These unit formats are constrained by status-bar space and are not localized through String Resources.

## 6. RTL and Accessibility

The manifest declares `supportsRtl=true`. New layouts should use Start and End instead of fixed Left and Right unless an option explicitly refers to physical direction.

Icon buttons, notification channel descriptions, Tile states, and other accessibility labels must come from String Resources.

Before release, use pseudolocales or an RTL Locale to check:

- Long-text truncation.
- Two-pane settings and secondary navigation.
- Onboarding scrolling and button layout.
- Overlay Start/End alignment.
