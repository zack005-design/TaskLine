# TaskLine visual system

Updated 2 October 2026. Native Android Compose with an iOS-inspired hierarchy and original TaskLine components. The reference direction is the calm task organization on [Todoist](https://www.todoist.com/) and the calendar-led planning on [TickTick](https://ticktick.com/?language=en_US). These are product-pattern references; no branded assets or screen copies are included.

## Visual language

- Light canvas #F7F7FA, white grouped surfaces, indigo #4C61D6, restrained project colors. Dark mode uses charcoal surfaces and a lighter indigo.
- 34 sp large headings, 17 sp task titles, quiet secondary metadata, and scalable system typography.
- 16–22 dp content corners and a fixed four-tab bottom surface. Task rows have circular completion controls, tappable details, and a labeled overflow menu. Progress appears only when partially complete.
- Minimum 48 dp action targets; custom controls retain selected, disabled and checkbox semantics. Horizontal filters and vertical content scrolling accommodate more content and larger text.

## Screen anatomy

- Tasks: compact date heading, three actionable summary cards, search, time filters, optional project filter, task list, and quick capture at the end. Summary shortcuts clear the current search and project context so global counts match the opened list.
- Projects: custom collection cards with an original folder glyph, remaining count, project color, completion line, and a View tasks action.
- Calendar: custom Agenda/Timeline and Month/Week segmented controls, month navigation, day markers, selected-day agenda, and existing date-aware task creation.
- Statistics: completion ring, active/overdue/done metrics, estimated daily activity chart, streak and project progress.
- Task composer: a large borderless title, optional notes, compact Project/Priority rows, expandable Schedule, inline subtasks and a persistent Save action. No progress slider. Status is available under More options; editing preserves existing progress. Project editing, details and backups use rounded sheets with scrollable content. Focus uses a circular countdown. Imports use the same typography and grouped controls.
- Home-screen widget: branded Up next header, opaque day/night surfaces and inset task rows. Resizing and larger text reduce the visible rows when needed. It keeps the existing repository-backed ordering and tap-to-open behavior.

## Implementation

The offline expansion adds List/Board/Matrix task controls and a Library entry point. Library uses the same sheet anatomy for habits, notes, countdowns, templates, filters and activity. Habits show a seven-day check-in strip and explicit undo. Board cards use status menus; matrix categories state their priority/date rules. Calendar adds a year overview. Composer scheduling now includes a duration and independent deadline, with reviewable smart-entry suggestions above the notes field.

Task details includes local comments and attachments. Focus keeps the circular timer and adds pause/resume with selectable session length. Appearance offers six accent palettes with system/light/dark modes. A separate Daily rituals home-screen widget summarizes library content, while individual Up next rows open their task. Both widget layouts use the established legible day/night surfaces and size-dependent row capacity. See OFFLINE_FEATURES.md for feature boundaries.

TaskLineDesign.kt owns segmented controls, section headings, the progress ring, project collection cards, primary buttons and shared sheet anatomy. TaskLineComponents.kt owns task rows, tabs, filters, form fields and vector glyphs. Existing persistence, reminder scheduling, import, backup and focus state remain authoritative.

The app remains Android; this is not an iOS binary or Apple's native material renderer. Standard platform pickers and critical deletion confirmations remain available through the custom presentation.

## Verification

TaskLineDesignTest uses an isolated in-memory database to check summary navigation and capture real-window screenshots of Tasks, Today, Projects, Calendar, Week, Stats and the editor. No sample tasks are added to the user's stored database. The existing interaction suite checks creation, editing, completion, swipes, calendar persistence, subtasks, focus and imports. See VERIFICATION.md for exact results and limitations.
