# Release Guide — LoanMate

How to build the signed Play Store bundle (AAB).

---

## One-time: create the upload keystore

⚠️ **The keystore is irreplaceable.** If you lose it (or forget its password),
you can never publish an update to the same app on Play — you'd have to ship a
brand-new listing. Back it up in **at least two safe places** (password manager
+ encrypted cloud/USB).

> If you enroll in **Play App Signing** (recommended, and the default now),
> Google holds the real *app signing key*; the key below is your *upload key*.
> If you ever lose the upload key, Google can reset it — but still treat it as
> precious.

Create it from the project root:

```bash
keytool -genkeypair -v \
  -keystore loanmate-release.jks \
  -alias loanmate \
  -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` will prompt for:
- **Keystore password** — choose a strong one, save it.
- **Key password** — can be the same as the keystore password.
- **Name / Org / City / State / Country** — your details (e.g. CN = your name,
  C = IN). These sit in the certificate; not user-visible.

Then create `keystore.properties` (gitignored) from the template:

```
storeFile=loanmate-release.jks
storePassword=<your keystore password>
keyAlias=loanmate
keyPassword=<your key password>
```

## Build the AAB

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home \
  ./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`
Upload this file to the Play Console.

## Build a signed APK (for sideload testing, optional)

```bash
JAVA_HOME=... ./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

## Backing up the keystore

Keep copies of **all** of these together, off the repo:
- `loanmate-release.jks`
- `keystore.properties` (has the passwords)
- The SHA-1 / SHA-256 fingerprints (also needed for Google Drive OAuth &
  any other signed-cert integrations):

```bash
keytool -list -v -keystore loanmate-release.jks -alias loanmate
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
