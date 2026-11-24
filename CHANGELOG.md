# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.11]

Maintenance release: documentation, repository and CI/CD changes only. The app itself is unchanged apart from its version number.

### Fixed
- README badges and links pointed to a placeholder owner (`OWNER`); they now point to this repository.
- README and CONTRIBUTING referred to `incoming/README.md`, which did not exist. It now explains how to publish a version and how to set up release signing.
- README: duplicates are described as they work since 1.9 (same app name, size and SHA-256, not package and version); the naming tokens and default template added in 1.9 and 1.10 are documented; the roadmap no longer lists search/sort/filter and the signed release workflow, which already exist.

### Added
- GitHub Release notes are taken from this changelog and include the APK's SHA-256 checksum.
- Repository hygiene check in CI: required files, no placeholder owner, no tracked keystore or `local.properties`, working relative links in Markdown files.
- `timeout-minutes` on every workflow job; CI runs for the same pull request cancel each other.
- `.gitattributes`, and a contact link to private vulnerability reporting in the issue chooser.

### Changed
- CI uploads artifacts with `actions/upload-artifact@v6` (Node.js 24 runtime).

## [1.10]

### Added
- Filter tags next to **All**: **Rename** (light red, files that need a new name) and **Named** (light green, already correct), with counts. The default view now shows only files that need renaming.
- Rows that already have the right name get a very light green background and a green status text (red is kept for real problems); works in light and dark themes.
- The file-name template is now built from **draggable tags**: long-press and drag to reorder, tap to edit text/tag, make a tag or group optional, move or remove it. A raw-text editor remains under "Advanced".
- Non-blocking **quick tour** (first launch; replay from Settings → Help). The highlight does not capture touches and the tip card can be dragged away or skipped.
- Each release now has a single APK named with its version, e.g. `ApkNamer-v1.10.apk`.

### Changed
- New default name template for fresh installs: `{name}< V{version}>< (Android {minandroid} to {targetandroid})>< {arch}a>`, e.g. `Telegram V10.14.5 (Android 8.0 to 14) v8a`. A stored template equal to the old default is upgraded automatically.

## [1.9]

### Fixed (important)
- Duplicate detection no longer relies on package + version code, which wrongly merged different builds of the same version (for example per-CPU builds). Files are now duplicates only when they match, in this order, on **app name, file size and a SHA-256 checksum**. The checksum is computed only for files that already share name and size, and cached.

### Added
- Duplicates review page: every group with all copies, full paths, sizes and dates. Select the copies to delete; selecting every copy of an app is refused (the last unselected copy is locked). Confirmation lists the full path of every file that will be deleted; everything is re-verified after deleting. "Auto-select" keeps one copy per group.
- New naming tokens: `{minsdk}`, `{minandroid}`, `{targetsdk}`, `{targetandroid}`, `{abi}` (e.g. arm64-v8a) and `{arch}` (v8, v7, v5, x86, x64, arm, universal). A ready-made preset and optional list-row info ("Android 8.0+ · v8").
- CPU architecture row in the details page; the known SHA-256 is shown without recomputing.

### Removed
- The one-tap "remove duplicates" dialog (replaced by the review page).

## [1.8]

### Changed
- Counts are now proper plurals (English one/other, Arabic zero/one/two/few/many/other), so messages read correctly in both languages ("ملف واحد", "ملفان", "3 ملفات", "11 ملفاً").
- Lint clean-up: `mipmap-anydpi-v26` merged into `mipmap-anydpi` (minSdk is 26), explicit handling of the backup-rules and in-app-language warnings, library-version notices left to Dependabot.
- CI can be started manually (Actions → CI → Run workflow).

## [1.7]

### Added
- One-tap duplicate cleanup: keeps one file per duplicate group (already correctly named, otherwise newest) and deletes the rest after a preview; everything is re-verified afterwards.
- Total size of the found packages and the space duplicates occupy.
- Optional "scan automatically on launch".
- File SHA-256 on demand in the details page (streamed, constant memory).

### Fixed
- Lint `ProduceStateDoesNotAssignValue`: the icon composable no longer uses `produceState`.
- Removed an unused string resource.

## [1.6]

### Fixed
- Lint error `ProduceStateDoesNotAssignValue` in the app icon composable that made the CI lint step fail (the app itself compiled and all unit tests passed).

## [1.5]

### Added
- Choose exactly which fields each list row shows (original name, version name/code, package, size, date, folder path, format/install/duplicate tags, status) with Compact / Standard / Detailed presets.
- "Open folder" (details page, duplicate rows, scan location).
- Delete a file from its details page. All duplicates (same package and version code) are listed with their paths and sizes; after every deletion all known files are re-checked and the list is refreshed.

### Changed
- Releases now contain a single APK asset named `ApkNamer.apk`; other assets are removed on re-runs. (GitHub always adds its own "Source code" archives, which cannot be disabled.)
- Workflows: Node 24-ready action versions, pinned `ubuntu-24.04`, a more robust and more verbose commit/push step.
- Dependabot: monthly, grouped, and no longer proposes AGP / Kotlin / coroutines / Gradle-wrapper upgrades that need manual migration.
- Removed the unused DataStore dependency.

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
