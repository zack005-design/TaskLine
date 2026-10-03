# TaskLine offline feature expansion

The user explicitly excluded cloud features. TaskLine remains a native Android app with an iOS-inspired presentation. There are no accounts, subscriptions, cloud sync or online collaboration added by this change.

Reference inventory checked on 2026-10-02: [Todoist plans](https://www.todoist.com/pricing) and [TickTick plans](https://ticktick.com/about/upgrade). These references include both individual productivity tools and hosted services. This implementation is not complete feature-for-feature parity with either product.

## Available in this expansion

| Area | Working behavior |
| --- | --- |
| Task views | List, status board and four priority/deadline quadrants. Board moves persist, including recurring completion behavior. Existing search and project filters apply across views. |
| Smart composer | Reviewable English suggestions for today/tomorrow, next week, weekday names, ISO dates, times, p1–p4 and daily/weekly/monthly/yearly repeats. Apply is explicit; unsupported text remains editable. |
| Planning | Task duration and a separate fixed deadline; year overview opens the selected month. Existing month/week, agenda and timeline remain available. |
| Habits | Daily numeric targets, check-in/undo, seven-day history and current streak. Records survive relaunch and backup/restore. |
| Library | Editable notes, dated countdowns, reusable task-title templates and saved text/priority/status filters. Template creation is transactional. |
| Task context | Local comments and file attachments up to 2 MB each. Attachments are copied into local storage and included in portable backups. Opening a file uses an installed viewer. |
| Activity | Task creation, updates, completion, reopening and deletion are recorded from this version onward. Previous history is not fabricated. |
| Focus | Selectable session length, persistent pause/resume, existing completion notifications and stop controls. |
| Appearance | Six accent palettes and system/light/dark modes. Preferences persist locally. |
| Widgets | Up next opens individual tasks; Daily rituals shows habits, countdowns and notes and opens Library. Both adapt row count to size and font scaling. |
| Data | Non-destructive database migrations 3→4→5; backup format 4 accepts formats 1–4, validates before replacing data, and includes all library records. |

No paid feature gates or task/project/habit quotas were introduced. File size and backup size limits protect local memory use.

## Boundaries and remaining differences

- Accounts, multi-device/cloud sync, shared workspaces, invitations, remote assignments/comments, online calendar subscriptions, email delivery, hosted AI and service integrations are excluded by the offline requirement.
- This does not create iOS, desktop, browser-extension or watch clients.
- Board movement uses an accessible menu; drag-and-drop ordering, project sections/folders and nested task hierarchies are not implemented.
- Saved filters use text/priority/status fields, not a general query language. Templates contain task titles, not a full project structure.
- Habits currently use daily targets; custom weekday schedules and habit-specific reminder schedules are not implemented.
- Scheduling retains one task reminder and the existing recurrence grammar. Multiple reminders, subtask reminders and arbitrary recurrence rules are not implemented.
- Advanced hour-grid scheduling, calendar drag-rescheduling, multiweek/split layouts, automatic backup retention and long-term focus analytics are not implemented.
- Widgets are summaries rather than independently editable task/habit lists. Launcher refresh timing is controlled by Android.
- Appearance and the current focus timer are device preferences, not portable backup data. Backup files are readable JSON and have a 20 MB limit. Restoring an older backup replaces the library with an empty library; the UI explains this before confirmation.
- Existing completion charts still estimate dates from task updates; the new Activity view is a separate event record.

See `VERIFICATION.md` for executed checks and device limitations. Source and APK are development artifacts until device/release validation is performed.
