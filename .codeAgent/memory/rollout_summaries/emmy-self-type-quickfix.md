source_id: 33685c736b9b4375a7883c3012fae983
updated_at: 2026-06-27T17:21:37.9110883+08:00
cwd: <workspace>

Added a second inspection quick-fix for missing self field type hints in LuaDoc.
Scope: GeneratedEmmyAnnotationInspections.kt, LuaCommentUtil.kt, SuggestTypeMacro.kt, GeneratedEmmyAnnotationInspectionTest.kt.
New action name: `Generate type annotation`.
Behavior: inserts `---@type ...` before the assignment statement, while existing `Generate field annotation` still generates class `---@field`.
Type defaulting was stabilized by letting `SuggestTypeMacro(defaultType)` surface the provided type as the first lookup/default value.
Verified with `./gradlew.bat test --tests com.tang.intellij.test.inspections.GeneratedEmmyAnnotationInspectionTest` and `./gradlew.bat compileKotlin`.
