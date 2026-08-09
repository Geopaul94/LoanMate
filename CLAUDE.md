# LoanMate

EMI & Loan Tracker — Android app. Personal/hobby project, targeting Play Store.

## Stack
- Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture.
- Room (persistence), Hilt (DI), WorkManager (EMI reminders), DataStore.
- Google Drive sync (play-services-auth + Drive v3, appDataFolder scope).

## Identity / release
- applicationId: `com.geo.loanmate` (Play). Code package (namespace):
  `com.loanmate` — intentionally different; only applicationId matters to Play.
- versionName 1.0.0 / versionCode 1. minSdk 26, targetSdk 35.
- Signing: upload keystore `loanmate-upload.jks` (repo root), read via gitignored
  `keystore.properties`. Passwords/alias per the global keystore standard.
- Build AAB: `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew bundleRelease`
  → `app/build/outputs/bundle/release/app-release.aab`.
- Backup folder: `~/Documents/PlayStoreBackups/loanmate_drive_playstore_backup/`
  (keystore, key.properties, AAB, README, store text, privacy policy).
- Full release steps: `docs/RELEASE.md`. Drive setup: `docs/SETUP_DRIVE.md`.

## Build gotchas
- Gradle daemon must run on JDK 17 (host default JDK 25 breaks Kotlin compiler):
  prefix builds with `JAVA_HOME=$(/usr/libexec/java_home -v 17)`.
- Google API client libs need the META-INF packaging excludes (build.gradle.kts)
  and NetHttpTransport (not the removed AndroidHttp).
- WorkManager uses on-demand init via Hilt (LoanMateApp : Configuration.Provider);
  the default WorkManagerInitializer is removed in the manifest (lintVital fails
  otherwise).
- First release ships with `isMinifyEnabled = false` (no R8) so nothing in the
  Hilt/Room/Compose/Drive graph is stripped.

## Design system
- Brand: emerald primary + gold accent, warm-tinted neutrals; NO dynamic color.
  Colors in ui/theme/Color.kt; type (Plus Jakarta Sans + Inter, bundled variable
  fonts) in ui/theme/Type.kt.
- Shared primitives: ui/components/DesignPrimitives.kt (Dimens, IconChip,
  SectionLabel) + EmptyState.kt. Cards 20dp; semantic tokens SuccessGreen /
  WarningAmber / DangerRed kept distinct from brand.
- Custom adaptive icon + branded SplashScreen (ic_splash_logo).

## Pure logic (unit-tested — 57 tests in app/src/test)
- utils/: EmiCalculator, PrepaymentCalculator, ForeclosureCalculator,
  PayoffStrategyCalculator, StreakCalculator, MissedPaymentDetector,
  EmiOccurrenceGenerator, PdfExporter, BackupManager.
- Run: `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest`.

## Git
- Remote: git@github-geopaul94:Geopaul94/LoanMate.git (personal identity
  geopaul94). NEVER commit keystore.properties, *.jks, or local.properties.

## Store assets (`/store` in repo, mirrored to backup store-assets/)
- `icon-512.png` — 512×512 Play listing icon (emerald + gold growth mark).
- `feature-graphic-1024x500.png` — feature graphic (logo + wordmark + tagline).
- Rendered from SVG via rsvg-convert; brand fonts installed to ~/Library/Fonts.
- Still TODO: phone screenshots (≥2). Physical MIUI device blocks adb input,
  so capture via the Pixel_7 emulator (`emulator -avd Pixel_7`), install the
  debug apk, seed data, navigate + `adb -e exec-out screencap`.

## Current state
- v1.0.0 AAB built + signed + smoke-tested on device (com.geo.loanmate); ready
  for Play Console. Icon + feature graphic done.
- Before upload: capture screenshots, host the privacy policy for a URL, and
  (for Drive in release) create a GCP OAuth client for com.geo.loanmate + the
  release signing SHA1.
