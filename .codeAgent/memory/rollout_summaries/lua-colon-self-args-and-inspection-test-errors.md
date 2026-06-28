source_id: external-codex-6287495fa1ae947b
updated_at: 2026-04-11T19:43:36.0685934+08:00
cwd: <workspace>

Fixed Lua plugin test issue #338 and corrected MatchMemberInspectionTest expectations.
Issue #338 root cause: `:` calls plus explicit `self` double-counted self in `IFunSignature.processArgs(...)`.
Changed `src/main/java/com/tang/intellij/lua/ty/TyFunction.kt` so explicit first `self` avoids injecting implicit self for non-`colonCall` signatures.
Verified `TestStringLiteralType.test issue #338` and `test literal type 1` passed.
Corrected `MatchMemberInspectionTest.kt` expected highlights from warning to error with exact descriptions.
Verified `./gradlew.bat test --tests "com.tang.intellij.test.inspections.MatchMemberInspectionTest"` passed.
