source_id: external-codex-b7c41e9f4a004fdf
updated_at: 2026-04-11T22:31:03.1740162+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/lua-emmy-field-insertion-after-last-field.md

## Task group
Lua/IntelliJ EmmyLua annotation generation and quick fixes.

## Scope of applicability
Applies when modifying generated Emmy annotation inspections, Lua comment manipulation, or field creation intentions in the workspace project.

## Search keywords
`GeneratedEmmyAnnotationInspections.kt`, `LuaCommentUtil.kt`, `CreateFieldFromParameterIntention.kt`, `GeneratedEmmyAnnotationInspectionTest.kt`, `---@field`, `---@class`, `self.foo`, `self.a`, `CreateFieldFromParameterIntention`, `GeneratedEmmyAnnotationInspectionTest`, `classDef.textRange.endOffset`.

## User correction / preference evidence
User reported that generated `---@field` for `self.a` was inserted in the wrong place: it appeared after the `---@class` first line and got squeezed onto the same line as the original first line. Expected behavior: insert after the last existing `---@field` line.

## Durable implementation details
- Centralized field annotation insertion in `src/main/java/com/tang/intellij/lua/comment/LuaCommentUtil.kt` around line 167.
- New logic should prefer appending new `---@field` after the last existing `---@field` in the class comment block.
- If there is no existing `---@field`, append at the end of the whole class annotation/comment block rather than directly at `---@class` line end.
- Both template-based and direct-write field insertion should use the centralized helper.
- `src/main/java/com/tang/intellij/lua/codeInsight/inspection/GeneratedEmmyAnnotationInspections.kt` around line 174 changed the `self.foo` quick fix to use the helper instead of direct class range offsets.
- `src/main/java/com/tang/intellij/lua/codeInsight/intention/CreateFieldFromParameterIntention.kt` around line 93 was aligned to the same insertion helper to avoid duplicate offset bugs.
- Guardrail: `CreateFieldFromParameterIntention` has a template-finish callback that inserts/keeps assignment like `self.xxx = param`; preserve this callback when refactoring field insertion.

## Tests / verification
- Regression test added in `src/test/kotlin/com/tang/intellij/test/inspections/GeneratedEmmyAnnotationInspectionTest.kt` around line 133.
- Test covers existing `---@field` case: new generated field must be appended after the last field line.
- Verified command:
  `./gradlew.bat test --tests com.tang.intellij.test.inspections.GeneratedEmmyAnnotationInspectionTest`
- Result reported as passing.

## Failure guardrails
- Avoid inserting a new field using `classDef.textRange.endOffset` or directly at the `---@class` line; this caused line-squashing / wrong placement.
- When adding another field creation path, route it through `LuaCommentUtil` field insertion logic so behavior stays consistent.
- Ensure inserted annotation includes proper newline handling and does not concatenate with an existing annotation line.
