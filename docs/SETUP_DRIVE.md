# Google Drive Sync — Setup Guide

LoanMate can back up your data to a visible folder named **"loanmate backupfile"** on your Google Drive. This allows you to see your backups and ensures they are safely stored in your own cloud.

To enable this, you need to:

1. Create a Google Cloud project (free).
2. Enable the Drive API.
3. Create OAuth 2.0 Client IDs for your Android app.
4. Add the web client ID to `local.properties`.

Estimated time: **10 minutes**, one-time.

---

## 1. Create a Google Cloud project

1. Go to <https://console.cloud.google.com/projectcreate>.
2. Project name: `LoanMate`.
3. Click **Create**.
4. Make sure the new project is selected in the top dropdown.

## 2. Enable the Drive API

1. Go to <https://console.cloud.google.com/apis/library/drive.googleapis.com>.
2. Click **Enable**.

## 3. Configure the OAuth consent screen

1. Go to <https://console.cloud.google.com/apis/credentials/consent>.
2. User type: **External**. Click **Create**.
3. Fill in the required fields (App name: LoanMate, Support email, Developer email).
4. Click **Save and Continue**.
5. On the **Scopes** step, click **Add or Remove Scopes**, search for and add:
   - `https://www.googleapis.com/auth/drive.file` (View and manage Google Drive files and folders that you have opened or created with this app)
   - `https://www.googleapis.com/auth/drive.appdata` (View and manage its own configuration data in your Google Drive)
6. Click **Update**, then **Save and Continue**.
7. On the **Test users** step, click **Add Users** and add your own Google account address. **Save**.

## 4. Get your debug SHA-1 fingerprint

From the project root, run:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Copy the **SHA-1** value.

## 5. Create OAuth 2.0 Client IDs

Go to <https://console.cloud.google.com/apis/credentials>.

### a) Android client (registers your app with Google)

1. Click **Create Credentials → OAuth client ID**.
2. Application type: **Android**.
3. Name: `LoanMate Android (debug)`.
4. Package name: `com.geo.loanmate`
5. SHA-1: paste the SHA-1 you copied above.
6. Click **Create**.

### b) Web client (used for the in-app Sign-In)

1. Click **Create Credentials → OAuth client ID**.
2. Application type: **Web application**.
3. Name: `LoanMate Web`.
4. Click **Create**. Copy the **Client ID**.

## 6. Plug the web client ID into your project

Open `local.properties` at the project root. Add:

```properties
GOOGLE_OAUTH_WEB_CLIENT_ID=your_copied_client_id_here.apps.googleusercontent.com
```

Then rebuild/sync the app.

---

## What gets uploaded

- A JSON file containing all your loans, payments, and achievements.
- Stored in a visible folder named **"loanmate backupfile"** on your Drive.
- The app automatically syncs this file on every cold launch if internet is available.
- The app keeps the last **5 backups** and prunes older ones automatically to save space.

## Privacy

LoanMate uses the `drive.file` scope, which means it **only** has access to the files and folders it creates itself. It cannot read your photos, documents, or other personal files on Google Drive.
