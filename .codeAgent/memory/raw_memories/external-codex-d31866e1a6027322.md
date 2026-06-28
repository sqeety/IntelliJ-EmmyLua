source_id: external-codex-d31866e1a6027322
updated_at: 2026-04-11T22:23:59.1449946+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/emmy-annotation-autogen-field-insert-order.md

## Task group
Emmy/Lua annotation auto-generation enhancement in IntelliJ Lua plugin.

## Scope of applicability
Use when working on automatic generation of Emmy annotations (`---@type`, `---@param`, `---@return`, `---@field`) via inspections, quick fixes, intentions, or comment insertion utilities.

## Search keywords
`GeneratedEmmyAnnotationInspections.kt`, `LuaCommentUtil.kt`, `LuaAnnotationSupport.kt`, `emmylua-core.xml`, `GeneratedEmmyAnnotationInspectionTest.kt`, `CreateTypeAnnotationIntention`, `CreateParameterAnnotationIntention`, `CreateFunctionReturnAnnotationIntention`, `SuggestTypeMacro`, `---@field`, `---@type`, `self.a`, `LuaNameDef.guessType`, `LuaParamNameDef.guessType`, `LuaFuncBodyOwner.guessReturnType`, `WEAK_WARNING`.

## User preferences / product decisions
- User wants project enhanced to auto-generate Emmy annotations.
- Final accepted interaction model: editor weak inspection hints + Alt+Enter/quick fix, not automatic source edits while typing.
- `local` variables: only offer automatic `---@type` generation when the type system cannot infer a stable type (`UNKNOWN` / invalid / unstable result). Do **not** prompt for locals whose type is already stably inferred.
- `local` variable quick fix should insert an editable `---@type ` template, not hard-code inferred type.
- Function parameters and return values: offer generation when type can be stably inferred, writing concrete inferred type into `---@param` / `---@return`.
- Existing intentions should remain available and be backed by shared helper logic.
- No new settings switch; no `LuaSettings` change.
- Additional user request: support `self.a` / member expressions when type cannot be inferred. Prefer generating `---@field`; fall back to `---@type` if class definition cannot be located.
- Latest user correction / unfinished follow-up: `self.a` generating `---@field` currently inserts at the first class line. It should insert **after the last existing `---@field` line** in the class doc block.

## Implemented files / entry points from this rollout
- `src/main/java/com/tang/intellij/lua/codeInsight/inspection/GeneratedEmmyAnnotationInspections.kt`
  - New generated Emmy annotation inspections/quick fixes live here.
  - Behaviors implemented: unresolved local weak hint; stable param/return hint; unresolved `self.foo` hint.
- `src/main/java/com/tang/intellij/lua/comment/LuaCommentUtil.kt`
  - Shared comment/doc insertion utility changes.
  - Important bug already fixed during rollout: when a function had no comment block, parameter annotation insertion used `textOffset` and inserted into the function name; fix was to use the declaration/owner start offset instead.
- `src/main/java/com/tang/intellij/lua/codeInsight/annotation/LuaAnnotationSupport.kt`
  - Shared annotation generation support/helper changes.
- `src/main/resources/META-INF/emmylua-core.xml`
  - Inspection registration added here.
  - There was also a `.171` compatibility XML path involved; be careful editing compatibility resource paths precisely.
- `src/test/kotlin/com/tang/intellij/test/inspections/GeneratedEmmyAnnotationInspectionTest.kt`
  - Regression tests added for generated Emmy annotation behavior.

## Verified command
Run targeted tests with PowerShell:
`./gradlew.bat test --tests com.tang.intellij.test.inspections.GeneratedEmmyAnnotationInspectionTest`
This passed after implementation.

## Existing behavior after implementation
- `local value = unknownCall()` or similarly unstable/unknown type: weak hint appears; quick fix inserts editable `---@type`.
- `local value = createUser()` with stable inferred type: no automatic local prompt.
- Existing `---@type` on local: no prompt.
- Missing `---@param` with stable type: prompt/intentions can write concrete inferred type.
- Missing `---@return` with stable type: prompt/intentions can write concrete inferred type.
- `self.foo` with unknown/unstable inferred type: weak hint appears.
  - If class definition can be located, intended output is editable `---@field public foo ...` in class doc.
  - Else falls back to editable `---@type` near the current statement.

## Failure guardrails / known issue
- Do not regress the fixed function annotation offset bug: for functions without existing doc blocks, insert annotations before the full declaration, not at `textOffset` inside/near the function name.
- For `self.a` -> `---@field`, current user-reported bug: insertion position is wrong. It inserts at the class first line. Required behavior: insert after the last existing `---@field` line in the class comment block. If there are no existing fields, infer correct class doc tag ordering rather than blindly inserting at top.
- User noted existing unrelated staged/working-tree changes in debugger/settings panel area; avoid touching or reverting them.

## Source clues / likely fix direction
- The field insertion ordering likely belongs in shared annotation/comment helper logic, probably `LuaCommentUtil.kt` or `LuaAnnotationSupport.kt`, not in individual inspection UI code.
- Search for helper that creates/inserts `---@field public <name>` and adjust tag insertion order: scan the class Emmy doc comment block for existing `---@field` tags and insert after the last one.
- Add/adjust test in `GeneratedEmmyAnnotationInspectionTest.kt` for a class with multiple existing `---@field` lines, then apply `self.a` quick fix and assert new field appears immediately after the last field, not at top of class doc.
