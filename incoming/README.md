# incoming/

Drop a project archive here to publish a new version of APK Namer. A GitHub Actions workflow
([`release.yml`](../.github/workflows/release.yml)) does the rest.

## Publish a new version

1. Zip the whole project and name the file `ApkNamer-vX.Y.zip` (any name containing `vX.Y` or `vX.Y.Z` works).
   The archive must have `settings.gradle.kts` at its root, either directly or inside a single top-level folder.
2. Upload the zip to this folder on the `main` branch (*Add file → Upload files*).
3. The workflow then:
   - replaces the project files with the zip contents (it keeps `.git`, this folder and `.github/workflows/`);
   - sets `versionName` to `X.Y` and `versionCode` to `X*10000 + Y*100 + Z` (for example `1.11` → `11100`);
   - runs the unit tests and builds the APK;
   - commits the new project state, deleting the zip in the same commit;
   - publishes a GitHub Release `vX.Y` with one asset, `ApkNamer-vX.Y.apk`. The release notes are the section of
     [`CHANGELOG.md`](../CHANGELOG.md) with the same version, followed by the APK's SHA-256.

If the tests or the build fail, **nothing is committed** and the zip stays here. Fix the problem and upload a new zip, or run
*Actions → Release from incoming zip → Run workflow* and type the zip's file name.

Workflow files are never replaced by a drop. Changes to `.github/workflows/` must be uploaded by hand.

## Release signing (one-time setup)

Without the secrets below the APK is **debug-signed**: every build gets a different key, so it cannot be installed over a
previous version and the old one has to be uninstalled first. With them, every release is signed with the same key and
updates install normally.

1. Create a keystore on your computer (keep it **out of the repository**; `.gitignore` already excludes `*.jks` and `*.keystore`):

   ```bash
   keytool -genkeypair -v -keystore release.jks -alias apknamer \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Encode it as one line of text:

   ```bash
   base64 -w0 release.jks
   ```

3. In the repository open *Settings → Secrets and variables → Actions* and add four repository secrets:

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | the output of the `base64` command |
   | `KEYSTORE_PASSWORD` | the keystore password |
   | `KEY_ALIAS` | `apknamer` (the alias used above) |
   | `KEY_PASSWORD` | the key password |

4. Back up `release.jks` and its passwords somewhere safe. If they are lost, users must uninstall the app once to install a
   build signed with a new key.

Signed builds use the `release` build type (minified with R8); unsigned ones use `debug`.
