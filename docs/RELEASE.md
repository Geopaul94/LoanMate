# Release Guide — LoanMate

How to build the signed Play Store bundle (AAB), Windows edition.

---

## One-time: create the upload keystore

⚠️ **The keystore is irreplaceable.** If you lose it (or forget its password),
you can never publish an update to the same app on Play — you'd have to ship a
brand-new listing. Back it up in **at least two safe places** (password manager
+ encrypted cloud/USB) — see "Backing up" below.

> If you enroll in **Play App Signing** (recommended, and the default now),
> Google holds the real *app signing key*; the key below is your *upload key*.
> If you ever lose the upload key, Google can reset it — but still treat it as
> precious.

The keystore lives **outside the repo**, at `C:\Users\geopa\loanmate-upload.jks`
(same convention as every other app — never inside `D:\git clones\...`).
From anywhere:

```bash
keytool -genkeypair -v \
  -keystore C:/Users/geopa/loanmate-upload.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 36500 \
  -dname "CN=Geo Paulson, OU=Mobile Apps, O=Ghonestapps, L=Chalakudy, ST=Kerala, C=IN"
```

`keytool` will prompt for:
- **Keystore password** — `Geopaul@7557` (standard password for all projects).
- **Key password** — same, `Geopaul@7557`.

`-validity 36500` = 100 years, per the "app must remain valid for a century"
rule. `-dname` pre-fills the Distinguished Name so keytool doesn't prompt for
it interactively.

Then create `key.properties` (gitignored) from the template:

```
storeFile=C:/Users/geopa/loanmate-upload.jks
storePassword=Geopaul@7557
keyAlias=upload
keyPassword=Geopaul@7557
```

## Build the AAB

Always build with Android Studio's bundled JDK (JBR), not the system default:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew bundleRelease
```

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`
Upload this file to the Play Console.

## Build a signed APK (for sideload testing, optional)

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

## Backing up the keystore

After generating or modifying the keystore, create/update the backup folder
`D:\PlayStoreBackups\loanmate_drive_playstore_backup\` (never inside a git
repo). Standard contents — confirm every item exists before calling it done:

- `loanmate-upload.jks` — the keystore
- `key.properties` — copy (passwords, alias, storeFile path)
- `README-LOANMATE.md` — passwords, alias, applicationId, SHA1/SHA256
  fingerprints, versionCode/versionName, creation date
- `LoanMate-v<X.Y.Z>-release.aab` — the exact bundle uploaded to Play Console
- `store-assets/` — icon, feature graphic, screenshots
- `store-listing-text.txt` — app title, short description, full description
- `privacy-policy-text.txt` — the privacy policy text/URL used in Play Console
- `local.properties.backup` — copy of local.properties

Then tell Geo the full folder path so he can move it to Google Drive. Update
the folder on every new release (new `.aab`, refreshed README).

Get the fingerprints any time with:

```bash
keytool -list -v -keystore C:/Users/geopa/loanmate-upload.jks -alias upload -storepass Geopaul@7557
```

## Pre-release checklist

- [ ] `versionCode` bumped in `app/build.gradle.kts` (integer, +1 each release).
- [ ] `versionName` updated (e.g. "1.0.1").
- [ ] For the **release** signing cert's SHA-1, add an Android OAuth client in
      Google Cloud so Drive sync works in production (see `SETUP_DRIVE.md`).
- [ ] Privacy policy URL ready (Play requires one; the app collects loan data).
- [ ] Screenshots + feature graphic + store description.
- [ ] Test the release build on a device before uploading:
      `adb install app/build/outputs/apk/release/app-release.apk`

## Later: enable R8 shrinking (smaller AAB)

The first release keeps `isMinifyEnabled = false` so nothing in the
Hilt / Room / Compose / Google-Drive dependency graph is stripped. To shrink
later, flip it on and add keep rules in `proguard-rules.pro`, then **test the
release build end-to-end** (sign-in, Drive backup, all screens) before shipping.
