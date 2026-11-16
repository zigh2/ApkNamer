# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
