# TODO

## Shop photo as merchant icon

Let the user pick a photo (e.g. the shop front) for a merchant's icon instead of the
initial-on-colour avatar, so merchants with the same initial are easy to tell apart.

- Pick the image with the photo picker (`PickVisualMedia`, no storage permission needed).
- Crop to a square and scale down to the adaptive-icon size (108dp). Keep the subject
  inside the 66dp safe zone, since launchers mask the edges to a circle or squircle.
- Save the image in app-private storage (`filesDir/icons/<merchantId>.png`) and store
  its path on `Merchant` (Room migration: add a nullable `iconPath` column).
- Use it everywhere the avatar appears: pinned shortcut (`Shortcuts.build`), the
  merchant list and form (`MerchantAvatar`), and the widget. The widget draws plain
  shapes today to keep RemoteViews small, so use a downscaled bitmap there.
- Keep the colour and initial as the fallback when there's no photo, and offer
  "Remove photo".
- Delete the file when the merchant is deleted.
- Export/import: either leave photos out, or embed them as base64 in the JSON.

## Preset amount per merchant

For payments that are always the same (daily milk, tiffin, rent), pre-fill the amount
so the UPI app opens with it already entered.

- Add an optional "Fixed amount" field to the merchant form.
- Store it in the link itself as `am=<amount>` (don't add a DB column), so the
  shortcut, widget and export all carry it automatically.
- Setting the amount replaces or adds `am`; clearing it removes `am`. Add a
  `UpiLink.withParam(link, key, value)` next to `withoutParam`, leaving every other
  parameter untouched.
- Validate: a positive number with at most 2 decimals. Format it as `123.00`.
- The "fixed amount" warning shouldn't show for an amount the user set on purpose.
  Only warn when the amount came from the scanned QR code.
- Check that each UPI app (PhonePe, GPay, Paytm) still lets the user change a
  pre-filled amount; some lock the amount when `am` is present.

## Zero permissions: rebuild the widget without Glance

The app declares `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`, all added
by WorkManager, which the Glance widget library depends on. Our own code needs none of them.
Rebuilding the widget without Glance drops all three, leaving no user-facing permissions
(only AndroidX's internal `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which users never see).

- Don't strip them with `tools:node="remove"`: WorkManager can take a wake lock, and without
  the permission that is a SecurityException, i.e. a crash in rare, hard-to-reproduce paths.
- Replace `widget/MerchantWidget.kt` with a plain `AppWidgetProvider` + `RemoteViews`:
  - Merchant grid: a `GridView` backed by a `RemoteViewsService`, or on Android 12+
    `RemoteViews.RemoteCollectionItems` (no service needed; minSdk 26 still needs the service).
  - Each cell: coloured circle with the initial, plus the name. Tap uses a
    `setPendingIntentTemplate` on the grid and `setOnClickFillInIntent` with the merchant id
    per cell, launching `PayActivity`.
  - Empty state: "Add merchants in UPI Shortcuts" opening `MainActivity`.
  - Refresh: replace `MerchantWidget.refresh()` with `AppWidgetManager.notifyAppWidgetViewDataChanged`
    and `updateAppWidget`, called from the same places (save, delete, import, after a payment).
- Remove the `androidx.glance:glance-appwidget` dependency. Check with
  `aapt2 dump permissions app-debug.apk` that only the internal permission remains.
- Bonus: a smaller APK, since Glance and WorkManager are dropped.
