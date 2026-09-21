# TaskLine

A native Android task and project app built with Kotlin, Jetpack Compose, Material 3, and Room. TaskLine stores its data locally and uses Flow and StateFlow to connect persistence to screen state.

> **Status: Phase 2 — Task Foundation.** This is an early development build, not a finished task manager. The current screen displays task and project counts and load errors. Creating, editing, and organizing items through the UI is not implemented yet.

## Download the Android test build

[Download TaskLine v0.2.0-alpha APK](https://github.com/zack005-design/TaskLine/releases/download/v0.2.0-alpha/TaskLine-v0.2.0-alpha-debug.apk) · [Release notes](https://github.com/zack005-design/TaskLine/releases/tag/v0.2.0-alpha)

Requires **Android 8.0 (API 26) or newer**. Download the APK on your Android device, open it, and allow installation from your browser or file manager if prompted. The installed app currently appears as **Task Foundation**.

This APK is debug-signed for early testing, not a Play Store release. It opens a minimal foundation screen; task creation and editing cannot yet be tested through the app. APKs are attached to GitHub Releases rather than committed into the source repository. The release also includes a SHA-256 checksum.

## Implemented

- Room entities for tasks, subtasks, projects, tags, and task-tag associations.
- Task and project DAOs with observable queries and database writes.
- Concrete offline repositories, entity-to-domain mapping, validation, and an injectable clock.
- Task and project ViewModels exposing StateFlow, with separate UI models.
- Foreign keys and indexes for relationships and common queries.
- Exported version 1 database schema and an instrumented project-deletion regression test.
- A minimal Compose screen wired to the repositories through ViewModels.

Task records include `id`, `title`, `description`, `projectId`, `startDateTime`, `dueDateTime`, `priority`, `status`, `progress`, `isCompleted`, `createdAt`, and `updatedAt`. Date/time values are nullable Unix epoch milliseconds; creation and update timestamps are epoch milliseconds. Progress ranges from 0 to 100.

## Data behavior

Room is the persistent source of truth. No cloud service, synchronization, account, or demo-data seeding is implemented. The application manifest does not request Internet permission.

Deleting a project sets its tasks' `projectId` to null; it does not delete those tasks or tasks in other projects. Deleting a task cascades to its subtasks and tag associations. Deleting a tag removes its associations, not the tasks themselves.

New databases are created at version 1. Destructive migration fallback is not enabled; future schema changes need explicit migrations.

## Architecture

```text
Compose UI → ViewModels → repository interfaces → Room repositories → DAOs → SQLite
                       ← StateFlow / domain models ← Flow / entity mapping ←
```

The `data` package owns Room entities, DAOs, mappers, and repository implementations. The `domain` package contains models, repository contracts, and the clock abstraction. The `ui` package owns screen models and ViewModels. `AppContainer` wires the concrete implementations. Room entities are not exposed to the UI.

## Build from source

Requirements: Android Studio with support for the configured Android Gradle plugin, JDK 17 or newer compatible with Gradle, Android SDK Platform 36, and SDK build tools. Gradle dependencies require an Internet connection on the first build.

```sh
git clone https://github.com/zack005-design/TaskLine.git
cd TaskLine
```

Open the folder in Android Studio and let it configure your SDK path. Alternatively, create an untracked `local.properties` containing `sdk.dir` for your Android SDK. Set `JAVA_HOME` to a compatible JDK; Android Studio's bundled JBR can be used.

Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

macOS / Linux:

```sh
sh gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The Gradle wrapper is included; a separate Gradle installation is unnecessary. The app currently uses application ID `com.example.taskfoundation`, version name `1.0`, and version code `1`. The `v0.2.0-alpha` GitHub label identifies the foundation milestone, not the Android package version.

## Verification

The debug APK build and instrumented-test compilation have passed. `ProjectDeletionTest` covers detaching the deleted project's tasks while preserving a second project's task relationship.

To execute the instrumented test, connect an Android device or start an emulator, then run:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Instrumented tests and interactive device testing have not yet been verified. Compilation does not establish on-device behavior.

## Planned work

- Task and project creation, editing, deletion, and navigation in the UI.
- Task assignment, subtasks, and tag management screens.
- Interaction, persistence, and accessibility testing on devices.
- Calendar, Gantt, widgets, and reminders in later phases.
- Production branding, signing, and release preparation.

## Repository contents

Only source, resources, Gradle configuration and wrapper files, tests, the exported Room schema, and documentation belong in Git. Generated builds, caches, machine-specific SDK paths, signing keys, and APKs are excluded. Test APKs are distributed through Releases.
