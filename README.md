# TaskLine

An offline Android task and project planner built with Kotlin, Jetpack Compose, and Room. Requires Android 8.0 / API 26 or newer.

## Features

- Apple-inspired translucent cards, rounded controls and home-screen widget, floating navigation, light/dark themes, and large-text layouts. This is an Android interpretation, not Apple's native Liquid Glass renderer.
- Tasks, projects, subtasks, tags, priorities, status, progress, search, and All / Today / Upcoming / Unscheduled / Overdue / Active / Completed filters. Upcoming shows tasks due in the next seven days; Unscheduled shows incomplete tasks with no date.
- A **Calendar** tab with two modes: **Agenda** (month picker + ordered daily task list) and **Timeline** (bounded 14-day Gantt chart with Previous, Today, and Next navigation showing progress and project).
- A **Stats** tab with active / overdue / done counts, a completion-streak banner, an animated 7-day bar chart, and per-project progress bars. Activity is estimated from completed tasks' last-update timestamp; reopened and repeating tasks are excluded.
- A **Focus timer** that starts a 25-minute countdown from a task's detail sheet. The session state is persisted and restored across process death. A notification fires on completion when Android allows background work. The live countdown is visible in the top bar while a session runs.
- A confetti burst animation when a task is marked complete.
- Calendar dates that stay on the same day when time zones change, plus optional local due times.
- Optional notifications at the due time or before it, with Complete and Snooze 10 min actions. Tapping a notification opens the relevant task.
- Daily, weekly, monthly, and yearly recurring tasks with custom intervals. Completion advances to the next scheduled date, skips missed dates, and resets progress and subtasks. Month-end/leap-day anchors are retained. No occurrence history is recorded.
- One-time import of calendars synced to Android (including Google Calendar), or an `.ics` file. Preview events, choose a project, select events, and confirm import. Re-importing the same source skips existing event keys.
- Validated JSON backup/restore with record-count preview, explicit replacement confirmation, and transactional rollback on failure.
- Retryable database loading errors and saved editor fields across activity recreation.
- A rounded **TaskLine · Up next** home-screen widget showing up to three incomplete tasks from the same database.

## Try it

1. Create a project in **Projects**, then use **View tasks** to add tasks to it.
2. In **New task** or **Edit**, set a due date to reveal due time, reminder, and repeat controls. Enable notifications when prompted if you want reminders.
3. Open **Tools → Calendar import** to select a synced phone calendar or an `.ics` file. Google Calendar ZIP exports must be extracted first. Review the preview before importing.
4. Open **Tools → Backup & restore** to export a file or inspect a backup. **Replace and restore** replaces current data; **Cancel** preserves it.
5. Add **TaskLine · Up next** through your Android launcher's widget picker. Tap the widget to open TaskLine.

## Scheduling and calendar limits

Reminders use Android inexact alarms and can be delayed by battery management. They follow the device's local time zone and require notifications to be enabled. Pending reminders are reconciled after data changes, app resume, reboot, clock/time-zone changes, and app updates. Force-stopping the app prevents Android from running its receivers until it is opened again. Physical-device/OEM background-delivery behavior is not yet verified.

Calendar import copies events into tasks; it is not ongoing synchronization and does not upload data. Imported tasks are due at the event start, with reminders initially off. Device calendar import relies on accounts/calendars already synced to the phone; TaskLine does not implement Google sign-in.

The bounded `.ics` reader supports all-day events, UTC/IANA time zones, basic daily/weekly/monthly/yearly recurrence, counts/end dates, and exclusions. Unsupported recurrence rules, custom time zones, durations, and edited recurrence sets are reported as skipped. Use the synced phone-calendar route for provider-expanded complex recurrence. File import is limited to 10 MB, up to 5,000 events, and a preview window of at most one year.

The widget requests periodic updates every 30 minutes; Android controls the actual update time. It also refreshes when tasks change while the process runs. Its three entries are ordered by due date, priority, and ID. It opens the app; inline widget task completion is not implemented.

## Data and backup

Room is the persistent source of truth. No Internet permission, cloud account, demo-data seeding, or synchronization service is included. Calendar access is read-only and requested when the user chooses phone-calendar import.

Deleting a project detaches its tasks. Deleting a task cascades to its subtasks and tag links. Tag removal leaves tasks intact.

Database version 3 has explicit migrations: 1→2 converts legacy local-midnight values to UTC-midnight calendar-date encoding; 2→3 adds scheduling and import fields. UTC-midnight date values must be decoded with `CalendarDates`, not converted as instants to the device's zone. Legacy records did not store their original time zone, so migration preserves the date visible in the device's zone at upgrade time. Destructive migration fallback is disabled.

Backups include projects, tasks, subtasks, tags, relationships, reminder/repeat state, and import keys. Current exports use format version 2; canonical version 1 backups remain readable with default scheduling values. The app validates types, IDs, references, dates, completion state, and schedules before showing restore confirmation. A failed replacement transaction leaves existing data unchanged. Exports/imports are limited to 20 MB. Backups are readable JSON and are not encrypted by TaskLine.

File operations run off the main thread. If Android kills the process before restore confirmation, choose the file again. An interrupted export may be incomplete; wait for **Backup saved** before relying on the file.

## Build and verify

Use Android Studio's bundled JBR (or a compatible JDK), SDK Platform 36, and the included Gradle wrapper. Configure the local SDK path in untracked `local.properties`.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=2
# With an emulator or device connected:
./gradlew.bat :app:connectedDebugAndroidTest --max-workers=2
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. It is a development build, not a signed store release. Application ID is `com.example.taskfoundation`, version name `1.0`, version code `1`.

The app explicitly aligns the serialization runtime with Room's migration-test dependency so the app and test APKs use a compatible ABI. See `VERIFICATION.md` for the final observed results and remaining verification limits.

## Source organization

- `domain`: task models, calendar-date encoding, recurrence/reminder calculations, and repository contracts.
- `data`: Room schema/DAOs/migrations, repositories, validated portable backup format.
- `calendar`: synced-calendar reader, bounded offline `.ics` reader, transactional duplicate-safe import.
- `reminders`: Room-derived Android alarm/notification scheduling and recovery receivers.
- `ui`: screen state, editors, calendar import, backup confirmation, reusable glass components.
- `widget`: native RemoteViews provider using the same task repository.

`DESIGN.md` documents the visual system and references. Android Studio component previews include light, dark, and 150% text. Generated builds, local paths, signing material, and APKs are excluded from Git.

## Remaining product work

Production signing/branding and store preparation; physical-device and TalkBack audits; full calendar synchronization; inline widget completion; task dependencies, templates, and completion history.
