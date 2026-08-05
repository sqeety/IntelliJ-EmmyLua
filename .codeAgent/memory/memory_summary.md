# Workspace Memory Summary

## User Profile
- Memory is scoped to this workspace and represents prior local project context rather than confirmed-current truth.

## General Retrieval Tips
- Search Memory when a task may depend on prior architecture decisions, exact semantics, tests, commands, errors, or conventions.
- Start with `rollout_summaries/` for routing, then inspect the matching source section in `raw_memories.md` for details.
- Verify remembered behavior against current repository files before modifying code or reporting current status.

## Stable Index

### Lua constructor aliases and initializer mapping
- Summary: `rollout_summaries/lua-constructor-alias-new-ctor.md`
- Detailed evidence: `raw_memories.md`, source `a3d2a6c14fe744509fd80a3752e72ae5`
- Use for: configurable constructor syntax such as `new=ctor`, member inference, navigation, parameter hints, inspections, generic constructor returns, inheritance, rename behavior, bracket references, and reverse Find Usages.
- Search clues: `constructorNamesString`, `getConstructorInitializerName`, `getConstructorNamesForInitializer`, `findConstructorTargets`, `LuaConstructorTarget`, `asConstructorFunction`, `LuaConstructorReferencesSearcher`, `ReferencesSearch`, `Unknown function 'new'.`
- Key files: `src/main/java/com/tang/intellij/lua/project/LuaSettings.kt`, `src/main/java/com/tang/intellij/lua/psi/LuaConstructorUtil.kt`, `src/main/java/com/tang/intellij/lua/ty/TyFunction.kt`, `src/main/java/com/tang/intellij/lua/ty/Expressions.kt`, `src/main/java/com/tang/intellij/lua/psi/LuaPsiResolveUtil.kt`, `src/main/resources/META-INF/emmylua-core.xml`
- Important guardrails: real members take priority over configured aliases; retain instantiated generic types; do not globally special-case `processArgs()`; preserve overloads, flags, and type parameters; reverse search must verify `isReferenceTo(target)`; renaming an initializer must not rewrite a globally configured alias.

### Generated Emmy annotation fallback types
- Summary: `rollout_summaries/lua-constructor-alias-new-ctor.md`
- Detailed evidence: `raw_memories.md`, source `a3d2a6c14fe744509fd80a3752e72ae5`
- Use for: platform-dependent generated annotation template failures or unexpected `string` suggestions where `table` is the intended fallback.
- Search clues: `SuggestTypeMacro(defaultType)`, `MacroCallNode`, `LuaCommentUtil.kt`, `GeneratedEmmyAnnotationInspectionTest`
- Key behavior: pass `defaultType` directly to `SuggestTypeMacro` for deterministic parameter, return, and field annotation fallbacks.
