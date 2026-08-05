# Raw Memories

Merged stage-1 raw memories (stable ascending source-id order):

## Source `a3d2a6c14fe744509fd80a3752e72ae5`
updated_at: 2026-08-05T10:30:53.6755360+08:00
cwd: <workspace>
rollout_summary_file: rollout_summaries/lua-constructor-alias-new-ctor.md

## Task group
Lua constructor alias mapping, bidirectional references, signature adaptation, and annotation-template stabilization in IntelliJ-EmmyLua.

## Scope of applicability
Use this when changing `Constructor names`, Lua member/reference resolution, constructor return inference, parameter hints/signature inspections, Find Usages, rename behavior, generic class constructors, or generated Emmy annotation templates.

## Search keywords
`constructorNamesString`, `new=ctor`, `getConstructorInitializerName`, `getConstructorNamesForInitializer`, `findConstructorTargets`, `asConstructorFunction`, `LuaConstructorReferencesSearcher`, `ReferencesSearch`, `SuggestTypeMacro(defaultType)`, `GeneratedEmmyAnnotationInspectionTest`

## User requirement and durable semantics
The constructor configuration supports mapping a synthetic entry point to a real initializer:

```text
new=ctor;create=init;alloc
```

For:

```lua
local a = class("A")
function a:ctor(cnt) end
local b = a.new(111)
```

Required behavior:
- `a.new` resolves/navigates to `a:ctor`.
- `111` maps to `cnt`, not implicit `self`.
- Constructor parameter hints, parameter info, count/type checks, overloads, and return inference reuse the initializer signature.
- The return type is the current class instance, including instantiated generic types such as `Box<number>`.
- Find Usages from `ctor` includes configured aliases such as `a.new` and `a["new"]`.
- A real `new` member takes priority over configured fallback mapping.
- Inherited initializers are supported while the returned instance remains the child type.
- Legacy entries such as `new` still only mean “returns a class instance” and remain permissive if no initializer exists.
- A mapped entry such as `new=ctor` with no `ctor` is reported as an unknown function.
- Renaming `ctor` must not rename the global configured alias `new`; the global mapping cannot safely be rewritten for one class.

## Architecture and entry points
- `src/main/java/com/tang/intellij/lua/project/LuaSettings.kt`
  - Parses both legacy constructor names and `alias=initializer` mappings while retaining the existing persisted `constructorNames` field.
  - `getConstructorInitializerName(name)` performs forward lookup.
  - `getConstructorNamesForInitializer(initializerName)` performs reverse lookup for Find Usages.
- `src/main/java/com/tang/intellij/lua/psi/LuaConstructorUtil.kt`
  - Central constructor-target lookup via `findConstructorTargets(...)`.
  - `LuaConstructorTarget` carries `instanceType: ITy`, not only the base `ITyClass`; this is necessary to preserve `Box<number>`.
  - For generic lookup, use the generic base to find the member but retain the original instantiated type for signature substitution and return type.
  - Handles tuple first-result types and inherited members.
- `src/main/java/com/tang/intellij/lua/ty/TyFunction.kt`
  - Constructor signature adaptation is centralized in `asConstructorFunction(...)`.
  - Convert a colon initializer into a point-call constructor view, omit implicit/explicit `self`, preserve parameters, varargs, type parameters, overloads, and flags, and replace return type with the current instance type.
- `src/main/java/com/tang/intellij/lua/ty/Expressions.kt`
  - Normal member inference runs first; constructor mapping is only a fallback when no real member type is found.
  - Generic substitution must compare the parameter type identity/display name to the generic name, not the parameter variable name.
  - Preserve `signature.tyParameters`, serialized function overloads, and flags during generic inference.
- `src/main/java/com/tang/intellij/lua/psi/LuaPsiResolveUtil.kt`
  - Normal member resolution has priority; mapped initializer resolution is fallback.
  - Pass the actual requested member string so bracket syntax can resolve correctly.
- `src/main/java/com/tang/intellij/lua/reference/LuaIndexReference.kt`
- `src/main/java/com/tang/intellij/lua/reference/LuaIndexBracketReference.kt`
  - Protect mapped aliases during rename: if the reference resolves to the configured initializer, leave alias text unchanged.
- `src/main/java/com/tang/intellij/lua/codeInsight/inspection/MatchMemberInspection.kt`
  - Suppress unknown-member diagnostics only for legacy constructor entries.
  - A mapped constructor whose initializer cannot be found must still produce `Unknown function 'new'.`
- `src/main/java/com/tang/intellij/lua/reference/LuaConstructorReferencesSearcher.kt`
  - Custom `QueryExecutor<PsiReference, ReferencesSearch.SearchParameters>` implementing reverse `ctor → new` search.
  - IntelliJ's default search scans the target word `ctor` and therefore cannot discover text occurrences named `new`, even when `a.new.resolve()` returns `ctor`.
  - Search every configured alias with `PsiSearchHelper.processElementsWithWord`, obtain enclosing `LuaIndexExpr` references, and accept only references where `reference.isReferenceTo(target)` is true.
  - Deduplicate by expression plus reference range, respect `effectiveSearchScope`, and stop when the consumer returns false.
- `src/main/resources/META-INF/emmylua-core.xml`
  - Registers `LuaConstructorReferencesSearcher` using `<referencesSearch implementation="..."/>`.
- Settings UI text was updated in `LuaSettingsPanel.kt` and `LuaBundle.properties` to describe `name[=initializer]` syntax.

## Failure guardrails
- Do not modify global `processArgs()` to special-case constructors. Wrap the initializer as a point-call function type at constructor-member inference instead; otherwise unrelated colon calls can regress.
- `LuaClassMethodDef` colon signatures already keep implicit `self` outside the declared parameter list and add it dynamically during call processing. Constructor adaptation must disable colon-call behavior rather than dropping the first normal parameter blindly.
- Explicit `self` forms must also map `a.new(111)` to `cnt`, not `self`.
- Never reduce an instantiated generic class to its base class during constructor lookup; lookup may use the base, but signature adaptation and return inference must retain the original `ITyGeneric` instance.
- Preserve overload signatures, function flags, and type parameters when rebuilding function signatures.
- Reverse usage search must verify `isReferenceTo(target)`; searching raw alias text alone incorrectly counts another class's `b.new` as a usage of `a:ctor`.
- A real class member named `new` must win over `new=ctor` fallback.
- Do not let rename refactoring turn alias calls into `a.initialize(...)`; constructor aliases are global configuration strings.

## Tests added/expanded
- `src/test/kotlin/com/tang/intellij/test/reference/LuaConstructorReferenceTest.kt`
  - Mapping parsing, forward navigation, real-member priority, legacy return type, generic instance preservation, inheritance, bracket references, parameter inlay names, explicit self omission, rename protection, and reverse ReferencesSearch with cross-class filtering.
- `src/test/kotlin/com/tang/intellij/test/inspections/LuaConstructorInspectionTest.kt`
  - Parameter type/count checks, generic parameter substitution, and overload preservation.
- `src/test/kotlin/com/tang/intellij/test/inspections/MatchMemberInspectionTest.kt`
  - Missing mapped initializer reports an error; missing legacy initializer remains compatible.

## Independent template failure and fix
A previously failing `GeneratedEmmyAnnotationInspectionTest` exposed platform-dependent template candidate ordering. The declared fallback `table` was supplied only as `TextExpression(defaultType)`, while `SuggestTypeMacro()` selected `string` from candidates on IDEA 2025.3.

Fix in `src/main/java/com/tang/intellij/lua/comment/LuaCommentUtil.kt`:
- Use `MacroCallNode(SuggestTypeMacro(defaultType))` for parameter, return, and field annotation templates.
- This makes unknown inferred types deterministically use the requested fallback (normally `table`) rather than candidate ordering.
- Updated `src/test/kotlin/com/tang/intellij/test/inspections/GeneratedEmmyAnnotationInspectionTest.kt` accordingly.

## Verified commands
```powershell
.\gradlew.bat compileKotlin --console=plain
.\gradlew.bat test --tests com.tang.intellij.test.reference.LuaConstructorReferenceTest --console=plain
.\gradlew.bat test --tests com.tang.intellij.test.reference.LuaConstructorReferenceTest --tests com.tang.intellij.test.inspections.LuaConstructorInspectionTest --tests com.tang.intellij.test.inspections.MatchMemberInspectionTest --console=plain
.\gradlew.bat test --tests com.tang.intellij.test.inspections.GeneratedEmmyAnnotationInspectionTest --console=plain
.\gradlew.bat test --console=plain
git diff --check
```

Final state reported: full test suite passed and `git diff --check` passed.

## Source clues
Conversation/workspace history: `a3d2a6c14fe744509fd80a3752e72ae5`, title `支持new引用及真实类初始化函数配置`, work performed on August 5, 2026.
