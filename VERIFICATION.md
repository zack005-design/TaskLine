# TaskLine verification — 27 September 2026

## Delivered artifact

- Debug APK: `build/deliverables/TaskLine-2026-09-27-debug.apk`
- SHA-256: `A7472CDEBB3EC1250B18B0EB2F12679498028C8CC599B643BBA592852730C40D`
- Android application: `com.example.taskfoundation`, version 1.0 (1), minimum API 26.
- Installed and launched successfully on the Android 16 `medium_phone` emulator.
- Development signing only; no store submission or production release was performed.

## Observed results

- `:app:assembleDebug`: passed.
- `:app:testDebugUnitTest`: 14 tests passed. Covers calendar dates, recurrence anchors/skipped dates, local reminder calculations, ICS parsing, and widget ordering.
- `:app:connectedDebugAndroidTest`: 21 tests passed with dark mode and 150% text. Covers Room migrations, backup validation/rollback, retry states, task editing, scheduling controls, import preview/confirmation/deduplication, notification actions, and RemoteViews widget host inflation.
- After the final reminder transaction change, `ReminderDeliveryTest` passed again, including an obsolete notification action that must preserve a newer notification. The entire 21-test suite was not repeated after this isolated change; all 14 unit tests were repeated.
- `:app:lintDebug`: completed with zero errors and 18 warnings. Warnings include newer SDK/dependency availability, API-specific widget preview metadata, optional icon/KTX/resource cleanup, and a generic ViewModel context warning. The production factory explicitly supplies `applicationContext` to that ViewModel.
- `git diff --check`: passed; Git also reported Windows line-ending normalization notices.

Full device-suite evidence is preserved at `build/verification/full-suite.xml`; final reminder regression evidence is at `build/verification/reminder-regression.xml`. Unit results are under `app/build/test-results/testDebugUnitTest/`, and lint reports under `app/build/reports/`.

## Rendered inspection

Inspected the installed app in light mode at normal text size and dark mode at 150% text. Verified visible Tools entries for Calendar import and Backup & restore. Screenshots: `app/build/screenshots/final-light.png` and `app/build/screenshots/final-dark-large.png`. Restored the development emulator to normal text size and light mode afterward.

The visual system uses translucent Compose surfaces and rounded controls with an ambient background. It does not implement Apple's native Liquid Glass refraction. The native home-screen widget has matching rounded day/night resources; automated host rendering passed.

## Verification limits

No physical-device/OEM battery-management audit, real Google-account calendar import, TalkBack audit, or signed release validation was performed. Reminder tests exercise notification delivery and actions directly; they do not establish real-world inexact alarm latency or reboot delivery. Phone-calendar import reads calendars already synced to Android; it is not Google sign-in or ongoing synchronization. See README.md for backup, migration, recurrence, and ICS format limitations.
