source_id: external-codex-040abf49e7cafc67
updated_at: 2026-04-11T19:48:53.0640600+08:00
cwd: <workspace>

Fixed IntelliJ plugin test `MoveFileTest`: `MoveFilesOrDirectoriesUtil.doMoveFile(...)` must be run inside a write action on newer platform versions. Verified with `./gradlew.bat test --tests com.tang.intellij.test.refactoring.MoveFileTest`.
