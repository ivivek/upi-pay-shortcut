# UPI Shortcuts

Pay your regular shops with one tap. Scan a merchant's UPI QR code once, pin it to your home
screen, and from then on tapping the icon opens your UPI app straight on that merchant's payment
screen, with the amount field focused. No more scanning the same QR code every day.

## Features

- **Save merchants** by scanning their QR code with the camera, scanning a screenshot or photo,
  sharing a QR image or `upi://` link from another app (e.g. WhatsApp), or typing the link in.
- **Pin to home screen**: each merchant becomes an icon (their initial on a colour you pick).
- **One tap to pay**: the icon opens the UPI app directly. On a mid-range phone our part of the launch
  measured 17–83 ms; the rest is the UPI app starting up.
- **Choose the UPI app per merchant** (PhonePe, GPay, Paytm, …) or use the system default.
- **Edit and it follows**: renaming or changing a merchant's link or colour updates the pinned icon.
- **Long-press the app icon** for your 4 most-used merchants.
- **Home-screen widget** with a grid of all merchants.
- **Export / import** merchants as JSON; Android Auto Backup also covers them.
- **Link checks**: warns about fixed amounts, one-time transaction references, personal
  (non-merchant) UPI IDs and duplicates.

## Privacy

The app has **no internet permission** and never asks for any runtime permission.

- The camera scanner is Google's Code Scanner, which runs in Google Play services and hands back
  only the QR text, so the app needs no camera permission.
- Images and backup files are opened through Android's photo picker and file dialog, which grant
  access only to the file you choose.
- The only declared permissions come from the widget library (WorkManager: wake lock, run at
  startup, foreground service) and are granted silently. See `docs/TODO.md` for removing them.

## How it works

```
Home-screen icon ──► PayActivity (invisible) ──► reads merchant from local DB
                                              └─► Intent(ACTION_VIEW, upi://pay?...) ──► UPI app
```

A pinned shortcut carries only the merchant's id. `PayActivity` looks up the saved `upi://pay`
link, hands it to the chosen UPI app (or the system default) and closes. Because the link lives in
the database, edits reach existing icons without re-pinning.

The link is stored exactly as scanned (only spaces are encoded), so merchant fields such as `mc`,
`orgid` and `sign` reach the UPI app untouched.

## Limitations

- **Saved QR codes can go stale.** Shops change their QR codes (new payment provider, new sound
  box). Paying an old saved code may reach an account the shop no longer uses, and the shop's
  sound box won't announce it. The app shows a one-time warning on first launch; if the QR at the
  counter changes, delete the merchant and scan again.
- **Apps can't remove home-screen icons.** Deleting a merchant disables its icon (greyed out) and
  asks you to remove it yourself.
- **Payments start from another app.** UPI apps know the payment was started by another app, not
  by their own scanner, and may apply their own checks. Merchant QR codes worked in testing with
  PhonePe.
- Some launchers don't support pinned shortcuts; the widget and long-press menu still work there.
- The app can't tell whether a payment succeeded: it hands off to the UPI app and closes.

## Build

Requirements: JDK 17+ and the Android SDK (compileSdk 36). `build.sh` defaults to
`JAVA_HOME=/opt/jbr` and `ANDROID_HOME=/opt/android-sdk` if unset.

```sh
./build.sh build     # debug APK → app/build/outputs/apk/debug/app-debug.apk
./build.sh install   # build, install on the connected device, launch
./build.sh test      # JVM unit tests
```

- Package: `com.linetra.upishortcut`
- Supports Android 8.0 (API 26) and later.
