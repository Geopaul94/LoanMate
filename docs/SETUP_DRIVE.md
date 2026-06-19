# Google Drive Sync — Setup Guide

LoanMate can back up your data to your personal Google Drive's **app-specific folder** (invisible to the user, isolated to this app). To enable this, you need to:

1. Create a Google Cloud project (free).
2. Enable the Drive API.
3. Create OAuth 2.0 Client IDs for your Android app.
4. Add the web client ID to `local.properties`.

Estimated time: **10 minutes**, one-time.

---

## 1. Create a Google Cloud project

1. Go to <https://console.cloud.google.com/projectcreate>.
2. Project name: `LoanMate` (or anything you like).
3. Click **Create**. Wait ~30 seconds for the project to be created.
4. Make sure the new project is selected in the top dropdown.

## 2. Enable the Drive API

1. Go to <https://console.cloud.google.com/apis/library/drive.googleapis.com>.
2. Make sure your LoanMate project is selected.
3. Click **Enable**.

## 3. Configure the OAuth consent screen

1. Go to <https://console.cloud.google.com/apis/credentials/consent>.
2. User type: **External**. Click **Create**.
3. Fill in:
   - **App name**: LoanMate
   - **User support email**: your email
   - **Developer contact email**: your email
4. Click **Save and Continue**.
5. On the **Scopes** step, click **Add or Remove Scopes**, search for `drive.appdata`, check it, click **Update**, then **Save and Continue**.
6. On the **Test users** step, click **Add Users** and add your own Google account. Save.

You can leave the app in "Testing" mode forever — only the test users (you) can sign in. That's fine for personal use.

## 4. Get your debug SHA-1 fingerprint

LoanMate signs debug builds with the default Android Studio debug key. From the project root, run:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
```

Copy the SHA1 value (looks like `AA:BB:CC:...:99`).

## 5. Create OAuth 2.0 Client IDs

Go to <https://console.cloud.google.com/apis/credentials>.

### a) Android client (so Google trusts our app)

1. Click **Create Credentials → OAuth client ID**.
2. Application type: **Android**.
3. Name: `LoanMate Android (debug)`.
4. Package name: `com.loanmate`
5. SHA-1: paste the SHA-1 you copied above.
6. Click **Create**. The Android client ID itself is not used in code — it just registers your app.

### b) Web client (used as the server-side client for Sign-In)

1. Click **Create Credentials → OAuth client ID**.
2. Application type: **Web application**.
3. Name: `LoanMate Web`.
4. Leave the redirect URI fields blank.
5. Click **Create**. Copy the **Client ID** (looks like `1234567890-abc...apps.googleusercontent.com`).

## 6. Plug the web client ID into your project

Open `local.properties` at the project root (already gitignored). Add:

```properties
GOOGLE_OAUTH_WEB_CLIENT_ID=1234567890-abc...apps.googleusercontent.com
```

Then rebuild the app. The Settings → Backup screen will now offer Google Drive sync.

## 7. (Optional) For release builds

When you create a release-signed APK, the signing SHA-1 will be different. You'll need to add it as an additional credential under your Android client in GCP.

---

## What gets uploaded

- The same JSON we use for local backup — schema-versioned, contains all loans, payments, and achievements.
- Stored in your Drive's **appDataFolder** scope. This is invisible to the Drive web UI; only this app can see/modify it.
- One file per backup, named `loanmate_backup_YYYY-MM-DD_HHmm.json`.
- The app keeps the last 5 backups and prunes older ones automatically.

## What does NOT get uploaded

- No personal info beyond what you entered (loan name, bank, amounts, dates).
- No tokens or credentials.
- No analytics, no telemetry. Drive sync is entirely user-initiated.

## Privacy

The Drive scope used is `drive.appdata` — the most restricted Drive scope. We literally **cannot** see your other Drive files even if we wanted to. Read more: <https://developers.google.com/drive/api/guides/appdata>.
