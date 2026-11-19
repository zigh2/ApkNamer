# APK Namer

> Batch-rename APK, XAPK, APKM and APKS files based on what is **inside** them — app name, package, version.

[![CI](https://github.com/OWNER/ApkNamer/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/ApkNamer/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
![Min SDK 26](https://img.shields.io/badge/minSdk-26-blue)

Files like `base.apk`, `download (3).xapk` or `com.foo.bar_1234.apkm` are impossible to tell apart.
APK Namer inspects each file, reads the real metadata and renames it to something readable such as
`Telegram v10.14.5 (5034).apk`. It is a modern take on the classic *APK Renamer*, which stopped working
once apps became split bundles.

🇸🇦 [نسخة عربية مختصرة](README.ar.md)

## Features

- **All the formats** – plain APK, XAPK, APKM, APKS (plus APKX / ASPK aliases).
- **Content-based detection** – *Deep scan* finds packages even when renamed to `.zip`, `.bin` or no extension.
- **Real metadata** – app label, package name, version name and version code.
- **Flexible templates** – `{name}< v{version}>< ({code})>` with live preview and presets.
- **Safe** – never overwrites, resolves duplicates (`… (2).apk`), previews every new name, and **undo** for the last batches.
- **Private** – no `INTERNET` permission, no analytics, nothing leaves the device.
- **Rich list** – app icons, size, date, folder path; search, filter (type / updates / duplicates) and sort.
- **Details page** – package, versions, min/target Android, permissions, signature SHA-256, bundle contents, install state vs. your device.
- **Duplicates & cleanup** – see every duplicate of an app (same package and version) with paths and sizes, delete from the details page, open the folder in your file manager.
- **Safe duplicate cleanup** – duplicates are byte-identical files (same app name, size and SHA-256). A review page lets you pick the copies to delete; at least one copy of each app is always kept, and every path is listed before deleting.
- **Android & CPU info** – minimum/target Android and CPU architecture (v8/v7/x86…) in the list, details page and file-name templates.
- **Customisable** – light/dark/system theme, dynamic colors, pure black, in-app language (English / Arabic with full RTL).

## Supported formats and where metadata comes from

| Format | Detected by | Metadata source |
|---|---|---|
| APK | `AndroidManifest.xml` at archive root | Android `PackageManager` parser |
| XAPK | `manifest.json` containing `package_name` | JSON; falls back to base APK for missing fields |
| APKM | `info.json` containing `pkg_name` | JSON; falls back to base APK for missing fields |
| APKS / SAI / bundletool | nested `.apk` files (base + splits) | Base APK (`base.apk`, `base-master.apk`, `standalone.apk`, `<package>.apk`, or the largest non-config split) |

## Name templates

| Token | Meaning | Example |
|---|---|---|
| `{name}` | App label (falls back to package name) | `Telegram` |
| `{package}` | Package name | `org.telegram.messenger` |
| `{version}` | Version name | `10.14.5` |
| `{code}` | Version code | `5034` |
| `{type}` | Detected format | `XAPK` |
| `{original}` | Original file name without extension | `download (3)` |

Wrap text in `<...>` to make it **optional**: the group disappears if any token inside it is empty.
`{name}< v{version}>< ({code})>` becomes `Telegram v10.14.5 (5034)`, or just `Telegram` when no version is known.

## Permissions

APK Namer asks for **All files access** (`MANAGE_EXTERNAL_STORAGE`) on Android 11+ (and legacy storage
permissions on Android 8–10). That is the only way to scan arbitrary folders and rename files *in place*.
Because of this policy the app is intended for distribution via GitHub Releases / F-Droid / IzzyOnDroid rather than Google Play.

## Download

Latest release: **[Releases → latest](https://github.com/OWNER/ApkNamer/releases/latest)** — each release has one file named `ApkNamer-vX.Y.apk`. All versions are on the [Releases](https://github.com/OWNER/ApkNamer/releases) page.

## Publishing a new version (maintainers)

Upload `ApkNamer-vX.Y.zip` to [`incoming/`](incoming/README.md). A workflow replaces the project files,
runs the tests, builds the APK and publishes a GitHub Release automatically. Details and one-time signing setup: [incoming/README.md](incoming/README.md).

## Build

Requirements: Android Studio Ladybug+ (or Gradle 8.9), JDK 17.

```bash
git clone https://github.com/OWNER/ApkNamer.git
cd ApkNamer
gradle wrapper --gradle-version 8.9   # once, to generate ./gradlew
./gradlew assembleDebug               # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest           # unit tests
```

## Architecture

```
app/src/main/java/io/github/apknamer/
├── domain/        Pure Kotlin: models, NameTemplate, FileNameSanitizer, RenamePlanner   (unit-tested)
├── data/
│   ├── parse/     ArchiveInspector, ApkMetadataReader, BundleManifest (XAPK/APKM JSON)
│   ├── scan/      FileScanner – directory walk + bounded-parallel inspection
│   ├── rename/    Renamer + RenameHistory (persistent undo)
│   ├── settings/  DataStore-backed settings
│   └── storage/   Permission + folder-picker helpers
└── ui/            MainViewModel (StateFlow), Compose screens, theme
```

MVVM with unidirectional data flow: the ViewModel owns an immutable `UiState`; the UI only renders it and
sends events back. The rename plan is a pure function of *(detected files, options)*, so previews and tests are cheap.

## Troubleshooting

If the app closes unexpectedly, reopen it: a **Previous session report** dialog shows what happened.
Copy it into a bug report. Note that Android itself restarts an app right after you grant *All files access*.

## Known limitations

- App labels are resolved in the **device language**; apps without a translation fall back to their default label.
- Split-only config APKs (no launcher label) are named by package name.
- Bundles whose JSON descriptor and base APK are both unreadable are listed as "couldn't read app info" and left untouched.
- Folder picker results are converted to real paths, which works for internal storage and SD cards but not cloud providers.

## Roadmap

- [ ] Storage Access Framework mode (no all-files permission) for Play-compatible builds
- [ ] Dry-run export (CSV) and rename rules per folder
- [ ] Sort/filter results, search box
- [ ] Share-sheet entry: rename a single file from a file manager
- [ ] Fastlane metadata for F-Droid, signed release workflow
- [ ] Screenshots in this README

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Please also read the [Code of Conduct](CODE_OF_CONDUCT.md) and [Security policy](SECURITY.md).

## License

[MIT](LICENSE)
