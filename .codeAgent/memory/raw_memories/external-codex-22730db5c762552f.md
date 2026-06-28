source_id: external-codex-22730db5c762552f
updated_at: 2026-04-11T21:33:06.2306268+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/generic-class-test.md

## Task group
Testing generic class support in Kotlin test suite for Lua/Emmy annotations.

## Scope of applicability
Use when modifying or extending generic type inference/completion tests, especially class generics and member field type substitution.

## Search keywords
`GenericTest.kt`, `com.tang.intellij.test.generic.GenericTest`, `---@class Box<T>`, `---@field value T`, `---@type Box<Emmy>`, `box.value`, `sayHello`, generic class, member type substitution, `genericNames`.

## Preference evidence
User requested in Chinese: “补上generic class的测试用例” (add generic class test cases). Existing assistant intentionally kept changes localized to existing generic test layout.

## Reusable details
- Generic-related tests are centralized in `src/test/kotlin/com/tang/intellij/test/generic/GenericTest.kt`.
- A minimal high-signal generic class test was added around line 109.
- Covered scenario:
  - Declare generic class: `---@class Box<T>`
  - Declare generic field: `---@field value T`
  - Instantiate with concrete type: `---@type Box<Emmy>`
  - Assert completion/member inference on `box.value` includes `Emmy` member `sayHello`.
- This specifically guards the chain where class generic metadata (`genericNames`) is stored and member access substitutes `T` with the concrete type `Emmy` for `Box<Emmy>`.

## Verified command
```powershell
./gradlew.bat test --tests com.tang.intellij.test.generic.GenericTest
```
Result: passed.

## Failure guardrails
If this test fails in future, inspect generic class member type substitution during field/member access, not just generic metadata parsing. The intended assertion is that `Box<Emmy>.value` resolves as `Emmy`, enabling completion of `sayHello`.
