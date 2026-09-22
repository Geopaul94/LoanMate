# LoanMate

EMI & Loan Tracker — Android app. Personal/hobby project, targeting Play Store.

## Stack
- Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture.
- Room (persistence), Hilt (DI), WorkManager (EMI reminders / background sync), DataStore.
- Google Drive sync (play-services-auth + Drive v3, **Visible Folder** scope).

## Identity / release
- applicationId: `com.geo.loanmate` (Play). Code package (namespace):
  `com.loanmate` — intentionally different; only applicationId matters to Play.
- versionName 1.0.5 / versionCode 6. minSdk 28, targetSdk 36, compileSdk 36.
- Signing: upload keystore `loanmate-upload.jks`, kept **outside the repo**.
- **Debug Fingerprints (for Google Cloud Console)**:
  - SHA-1: `B6:1E:BB:21:93:C8:1E:7A:90:72:36:6A:15:3F:71:91:33:4C:14:EF`
  - SHA-256: `57:38:16:65:08:C7:71:7A:EB:E9:9C:DA:51:FD:BB:E5:89:0E:33:E0:FD:E5:09:58:60:F7:47:50:66:4A:9B:B0`

## Build / run (Windows — this is the active dev machine)
- **Android Studio**: open the project; its bundled JDK handles everything.
- **CLI** — always use Android Studio's bundled JDK 21:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  .\gradlew.bat assembleDebug
  ```
- Test device: Redmi `24094RAD4I` over Wi-Fi debugging (`adb connect`).

## Architecture rules
- Clean Architecture: `data` / `domain` / `presentation`.
- Screen file > 300 lines → extract into `components/`. Split again at 150.
- Sensitive data → `EncryptedSharedPreferences` / `security-crypto`.

## UI rules
- Design tokens only from `Theme.kt`. Never hardcode colors or DP.
- Spacing 4/8/12/16/24/32/48 dp.
- 4 states per screen: Loading / Empty / Error / Success.
- Bottom nav: 5 tabs (Home, Calendar, Analytics, Rewards, Settings). Emerald brand colors.

## Google Drive Backup
- **Folder**: Visible folder named `loanmate backupfile`.
- **Scopes**: `DRIVE_FILE` (for folder creation) and `DRIVE_APPDATA`.
- **Sync**: Automatic background sync on app launch via `DriveSyncWorker` (`WorkManager`).
- **Retention**: Last 5 backups kept.

## Current state (as of 2026-09-22)
- **v1.0.5 (versionCode 6)**: Active version built and packaged (AAB & APK).
- **Implemented in this session**:
  - **Biometric Lock**: App locks automatically on launch if enabled and prompts for biometric or device credentials.
  - **Privacy Mode**: All loan values, balances, EMIs, and calculator results are masked with "••••" across all screens when enabled.
  - **Production-grade ZIP Backups**: Updated local and Drive backup engine to package Room database, attachments, and manifests in a secure ZIP format with 5-layer overwrite protection and rollback capabilities.
  - **Release APK**: Signed release APK pushed directly to the connected device's `Download/` folder (`LoanMate-v1.0.5-release.apk`).
