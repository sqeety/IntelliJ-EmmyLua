# AGENTS.md

## Scope

These instructions apply to the entire repository unless a deeper `AGENTS.md` overrides them.

## Project Overview

This repository is the EmmyLua IntelliJ IDEA plugin. Most production code is Kotlin placed under `src/main/java`, with generated Java PSI sources under `gen`. The Gradle root project is named `EmmyLua`.

The plugin provides Lua language support: parsing, PSI/stubs, indexing, type inference, code completion, inspections, refactoring, formatting, documentation, debugger resources, and bundled Lua standard library annotations.

## Important Paths

- `build.gradle.kts`: Gradle build, IntelliJ Platform setup, supported IDE build matrix, debugger download/install tasks, and plugin packaging.
- `gradle.properties`: project version and Gradle JVM settings.
- `src/main/java/com/tang/intellij/lua`: main plugin implementation. Despite the path name, Kotlin files are common here.
- `src/main/compat`: compatibility shims for different IntelliJ Platform versions.
- `gen`: checked-in generated PSI/parser support sources included in the main source set. Do not mass-edit generated files unless the generator/source grammar workflow is known.
- `src/main/resources/META-INF/plugin.xml`: top-level plugin descriptor. It includes `emmylua-core.xml` and optionally `emmylua-project.xml`.
- `src/main/resources/std`: bundled Lua standard library files for Lua 5.0 through 5.4.
- `src/main/resources/postfixTemplates`, `src/main/resources/fileTemplates`, `src/main/resources/liveTemplates`: user-facing templates.
- `src/test/kotlin/com/tang/intellij/test`: IntelliJ Platform test suite.
- `src/test/resources`: Lua fixtures used by tests.
- `.github/workflows`: CI build and publish workflows.
- `build`, `temp`, `.gradle`, `.intellijPlatform`, and IDE caches are generated/local artifacts and should not be committed.

## Environment

Use the Gradle wrapper. On Windows prefer `./gradlew.bat`; on Unix-like shells prefer `./gradlew`.

The current Gradle script defaults to the first entry in `buildDataList`, currently `IDEA_VER=2025.3`, with Java target 21. Older supported targets can be selected with `-DIDEA_VER=241` or `-DIDEA_VER=231`. CI currently builds and publishes with JDK 17 and `IDEA_VER=231`.

The README still contains older build notes for JDK 11 and `IDEA_VER=203`; treat `build.gradle.kts` and CI workflow files as the current source of truth when they differ.

## Build And Test Commands

Use the smallest command that validates the change.

- Compile Kotlin only: `./gradlew.bat compileKotlin`.
- Run all tests: `./gradlew.bat test`.
- Run one test class: `./gradlew.bat test --tests com.tang.intellij.test.inspections.GeneratedEmmyAnnotationInspectionTest`.
- Build the plugin for the default target: `./gradlew.bat buildPlugin`.
- Build for a specific target: `./gradlew.bat buildPlugin -DIDEA_VER=231`.
- Publish is CI/release oriented and requires `IDEA_PUBLISH_TOKEN`: `./gradlew.bat publishPlugin`.

`buildPlugin`, `processResources`, and `patchPluginXml` depend on debugger resource installation. The build may download EmmyLua debugger archives into `temp`. Do not commit downloaded binaries or local build outputs.

## Coding Guidelines

Preserve the existing Kotlin style: concise classes/functions, IntelliJ Platform idioms, and project-local helper APIs. Avoid broad rewrites when a targeted fix is sufficient.

Use Kotlin for new plugin logic unless the surrounding code is Java or generated code. Keep package names under `com.tang.intellij.lua` or the matching existing package.

When changing IntelliJ extension behavior, update the relevant descriptor in `src/main/resources/META-INF` and add or update tests where possible.

When changing completion, inspections, intentions, refactoring, formatter, PSI/stub, or type inference behavior, look for an existing focused test under `src/test/kotlin/com/tang/intellij/test` before adding a new test class.

For tests that use inline Lua snippets, follow the existing fixture style in `LuaTestBase`, `LuaInspectionsTestBase`, and completion test bases. Store larger fixtures in `src/test/resources` next to similar cases.

Do not overwrite user changes in a dirty worktree. Check `git status --short` before editing if the task is non-trivial.

## IntelliJ Platform Notes

Plugin registrations are split across `plugin.xml`, `emmylua-core.xml`, `emmylua-project.xml`, and `experiment.xml`. Keep registrations near related existing entries and preserve optional dependency behavior.

`plugin.xml` has static metadata, but Gradle's IntelliJ Platform plugin patches version/build compatibility during packaging. Do not rely only on the XML `idea-version` when reasoning about packaged compatibility.

The build uses `bunch` to switch compatibility files for some targets. Avoid editing compatibility variants blindly; inspect `src/main/compat`, `.bunch`, and the relevant target version first.

## LuaDoc And EmmyLua Conventions

Newly generated LuaDoc field annotations should use `---@field name type`, not `---@field public name type`. Keep parser compatibility for legacy annotations such as `---@field public name type`.

When touching LuaDoc annotation generation or completion, check related files such as `LuaCommentUtil.kt`, `LuaDocCompletionContributor.kt`, tutorial/file templates, and `GeneratedEmmyAnnotationInspectionTest.kt`.

## Generated And Binary Resources

Treat `gen` as generated but checked-in source. Keep edits minimal and document why a generated file must be touched if no generator workflow is available.

Debugger resources under `src/main/resources/debugger` may be populated by Gradle tasks from archives in `temp`. Avoid committing local debugger downloads unless the project explicitly expects the resource update.

## Documentation

Keep README-facing instructions aligned with current Gradle behavior when updating docs. If README and build scripts disagree, verify against `build.gradle.kts` and `.github/workflows` before changing documentation.
