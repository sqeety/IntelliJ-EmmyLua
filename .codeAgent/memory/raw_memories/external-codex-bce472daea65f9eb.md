source_id: external-codex-bce472daea65f9eb
updated_at: 2026-04-11T20:17:57.3633500+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/genericType-tests-ai-lua-roadmap.md

## Task group
- IntelliJ/EmmyLua-like plugin development; branch comparison and test coverage for Lua typing/inspection features.

## Scope of applicability
- Applies when working in this repository on the branch the user called `generic`; actual branch found was `genericType`.
- Relevant to comparing `genericType` vs `master`, adding tests for newly added inspections, and planning stronger Lua language/AI understanding features.

## Search keywords
- `genericType`, `master`, `StrictGlobalNameInspectionTest.kt`, `LuaRequirePathInspectionTest.kt`, `MatchMemberInspectionTest`, `CreateFieldFromParameterIntentionTest`, `MoveFileTest`
- `StrictGlobalName`, `LuaRequirePathInspection`, `require path`, `MatchMemberInspection`, `DuplicateMethodDeclaration`, `EmptyBody`, `Global name can be local`
- `generic`, `@refer`, `@partial`, `@see`, `partial class`, `alias/class`, `require-like`, `source root`, `Emmy attach debugger provider`

## Durable findings from branch analysis
- User asked to analyze current `generic` branch vs `master`; repo did not have `generic`, corresponding branch was `genericType`.
- Main added capability areas in `genericType` vs `master`:
  1. Generic/type-system enhancements: generic class/function inference fixes, support for forms like `A<T>:T`, multiple inheritance, partial class extension fields, alias/class coexistence member inference, and fixes for inference loops/errors. Core areas mentioned: `ty/`, `comment/psi/`, `stubs/`, grammar definitions.
  2. LuaDoc annotation expansion: added `@refer`, `@partial`, stronger `@see`/class-name reference parsing, with parser/lexer/stub/reference support.
  3. New/enhanced inspections/lints: `MatchMemberInspection`, `LuaRequirePathInspection`, `StrictGlobalName`, `DuplicateMethodDeclaration`, `EmptyBody` warning behavior, and `Global name can be local` default enabled.
  4. `require` / source-root configuration: require-like function names, path separators/rules, additional source roots, handling project roots under Lua path subdirectories; improves module resolution/navigation/completion/inspection consistency.
  5. Debugging: Emmy attach debugger provider integration, attach-process/file utilities, mobdebug and variable hover evaluation compatibility fixes.
  6. Editor/navigation: inlay hints provider, variable-name suggestion macro, refresh Lua index action, improvements to line markers/usages/function navigation/table field docs/type match priority.
  7. Refactoring support: `CreateFieldFromParameterIntention`, moving files and reference updates.

## Tests added during rollout
- Added inspection tests:
  - `src/test/kotlin/com/tang/intellij/test/inspections/StrictGlobalNameInspectionTest.kt`
  - `src/test/kotlin/com/tang/intellij/test/inspections/LuaRequirePathInspectionTest.kt`
- Verified test groups included:
  - `MatchMemberInspectionTest`
  - `StrictGlobalNameInspectionTest`
  - `LuaRequirePathInspectionTest`
  - `CreateFieldFromParameterIntentionTest`
  - `MoveFileTest`
- Command pattern used: `./gradlew test --tests ...`; target tests passed after fixes.

## Failure guardrails / cause -> fix chains
- Branch-name guardrail: if user says `generic`, check actual repo branches; likely `genericType`.
- Test helper guardrail: inspection test helper requires a project file with a caret marker/opened file; missing caret caused failures. Fix by adding the caret marker to the appropriate test file.
- `StrictGlobalName` test guardrail: a test case using `strict-global-names.txt` could not assert through real filesystem path behavior in the sandbox. Fix was to test configuration behavior directly: unconfigured => reports diagnostic; configured => no diagnostic.
- `LuaRequirePathInspection` guardrail: temporary VFS in tests is case-sensitive; a wrong-case require path may fail during resolution as “file not found” before reaching path-case comparison logic. Stable test coverage should verify: valid resolvable path => no error; unresolvable path => diagnostic. Avoid relying on wrong-case comparison branch in this sandbox unless filesystem behavior is controlled.
- `LuaRequirePathInspection` NPE: source-root lookup initially caused null pointer due to test setup; avoid fragile source-root assumptions in test scaffolding.

## User preference evidence
- User writes in Chinese and asks for direct repo analysis, test additions, and a final feature list/explanation.
- User is interested in strengthening Lua language features and improving AI understanding of Lua code in the plugin.

## Reusable roadmap ideas discussed for future work
- Highest-priority features for stronger Lua typing and AI understanding:
  1. Type narrowing and nil-safety: narrow after `type(x) == "string"`, `x ~= nil`, `assert(x)`, support custom type-guard functions, detect nullable access/unhandled nullable returns.
  2. Explicit module export model: recognize `return table`, class-like objects, returned functions, dynamic assignment exports, and require alias propagation.
  3. Automatic Emmy/LuaDoc annotation generation: infer `---@param`/`---@return` from function bodies, `---@class`/`---@field` from table constructors, and infer params from call sites.
  4. Project-level semantic index for AI/tools: module exports, class inheritance/members/overrides, function params/returns/side effects, global definitions.
  5. Better generic constraints and overload resolution: `T : Base`, string-key constraints, multiple/intersection constraints, default generic args, overload selection by literal args/count, colon vs dot calls, vararg, return type dependent on params.
- Other potentially valuable features: structure/table-shape types, readonly/optional fields, string literal unions, enum-like constants, semantic tags for factory/constructor/pure/coroutine/event/callback functions, data-flow summaries, side-effect/immutability checks, API contracts, call/type-flow graphs, framework awareness, and project type-health dashboard.
