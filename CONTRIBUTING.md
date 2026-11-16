# Contributing

Thanks for helping improve APK Namer!

## Getting started
1. Install Android Studio (Ladybug or newer) and JDK 17.
2. Fork and clone the repository, then open it in Android Studio.
3. Run the unit tests: `./gradlew testDebugUnitTest` (or `gradle testDebugUnitTest`).

## Guidelines
- **Keep logic testable.** Anything that does not need Android classes belongs in `domain/` with a unit test.
- **No network access.** The app deliberately declares no `INTERNET` permission; PRs adding one will not be accepted.
- **Strings** go in `res/values/strings.xml`; please add an Arabic translation in `values-ar` or mention that one is missing.
- **Commits**: short imperative subject (`Add APKM fallback to base APK`), details in the body.
- Run `./gradlew lintDebug testDebugUnitTest` before opening a PR.

## Adding support for a new package format
1. Add the type/extension aliases in `domain/Models.kt`.
2. Teach `data/parse/ArchiveInspector.kt` how to recognise it and where its metadata lives.
3. Add a parser test in `app/src/test/.../data/` using a small JSON/manifest sample.

## Reporting bugs
Use the issue templates. For unreadable packages, please say which app/store the file came from and the output of `unzip -l yourfile` (no need to attach the file itself).

## Regex note
Android uses the ICU regex engine, which is stricter than the JVM used by unit tests: always escape `{`, `}`, `[`, `]`
(e.g. `\{`, `\}`, `\]`) even where java.util.regex tolerates them.
