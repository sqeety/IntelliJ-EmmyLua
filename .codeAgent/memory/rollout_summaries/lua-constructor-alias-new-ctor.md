source_id: a3d2a6c14fe744509fd80a3752e72ae5
updated_at: 2026-08-05T10:30:53.6755360+08:00
cwd: <workspace>

Implemented configurable Lua constructor aliases such as `new=ctor` across type inference, navigation, parameter hints, inspections, generics, and reverse Find Usages.
Constructor aliases preserve implicit-self semantics, instantiated generic types, overloads, inheritance, and real-member priority.
Added rename protection so global aliases such as `new` are not renamed with a class initializer.
Added `ReferencesSearch` integration for `ctor → new`, including bracket calls and cross-class filtering.
Also stabilized generated annotation templates by explicitly passing `defaultType` to `SuggestTypeMacro`.
Full Gradle test suite and `git diff --check` passed.
