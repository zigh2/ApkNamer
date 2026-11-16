# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.4]

### Added
- App icons in the list (loaded lazily, cached; bundles use their embedded `icon.png` when present).
- Details screen: package, versions, min/target Android, permissions, signature SHA-256, size, dates, full path, bundle contents, per-file rename and quick actions.
- Install state versus the device (installed / update / older) and duplicate detection.
- Search, filter (type, updates, duplicates) and sort (name, app, size, date, version).
- Settings as a separate page: theme (system/light/dark, dynamic colors, pure black), language (system/English/Arabic), scan, naming and list options.

### Changed
- Settings are stored in SharedPreferences (read synchronously) so theme and language apply on the first frame. Previous settings are reset once.
- New `QUERY_ALL_PACKAGES` permission, used only to compare files with installed apps.

## [1.3]

### Changed
- The release workflow now deletes the processed zip from `incoming/` after a successful release, and ignores pushes that contain no new zip (e.g. the deletion itself).

## [1.2]

### Fixed
- Crash when opening Options or finishing a scan: a name-template regex had an unescaped `}`, which Android's ICU regex engine rejects (the JVM used by unit tests accepts it).

## [1.1]

### Added
- Built-in diagnostics: after an unexpected close, the next launch shows a copyable report (Java stack trace or the system's process-exit reason).

### Changed
- Scan: lower parallelism, per-file failure isolation, de-duplication and throttled progress updates.

## [1.0]

### Added
- `incoming/` workflow: drop `ApkNamer-vX.Y.zip` into `incoming/` and CI replaces the project, builds, and publishes a GitHub Release with a direct-download APK.
- Optional release signing through repository secrets.

### Initial feature set

### Added
- Recursive scan for APK, XAPK, APKM and APKS/APKX/ASPK packages.
- Deep scan: detect packages by content, whatever their extension.
- Metadata extraction (app name, package, version name, version code) for every supported format.
- Customisable name templates with optional groups and live preview.
- Conflict-free rename plan, extension correction and persistent undo.
- English and Arabic (RTL) translations.
