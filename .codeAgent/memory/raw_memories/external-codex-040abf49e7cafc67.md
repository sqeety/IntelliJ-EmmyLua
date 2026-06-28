source_id: external-codex-040abf49e7cafc67
updated_at: 2026-04-11T19:48:53.0640600+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/intellij-movefiletest-write-action.md

## Task group
IntelliJ plugin Kotlin tests / refactoring test fixes.

## Scope of applicability
Applies to `<workspace>/src/test/kotlin/com/tang/intellij/test/refactoring/MoveFileTest.kt` and similar tests that directly invoke IntelliJ refactoring/move APIs.

## Search keywords
`MoveFileTest.kt`, `MoveFilesOrDirectoriesUtil.doMoveFile`, `runWriteAction`, `ApplicationManager.getApplication()`, `Write action`, `com.tang.intellij.test.refactoring.MoveFileTest`, IntelliJ platform upgrade test failure.

## Problem / failure symptom
The test case in `MoveFileTest.kt` failed directly at `doMoveFile(...)`, before final assertions. Cause was not fixture expectations or production move/refactoring logic.

## Cause
After an IntelliJ platform upgrade, `MoveFilesOrDirectoriesUtil.doMoveFile(...)` requires being executed inside an explicit write action.

## Fix
In `MoveFileTest.kt`, wrap the actual file move call with:

`ApplicationManager.getApplication().runWriteAction { ... }`

This was a test-only fix; production code was not changed.

## Verified command
From workspace root on PowerShell:

`./gradlew.bat test --tests com.tang.intellij.test.refactoring.MoveFileTest`

Result: passed.

## Guardrails / notes
- If similar tests fail at IntelliJ PSI/VFS/refactoring mutation APIs, first check whether the operation needs a write action.
- There was a `fullLine` plugin creation warning during the Gradle run, but it did not affect `MoveFileTest` passing.
