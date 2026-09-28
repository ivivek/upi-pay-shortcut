# CLAUDE.md

Android app (Kotlin, Jetpack Compose) that saves merchant UPI QR codes and pins them as
home-screen shortcuts that open the UPI app on the payment screen. See README.md for the product.

## Commands

```sh
./build.sh build      # assembleDebug
./build.sh install    # installDebug + launch on the connected device
./build.sh test       # testDebugUnitTest
./gradlew testDebugUnitTest assembleDebug   # usual check before committing
```

- Env: `JAVA_HOME=/opt/jbr`, `ANDROID_HOME=/opt/android-sdk` (`local.properties` holds `sdk.dir`).
- Launch timing on device: `adb logcat -s UpiShortcut` (logged by `PayActivity`).
- Verify permissions after dependency changes:
  `aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk`.

## Toolchain (pinned on purpose)

AGP 8.10.1, Gradle 8.11.1, Kotlin 2.2.20, KSP 2.2.20-2.0.4, Compose BOM 2025.10.01, Room 2.8.3,
Glance 1.1.1, compileSdk/targetSdk 36, minSdk 26. The newest AndroidX releases need AGP 9;
don't bump versions without also migrating the build.

## Layout (`app/src/main/java/com/linetra/upishortcut/`)

| File | Role |
|---|---|
| `PayActivity.kt` | Invisible trampoline for shortcuts and the widget: merchant id → DB → `ACTION_VIEW` upi link → finish. Keep it lean; it's on the tap path. |
| `Shortcuts.kt` | Pin, update, disable and dynamic (long-press) shortcuts; letter icons; `MerchantColors`; `PinnedReceiver`. |
| `UpiLink.kt` | Pure-Kotlin parse, validate, find and edit of `upi://pay` links. Unit-tested in `src/test/.../UpiLinkTest.kt`. |
| `MainViewModel.kt` | Screen state (`Screen.List` / `Screen.Form`), save, delete, scan, share, import/export, risk acceptance. |
| `MainActivity.kt` | Compose host, activity-result launchers, share intents, dialogs. |
| `ui/` | `MerchantListScreen`, `MerchantFormScreen`, `RiskDialog`, `MerchantAvatar`, `Theme`. |
| `data/` | Room: `Merchant`, `MerchantDao`, `AppDatabase` (`upi_shortcut.db`, version 1). |
| `QrScanner.kt` | Google Code Scanner (camera) and ML Kit (images); both run in Play services. |
| `UpiApps.kt` | Installed UPI apps via `<queries>`. |
| `Backup.kt` | JSON export/import. |
| `widget/MerchantWidget.kt` | Glance home-screen widget. |

## Design decisions (keep unless the user says otherwise)

- **Shortcuts carry only the merchant id** (`m-<id>`), never the link. PayActivity reads the DB
  (measured at ~12–35 ms, not noticeable). The user chose this over embedding the link so edits
  reach pinned icons. Room ids use AUTOINCREMENT, so shortcut ids are never reused.
- **Store the link exactly as scanned**; only `UpiLink.normalize` (encode spaces, drop line breaks).
  Don't rewrite `pn`, `mode` etc.; merchant fields like `mc`/`orgid`/`sign` must pass through.
  QR payloads can contain raw spaces (`pn=Example Services`): `UpiLink.find` takes a whole-text
  link as-is.
- **No INTERNET or ACCESS_NETWORK_STATE**: both are stripped in the manifest with
  `tools:node="remove"`. Don't add networking.
- **Pinned state is read from the launcher** (`Shortcuts.pinnedIds`), not stored.
- **Apps can't remove pinned icons**: deleting disables the shortcut and shows a dialog telling
  the user to remove it. Don't change the disabled-shortcut message.
- **Changing the merchant list** must go through `merchantsChanged()` in the ViewModel so dynamic
  shortcuts and the widget refresh.
- Adding DB columns needs a Room migration (bump the version in `AppDatabase`).
- Planned work is in `docs/TODO.md`.

## Rules

- **The org name is `linetra` only.** The package was renamed from a previous org's name and the
  git history rewritten so that name never appears. Never introduce any other org name into code,
  paths or commits.
- Git identity is repo-local: the owner's name and email (see `git log`). The global config's
  "Claude Code" identity must not be used.
- The user tests on their phone before commits. Commit when asked, and only what they asked
  for (e.g. code but not docs).
- Don't take screenshots or otherwise probe the user's phone without asking.
