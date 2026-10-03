# TaskLine user guide

## Tasks and projects

Create a project in **Projects**, then choose **View tasks** to add tasks to it. Use quick entry for a title or **New task** for notes, project, priority, schedule, and subtasks. Review smart-entry suggestions and explicitly apply the ones you want. Editing preserves task progress.

Search and project filters apply to List, Board, and Matrix. Board status menus persist task moves. Swipe right to complete/reopen a task or left to request deletion; deletion requires confirmation. Upcoming shows tasks due in the next seven days; Unscheduled shows incomplete tasks without a due date.

Deleting a project detaches its tasks. Deleting a task cascades to its subtasks and tag links. Removing a tag preserves its tasks.

## Calendar and scheduling

Expand **Schedule** in the task composer to set a due date, due time, reminder, recurrence, duration, or separate fixed deadline. Calendar dates retain their day when time zones change. Timed reminders follow the device's local time zone.

Use **Calendar → Agenda** to select a day, Month/Week to change the calendar view, Timeline for the bounded 14-day task chart, or the year overview to open a month.

Daily, weekly, monthly, and yearly recurrence supports custom intervals. Completing a recurring task advances to its next scheduled date, skips missed dates, and resets progress and subtasks. Month-end and leap-day anchors are retained; full occurrence history is not recorded.

Reminders use Android inexact alarms and can be delayed by battery management. Complete and Snooze actions are available on notifications. Pending reminders are reconciled after data changes, app resume, reboot, clock/time-zone changes, and app updates. Force-stopping the app prevents receivers from running until you reopen it. Enable notifications when prompted.

## Library and task context

Open **Library** for habits, notes, countdowns, title templates, saved filters, and activity. Habits have daily numeric targets, check-in/undo, seven-day history, and a current streak. Templates create tasks from titles; saved filters combine text, priority, and status.

Task details support local comments and attachments up to 2 MB each. Attachments are copied into local storage and included in portable backups. Opening them requires an installed viewer. Activity records task changes from this version onward; earlier history is not fabricated.

## Focus, statistics, and appearance

Choose **Focus** from a task's details, select a duration, and start the timer. Pause/resume and stop controls are available. Session state survives process death; completion notifications depend on Android background scheduling. Tap the running timer in the top bar to return to its sheet.

**Stats** shows active/overdue/done counts, a completion streak, a seven-day activity estimate, and project progress. Dates are inferred from completed tasks' last-update timestamp; later edits can change counts. Reopened and repeating tasks are excluded. The weekly local nudge uses the same estimate and skips empty weeks or disabled notifications.

Open **Tools → Appearance** for six accent palettes and system/light/dark mode. Appearance and the current focus session are device preferences outside portable backups.

## Calendar import

Open **Tools → Calendar import** to select a synced phone calendar or `.ics` file. Choose a project, review the preview, select events, and confirm. Extract Google Calendar ZIP exports before importing an `.ics` file. Re-importing the same source skips known event keys.

Import makes a one-time copy into tasks; it does not synchronize or upload data. Tasks are due at event start with reminders initially off. Phone-calendar import uses accounts already synced to Android; TaskLine has no Google sign-in.

The bounded `.ics` reader supports all-day events, UTC/IANA time zones, basic daily/weekly/monthly/yearly recurrence, counts/end dates, and exclusions. Unsupported rules, custom time zones, durations, and edited recurrence sets are reported as skipped. Use phone-calendar import for provider-expanded complex recurrence. File import is limited to 10 MB, 5,000 events, and a preview window of at most one year.

## Backup and restore

Use **Tools → Backup & restore** to export a file or inspect an existing backup. Check the preview before **Replace and restore**, which replaces current records. Cancel preserves current data. Replacement happens transactionally; validation or insertion failures leave the existing database unchanged.

Database version 5 has explicit migrations through 1→2→3→4→5 and no destructive migration fallback. Legacy dates are converted to calendar-date encoding; records without an original time zone retain the day visible in the device's zone at upgrade time.

Current backups use format 4 and accept formats 1–4. They include projects, tasks, subtasks, tags, relationships, scheduling/import state, and library records including attachment content and activity. Older backups replace Library with an empty library; the restore preview explains this. IDs, references, dates, schedules, completion state, and record types are validated before confirmation.

Backups are readable, unencrypted JSON and limited to 20 MB. Wait for **Backup saved** before relying on a file; an interrupted export may be incomplete. If Android kills the process before restore confirmation, choose the backup again.

## Home-screen widgets

Add **TaskLine · Up next** or **Daily rituals** through your launcher's widget picker. Up next orders incomplete tasks by due date, priority, and ID; individual rows open their task. Daily rituals summarizes habits, countdowns, and notes and opens Library. Both adapt row capacity to size and font scaling.

Android controls actual refresh timing, including requested periodic updates. Widgets refresh when task/library data changes while the process runs. Inline task completion and habit editing are not implemented in the widgets.

See [feature boundaries](OFFLINE_FEATURES.md) and [verification](VERIFICATION.md) for remaining limits.
