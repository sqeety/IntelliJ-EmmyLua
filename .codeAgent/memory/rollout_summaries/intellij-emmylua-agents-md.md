source_id: 96949baaf62545e6b513030355df7b1e
updated_at: 2026-06-24T16:33:52.4144069+08:00
cwd: <workspace>

Generated top-level AGENTS.md for IntelliJ-EmmyLua.
Key repo facts: Gradle/Kotlin IntelliJ plugin; source mostly under src/main/java with Kotlin files; generated PSI in gen; plugin descriptors in src/main/resources/META-INF.
Build truth: build.gradle.kts/CI override older README notes; default IDEA_VER=2025.3 Java target 21; CI uses JDK 17 IDEA_VER=231.
Validation commands documented: gradlew.bat compileKotlin, test, buildPlugin, buildPlugin -DIDEA_VER=231.
Preserved project convention: generated LuaDoc fields should be `---@field name type`, not `---@field public name type`, while parser supports legacy public form.
