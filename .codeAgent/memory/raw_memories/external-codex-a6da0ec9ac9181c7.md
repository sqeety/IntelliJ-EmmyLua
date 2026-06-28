source_id: external-codex-a6da0ec9ac9181c7
updated_at: 2026-04-11T21:37:41.2184331+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/java-settings-editors-to-kotlin.md

## Task group
Java-to-Kotlin migration for IntelliJ plugin settings/debugger editor UI classes.

## Scope of applicability
Workspace project under `src/main/java/com/tang/intellij/lua/...`; specifically classes backed by IntelliJ UI Designer `.form` files. Applies when continuing migration of settings panels/editors from Java to Kotlin.

## Search keywords
`LuaSettingsPanel.java`, `CompletionSettingsPanel.java`, `LuaMobSettingsEditor.java`, `EmmyDebugSettingsPanel.java`, `LuaAppSettingsEditor.java`, `.form binding`, `UI Designer`, `compileKotlin`, `LuaFileType.INSTANCE`, nullable Swing text field.

## Completed migration
The following Java files were replaced with same-package/same-class Kotlin files, preserving existing `.form` bindings and behavior:
- `src/main/java/com/tang/intellij/lua/project/LuaSettingsPanel.kt`
- `src/main/java/com/tang/intellij/lua/project/CompletionSettingsPanel.kt`
- `src/main/java/com/tang/intellij/lua/debugger/remote/LuaMobSettingsEditor.kt`
- `src/main/java/com/tang/intellij/lua/debugger/emmy/EmmyDebugSettingsPanel.kt`
- `src/main/java/com/tang/intellij/lua/debugger/app/LuaAppSettingsEditor.kt`

Corresponding `.java` files were deleted.

## Validation
Command verified after migration:
- `./gradlew compileKotlin --console=plain`

Result: passed. Only pre-existing repository warnings remained; no migration-related compile errors.

## Reusable migration steps
1. Inspect target Java class and same-directory Kotlin style before editing.
2. Check matching `.form` files before converting; preserve exact class names and bound field names for IntelliJ UI Designer compatibility.
3. Convert conservatively: same package, same public/internal API shape where possible, same constructor/init behavior, no UI structure changes.
4. Delete Java source only after same-named Kotlin class is in place.
5. Run `./gradlew compileKotlin --console=plain` and fix Kotlin interop issues.

## Failure guardrails / known fixes
- Do not break `.form` binding: keep bound Swing component fields with names expected by the `.form` file.
- Kotlin nullable strings cannot be assigned directly into Swing text properties without handling null; coerce/default as needed.
- For file type usage, use `LuaFileType.INSTANCE` rather than treating `LuaFileType` as the value directly.
- Watch Java getter/setter-to-Kotlin property mapping in IntelliJ APIs and settings beans.

## Source clues / workspace state
- User requested converting these 5 files: `LuaSettingsPanel.java`, `CompletionSettingsPanel.java`, `LuaMobSettingsEditor.java`, `EmmyDebugSettingsPanel.java`, `LuaAppSettingsEditor.java`.
- Worktree was initially clean except a pre-existing unrelated modified file: `src/test/kotlin/com/tang/intellij/test/generic/GenericTest.kt`; it was not touched.
