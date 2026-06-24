source_id: external-codex-2437e73aa4a9a488
updated_at: 2026-04-08T16:01:55.7077035+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/create-field-intention-preview-empty.md

## Task group
IntelliJ plugin intention action preview safety / Lua plugin code insight.

## Scope of applicability
Applies to `com.tang.intellij.lua.codeInsight.intention.CreateFieldFromParameterIntention` and similar IntelliJ `IntentionAction`s whose real `invoke()` path performs side effects such as `Application.invokeLater`, popup input, or live templates.

## Search keywords
`CreateFieldFromParameterIntention`, `Create field for parameter`, `Side effect not allowed: INVOKE_LATER`, `SideEffectGuard`, `IntentionAction.generatePreview`, `IntentionPreviewInfo.EMPTY`, `Application.invokeLater`, intention preview.

## Failure symptom → cause → fix
- Symptom: IntelliJ reported `Side effect occurred on invoking the intention 'Create field for parameter' ... on a copy of the file` with stack phrase `java.lang.RuntimeException: Side effect not allowed: INVOKE_LATER`.
- Stack pointed to `com.tang.intellij.lua.codeInsight.intention.CreateFieldFromParameterIntention.invoke(CreateFieldFromParameterIntention.kt:72)` during `IntentionAction.generatePreview` / `IntentionPreviewComputable`.
- Cause: IntelliJ generates intention previews in a no-side-effects context; this intention’s real `invoke()` unconditionally called `Application.invokeLater`, which is forbidden in preview on the copied file.
- Fix: In `src/main/java/com/tang/intellij/lua/codeInsight/intention/CreateFieldFromParameterIntention.kt`, explicitly override `generatePreview()` and return `IntentionPreviewInfo.EMPTY`, because this intention depends on UI/live-template behavior and should not execute during preview.

## Reusable steps
1. For intentions that cannot safely preview, add/keep an override like `generatePreview(...) = IntentionPreviewInfo.EMPTY` rather than letting IntelliJ call `invoke()` for preview.
2. Ensure imports include IntelliJ preview type as needed (`IntentionPreviewInfo`).
3. Add/maintain a lightweight regression test confirming this intention returns `EMPTY` for preview behavior. Existing test file noted: `src/test/kotlin/com/tang/intellij/test/editor/CreateFieldFromParameterIntentionTest.kt`.
4. Verify main code with PowerShell command: `.\gradlew.bat compileKotlin`.

## Failure guardrails
- Do not fix this by allowing `invokeLater` in preview or by moving the side effect elsewhere in `invoke()`; preview must avoid real UI/later-invocation side effects.
- This intention’s behavior is unsuitable for diff preview because it depends on popup input/live template flow.
- A targeted test run was reported blocked by pre-existing repository test dependency/platform test base resolution issues; distinguish that from this specific code change. Main `compileKotlin` passed.

## Source clues
Conversation dated 2026-04-08 in workspace `<workspace>`. Final reported modified files:
- `src/main/java/com/tang/intellij/lua/codeInsight/intention/CreateFieldFromParameterIntention.kt`
- `src/test/kotlin/com/tang/intellij/test/editor/CreateFieldFromParameterIntentionTest.kt`
