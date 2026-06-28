source_id: external-codex-b7c41e9f4a004fdf
updated_at: 2026-04-11T22:31:03.1740162+08:00
cwd: <workspace>

Lua Emmy annotation generation: fixed `self.foo`/field quick-fix insertion to append after last existing `---@field`, not at `---@class` line.
Centralized field annotation insertion in `LuaCommentUtil.kt`; both inspection quick fix and `CreateFieldFromParameterIntention` use it.
Regression test added in `GeneratedEmmyAnnotationInspectionTest.kt`; verified with targeted Gradle test.
