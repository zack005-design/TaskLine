# Shared workspace safety

The user explicitly requires concurrent chats to avoid conflicting edits and loss of important work.

- Read `TASK_COORDINATION.md` before modifying this checkout. Only the listed current writer may edit source, tests, configuration, or shared documentation or run builds against this checkout. Other chats may inspect read-only and must yield until handed ownership.
- Preserve all pre-existing tracked and untracked changes. Re-read a file immediately before editing; never overwrite it using an old in-memory copy.
- Do not delete unrelated files, backups, user data, signing keys, or local configuration. Do not run broad cleanup, `git clean`, destructive resets/restores, stash, branch switches, or worktree moves in this shared checkout as part of routine task work.
- Keep `task-safety-backups/` local and intact. It contains recovery material and may contain local configuration; do not commit, publish, or upload it.
- Before a handoff, finish or stop your own build/test processes, preserve current files, record verification and outstanding issues in `TASK_COORDINATION.md`, then set the next writer and notify that chat. Never have two writers. Do not kill another chat's processes.
- If a change requires removal or replacement of important existing work, preserve a verified recovery copy and obtain explicit user authorization for that specific destructive change.
- New concurrent tasks must wait for ownership or use a separately managed worktree after existing uncommitted work has been accounted for. Do not assume a new worktree includes these uncommitted files.

These are cooperative agent rules, not an operating-system lock.
