source_id: external-codex-d31866e1a6027322
updated_at: 2026-04-11T22:23:59.1449946+08:00
cwd: <workspace>

Implemented Emmy annotation auto-generation inspections/quick fixes.
Key files: GeneratedEmmyAnnotationInspections.kt, LuaCommentUtil.kt, LuaAnnotationSupport.kt, emmylua-core.xml, GeneratedEmmyAnnotationInspectionTest.kt.
Policy: local/self fields only prompt when type inference is unstable/unknown; params/returns prompt when stable inferred type exists.
User correction: for `self.a` -> `---@field`, insertion is currently wrong: it inserts at first class line; should insert after the last existing `---@field` line.
