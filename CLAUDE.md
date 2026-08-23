# LoanMate

EMI & Loan Tracker — Android app. Personal/hobby project, targeting Play Store.

## Stack
- Kotlin, Jetpack Compose (Material 3), MVVM + Clean Architecture.
- Room (persistence), Hilt (DI), WorkManager (EMI reminders), DataStore.
- Google Drive sync (play-services-auth + Drive v3, appDataFolder scope).

## Identity / release
- applicationId: `com.geo.loanmate` (Play). Code package (namespace):
  `com.loanmate` — intentionally different; only applicationId matters to Play.
- versionName 1.0.1 / versionCode 2. minSdk 26, targetSdk 35.
- Signing: upload keystore `loanmate-upload.jks`, kept **outside the repo** at
  `C:\Users\geopa\loanmate-upload.jks`, read via gitignored `key.properties`
  (root of this repo). Alias `upload`, passwords/DN/100-year validity per the
  global keystore standard. **Generated 2026-08-13** — SHA1
  `36:24:FA:C5:6E:D6:86:67:86:9F:8C:66:29:C4:B7:43:5D:1E:17:36`.
- Backup folder: `D:\PlayStoreBackups\loanmate_drive_playstore_backup\` — fully
  populated (keystore, key.properties, `LoanMate-v1.0.1-release.aab`, README,
  store-listing-text.txt, privacy-policy-text.txt, store-assets/ with icon,
  feature graphic, and 5 screenshots). README-LOANMATE.md there has the full
  checklist status and fingerprints.
- Full release steps: `docs/RELEASE.md`. Drive setup: `docs/SETUP_DRIVE.md`.

## Build / run (Windows — this is the active dev machine)
- **Android Studio**: open the project; its bundled JDK handles everything.
- **CLI** — always use Android Studio's bundled JDK 21, not the system default:
  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  ./gradlew assembleDebug        # debug APK
  ./gradlew testDebugUnitTest    # unit tests
  ./gradlew bundleRelease        # signed .aab for Play (needs key.properties)
  ./gradlew installDebug         # build + push to connected device
  ```
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  .\gradlew.bat assembleDebug
  ```
- `ANDROID_HOME` → `%LOCALAPPDATA%\Android\Sdk`.
- Test device: Redmi (`beryl`, MIUI/HyperOS) over Wi-Fi debugging (`adb connect`).
  Reinstalling a debug build over a differently-signed existing install fails
  with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — uninstall the old package first.

## Build gotchas
- Google API client libs need the META-INF packaging excludes (build.gradle.kts)
  and NetHttpTransport (not the removed AndroidHttp).
- WorkManager uses on-demand init via Hilt (LoanMateApp : Configuration.Provider);
  the default WorkManagerInitializer is removed in the manifest (lintVital fails
  otherwise).
- First release ships with `isMinifyEnabled = false` (no R8) so nothing in the
  Hilt/Room/Compose/Drive graph is stripped.
- **`adb input`/`keyevent` is BLOCKED on the test Redmi** (`beryl`), same as on
  other projects tested on this phone: `SecurityException: Injecting input
  events requires ... INJECT_EVENTS`. No automated tapping/scrolling — either
  drive the UI by hand, or seed data directly (see "Seeding sample data" below).
  Screen capture itself (`adb exec-out screencap`) works fine regardless.
- The Pixel_5/Pixel_7 emulator route (previously the documented screenshot
  workaround) had GPU rendering issues on this machine and was abandoned in
  favor of seeding real data on the physical device instead.

## Seeding sample data on-device (debug builds only)
Since UI automation is blocked, populate realistic data directly in Room:
1. `adb shell run-as com.geo.loanmate cat databases/loanmate.db > local.db`
   (binary-safe via `exec-out`, not a `>` redirect through `adb shell`).
2. Also pull `loanmate.db-wal`/`-shm` if present; open the `.db` with
   `platform-tools/sqlite3.exe` and run `PRAGMA journal_mode=DELETE;` first to
   merge the WAL into the main file (rows written recently live only in the
   `-wal` file otherwise).
3. `INSERT` rows into `loans` / `payment_history` / `achievements` — schema
   mirrors the Kotlin entities exactly (enums stored as `.name` strings via
   `Converters.kt`, e.g. `loanType='HOME'`, `status='ACTIVE'`).
4. Push back: `adb shell "cat /sdcard/seed.db | run-as com.geo.loanmate sh -c
   'cat > databases/loanmate.db'"` (pipe through the shell user — `run-as ... <
   file` fails on SELinux), then `rm -f databases/loanmate.db-wal
   databases/loanmate.db-shm` so the app doesn't reopen stale WAL state.
   On Windows/git-bash, prefix on-device paths with `//` (e.g. `//sdcard/x`) or
   MSYS mangles the leading `/` into a Windows path.
5. `am force-stop` then relaunch — achievements table isn't auto-seeded until
   first touched by the app, so insert all 7 `AchievementType` rows yourself
   (definitions in `AchievementRepository.kt`) if the Rewards screen needs data.

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
- Run: `./gradlew testDebugUnitTest` (see Build/run above for `JAVA_HOME`).

## Git
- Remote: git@github-geopaul94:Geopaul94/LoanMate.git (personal identity
  geopaul94). NEVER commit key.properties, *.jks, or local.properties.

## Store assets (`/store` in repo, mirrored to backup store-assets/)
- `icon-512.png` — 512×512 Play listing icon (emerald + gold growth mark).
- `feature-graphic-1024x500.png` — feature graphic (logo + wordmark + tagline).
- Rendered from SVG via rsvg-convert; brand fonts installed to ~/Library/Fonts
  (from the earlier Mac session).
- `screenshots/` (5, done 2026-08-13) — captured on the physical Redmi with
  data seeded directly via SQLite (see "Seeding sample data" above), not the
  originally-planned emulator route: `01-dashboard.png`, `02-loan-list.png`,
  `03-loan-detail.png`, `04-analytics.png`, `05-rewards.png`.

## Privacy policy
- `docs/privacy.html` — committed on `main`, styled to match the emerald/gold
  brand. Covers: local-only storage by default, optional Drive `appdata`-scope
  backup (developer never sees it), biometric app-lock, no ads/analytics, and
  (added 2026-08-13, commit `6e27525`) an explicit "LoanMate is not a
  financial service" section — added to reinforce the Financial Services
  policy appeal below. Loan-type labels (KSFE chitty etc.) are called out as
  just category names, not facilitated products.
- **Live at `https://geopaul94.github.io/LoanMate/privacy.html`** (GitHub
  Pages enabled 2026-08-12, branch `main`, folder `/docs`). This is the URL
  used in Play Console → App content → Privacy policy / Data Safety. Pages
  rebuilds can lag ~1-2 min after a push — don't panic if `curl` shows stale
  content right after pushing.

## Current state (as of 2026-08-13)
- Fully release-candidate: keystore generated, signed AAB built + verified,
  release build smoke-tested on device (clean launch, no crashes — R8 is off
  so this mainly confirms signing/Hilt/Room wiring, not obfuscation issues),
  privacy policy live, store listing text + 5 screenshots done. Everything in
  the backup folder checklist is checked off except `local.properties.backup`.
- **Fixed this session**: "Add Loan" FAB was overlapping the Monthly EMI card
  on the dashboard for any user with 3+ active loans (Scaffold FAB pinned
  bottom-right collided with the summary card grid at initial scroll
  position). Switched to a compact icon-only FAB in `DashboardScreen.kt`.
- **Play Console submission REJECTED (2026-08-13)** — "Violation of Financial
  Services policy": Google's automated review flagged the Financial features
  declaration ("My app doesn't provide any financial features") as
  inaccurate, most likely triggered by the "KSFE Chitty" loan-type label in
  the app (India-region digital-lending scanning is known to be aggressive
  and keyword-driven). LoanMate is a pure manual tracker — no lending,
  payment processing, or institution connections of any kind — so the
  declaration itself is believed accurate. **Next step**: re-submit with the
  same "no financial features" declaration plus a written appeal explaining
  the KSFE-chitty labeling is just a category name, not a facilitated chit
  fund. Appeal text drafted in this session's conversation — reuse it if
  starting a fresh thread, and check Play Console for the outcome before
  trying anything else (e.g. don't be tempted to check "Crowdfunding and chit
  funds" to satisfy the scanner — that pulls in chit-fund licensing
  requirements LoanMate can't and shouldn't satisfy).
- Still open: Drive sign-in has not been tested on the release build (GCP
  OAuth Android client for this release SHA1 not registered yet — see
  `docs/SETUP_DRIVE.md`).
- **Uncommitted as of end of this session**: `.gitignore`, `CHANGELOG.md`,
  `app/build.gradle.kts` (version bump), `DashboardScreen.kt` (FAB fix),
  `docs/RELEASE.md`, `key.properties.template`, and the new `store/screenshots/`
  folder are all still sitting as local changes — never got an explicit
  commit/push go-ahead this session (only `docs/privacy.html` was committed,
  twice, both times on its own). Check `git status` at the start of the next
  session and ask before committing, per the git discipline rule.
