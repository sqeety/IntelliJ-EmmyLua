source_id: e8155d8c045c4d0fb4ca0e99e10e5916
updated_at: 2026-06-24T16:16:32.6391758+08:00
cwd: <workspace>

IntelliJ-EmmyLua: Lua @field generation should not include `public`.
Changed generation in `LuaCommentUtil.kt`, removed `public/protected` completion in `LuaDocCompletionContributor.kt`, updated tutorial template and inspection tests.
Keep parser compatibility for legacy `---@field public ...` annotations.
Verified with `./gradlew.bat test --tests com.tang.intellij.test.inspections.GeneratedEmmyAnnotationInspectionTest`.
