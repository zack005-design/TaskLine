# Offline expansion verification — 3 October 2026

Publication scope: this report covers the offline APK in the [3 October development release](https://github.com/zack005-design/TaskLine/releases/tag/dev-2026-10-03). Documentation was reorganized under `docs/`; application source was preserved byte-for-byte during publication. Selected fixture screenshots are included under `docs/screenshots/`. Raw build/device logs remain local.

The separate one-click test-data feature has not been integrated into this source or APK. Its feature-only patch and verification report remain preserved locally in its managed worktree and a recovery copy. Its isolated snapshot predates the final quick-entry fix; its reported quick-entry failure does not supersede the passing 40-test offline build below. It requires integration and a combined verification run before inclusion in a future release.

- Implemented offline boards/matrix, smart task-entry suggestions, duration/deadline fields, year planner, habits, notes, countdowns, title templates, saved filters, local comments/attachments, activity records, focus pause/resume, accent themes and Daily rituals widgets. Full scope and remaining reference-app differences are documented in OFFLINE_FEATURES.md; this is not complete Todoist/TickTick parity. No cloud services were added.
- Final app/test build and lint passed: build/fork-final-build.txt. Lint: zero errors, 31 warnings. The final test-only assertion/capture update built successfully in build/fork-regression-build.txt. All 25 JVM tests passed, with zero failures/errors in app/build/test-results/testDebugUnitTest/.
- Final full Android 16/API 36 suite: **40 tests passed**, zero failures (build/fork-full-device-suite.txt, 126.112 seconds). Coverage includes migrations through database version 5, library/attachment backup round trips and rollback, board moves, habit check-in/undo, template task creation, smart scheduling, focus pause/resume, widget binding, and the existing task/planner flows.
- Dark mode with 150% text: **5 tests passed** (build/fork-dark-large.txt), covering new feature flows, composer draft restoration and quick-entry persistence/filtering. Light/standard text settings were restored afterward. Real-window light/dark screenshots are in build/offline-verification/index.html. The gallery includes habits, board, matrix, year and quick entry with the keyboard; fixtures use isolated in-memory data.
- Fixed a real quick-entry regression: New task overlapped Add while the quick-entry field was focused. The floating action now hides during quick entry and returns after successful save clears focus. The regression test verifies the action is absent, Add is enabled, the task reaches the database and the Unscheduled filter finds it. Both light and dark/large-text runs passed. Captures confirm Add remains above the keyboard.
- Development-signed APK: build/deliverables/TaskLine-2026-10-03-offline-debug.apk (14,951,341 bytes). SHA-256: **9B817788C716F44D712A75112A49694202061411B6EF9008CC1D7B099013D3E1**. Adjacent checksum supplied. The packaged bytes match the tested app build. No publication or commit performed.
- Final checks: merged debug manifest has no INTERNET permission. Whitespace check passed with Windows CRLF recognized (`git -c core.autocrlf=false -c core.whitespace=cr-at-eol diff --check`). Existing tracked/untracked work, local configuration and task-safety-backups remain intact.
- Superseded attempts remain in build/: earlier 37/40-test runs failed quick-entry saving; the final 40-test pass supersedes them. A shared-emulator run was interrupted by another installation and is not counted. During fork verification, occupied console port 5584 required a new isolated emulator on console 5576 / ADB 5607, private server 5041. Boot-time System UI ANR and failed pre-boot installs are not passing evidence. The quick-entry failure reproduced after the dialog was cleared, then passed after the overlap fix.
- Limits: native Android development build, no iOS binary, production signing, store release, physical-device or TalkBack audit. Widgets were bound/rendered and checked for capacity/click targets; end-to-end launcher tap navigation was not separately verified. File opening depends on an installed viewer. Background timing remains approximate. Appearance/current focus state are device preferences outside portable backups; full limits are in OFFLINE_FEATURES.md.

---

# Custom TaskLine redesign verification — 2 October 2026

- Rebuilt the existing Android presentation with custom task rows and overflow actions, summary shortcuts, bottom navigation, project collection cards, calendar segmented controls, statistics, sheets, focus styling and a matching home-screen widget. No database migration or replacement was performed.
- Remade the task composer after user feedback: prominent borderless title, optional notes, compact project/priority rows, expandable scheduling, inline subtasks, and persistent Save. Removed the progress slider; status is under More options. Existing progress is preserved when editing.
- Final build: assembleDebug, assembleDebugAndroidTest and lintDebug passed in build/redesign-delivery-build.txt. Lint: zero errors, 26 warnings. All 21 JVM tests passed in build/redesign-validation-build.txt; unchanged domain tests were not repeatedly rerun for presentation-only refinements.
- Android 16/API 36: full suite passed 33 tests, zero failures (build/redesign-composer-suite.txt). After the final widget clipping and equal-height summary-card adjustments, the widget host/size and design interaction checks passed again: 3 tests (build/redesign-final-targeted.txt). Widget verification now checks the final text line, not just view bounds.
- Dark mode at 150% text: design navigation, Save visibility and draft-restoration checks passed, 2 tests (build/redesign-dark-large-tests.txt). The final design-only rerun after the last equal-height card adjustment passed 1 test (build/redesign-final-dark.txt).
- Screenshot gallery: build/redesign-verification/index.html. Screens under its files/ folder are real-window Android screenshots from isolated in-memory sample tasks. widget.png is the rendered RemoteViews fixture. Confirmed composer Save remains visible with the keyboard in dark/150% text. No sample tasks were inserted into the user's persistent task database by the design test.
- Development-signed APK: build/deliverables/TaskLine-2026-10-02-redesign-debug.apk.
- SHA-256: 3399CB7E8CF7A8FD83CAFFA20449604706C7F6DAC59E4DB4069732D4A4DFDDEE. An adjacent checksum file is included.
- Superseded attempts are retained: Android 17's image failed UI automation with InputManager.getInstance incompatibility in the existing test library; API 36 is the verified test target. The first API 36 full pass had a focus-resume selector ambiguity after task titles became tappable; the selector was made explicit and the full 33-test run passed. An initial widget capture clipped its last line; the minimum height, row capacity and text-layout assertion were corrected and retested. One lint pass hit an internal PSI error, followed by successful final lint runs. Initial emulator System UI ANR captures are not final design evidence.
- Scope limits: Android app with iOS-inspired styling, not an iOS binary. No physical-device, TalkBack, production signing, publication or live Google sync validation. Existing statistics-date estimates and approximate background reminders remain unchanged.

---

# Final planner integration verification — 1 October 2026

- Preserved the combined month/week calendar, selected-day agenda, quick entry, upcoming/unscheduled filters and timeline together with the completed task sheet, Apple-inspired design, focus timer, statistics, reminders, imports and backups. No additional source repair was needed in this final validation pass.
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest`, and `:app:lintDebug` passed. All 21 JVM tests passed; lint reported zero errors and 25 warnings.
- Full `:app:connectedDebugAndroidTest` run: 31 tests, zero failures, zero ignored on Android 16/API 36 medium_phone. This includes selected-calendar-date persistence, agenda completion, quick-entry filtering and timeline/search interaction. Evidence: `app/build/outputs/androidTest-results/connected/debug/TEST-medium_phone(AVD) - 16.xml` and `build/planner-verification/device-suite.txt`.
- The first device attempt ran before boot completed and installed no APK; Gradle misleadingly reported success. Only the subsequent 31-test run is counted as passing evidence.
- Inspected the live calendar in light mode and dark mode with 150% text. Screenshots: `build/planner-verification/calendar-light.png` and `calendar-dark-large.png`. Dismissed an emulator System UI not responding prompt using Wait before inspection. Restored font scale 1.0 and light mode afterward.
- Verified debug APK: `build/deliverables/TaskLine-2026-10-01-planner-debug.apk`. SHA-256: `71B4EA1DB71A27620775F693FF5D28070B92D77FF00806CFAE28B12407FCE2C1`; adjacent `.apk.sha256` file supplied. Installed and launched this app successfully on the emulator. This artifact is development-signed, not a production/store release.
- `git diff --check` passed (line-ending notices only). All owned build/test processes finished before handoff. Existing work and safety backups remain preserved.
- Limits: no physical-device, TalkBack, production-signing or live Google-account synchronization validation. Calendar import is a one-time copy, not ongoing sync; recurrence stores the next occurrence rather than full occurrence history. Earlier statistics/background-delivery limits below still apply.

---

# TaskLine enhancements verification — 28 September 2026

The seven-phase task-sheet enhancement pass is complete. The historical report below is retained separately.

- Debug APK: `build/deliverables/TaskLine-2026-09-28-enhancements-debug.apk`
- SHA-256: `15D8C4355466D797FCC46AFE30F0EB911EE13EC646A089573D385BB295B9D0F2`
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `:app:lintDebug`: passed. Lint reports zero errors and 25 warnings.
- JVM unit suite: 21 tests, zero failures; XML evidence under `app/build/test-results/testDebugUnitTest/`.
- Final full Android suite: 31 tests passed on Android 16/API 36 `medium_phone`, using the installed debug APKs and AndroidJUnitRunner. Authoritative final output: `build/taskline-verification/resume/full-device-suite.txt` (104.035 seconds, `OK (31 tests)`).
- An earlier connected-test report contains an intermittent inline-subtask click failure. The targeted test and subsequent full 31-test instrumentation run passed without further source changes. That older XML is not the final successful run.
- `git diff --check`: passed, with line-ending normalization notices only.

Coverage includes task draft restoration, atomic task/subtask persistence and rollback, swipe completion/reopening/delete confirmation, statistics boundaries/time zones, Lottie parsing/rendering, focus persistence/stale completion protection, and existing planner/import/backup/reminder behavior.

Live emulator inspection confirmed the new task sheet with title autofocus, keyboard, and visible Save footer. Actual screen captures: `build/taskline-verification/resume/live.png` and `live-task-sheet.png`. The empty draft was cancelled. Compose-generated task-sheet/focus screenshots capture the underlying activity rather than the modal and should not be used as modal visual evidence. A cold-boot System UI ANR was dismissed before the final successful test run.

Limitations: statistics infer completion dates from `updatedAt`, not a completion-history table; later edits can shift counts. Focus and weekly notifications use deferrable WorkManager execution, so background timing is approximate. LottieFiles downloads returned HTTP 403; five original bundled vector animations are documented in `app/EMPTY_STATE_ASSETS.md`. No physical-device, TalkBack, release-signing, or real elapsed-week verification was performed. Dark mode and enlarged-text visual integration should be repeated by the subsequent design/planner owners. This APK uses development signing only.

---

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
