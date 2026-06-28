source_id: external-codex-bce472daea65f9eb
updated_at: 2026-04-11T20:17:57.3633500+08:00
cwd: <workspace>

generic 分支实际为 `genericType`；对比 master 后已补测试并总结新增功能。
新增测试文件：`src/test/kotlin/com/tang/intellij/test/inspections/StrictGlobalNameInspectionTest.kt`、`LuaRequirePathInspectionTest.kt`。
测试覆盖：StrictGlobalName 未配置报错/配置后不报错；LuaRequirePath 路径不可解析报错/正确路径不报错。
运行验证：`./gradlew test --tests ...` 通过，涉及 MatchMember、StrictGlobalName、LuaRequirePath、CreateFieldFromParameterIntention、MoveFile。
后续用户关注：让 Lua 强语言特性更强、让 AI 更准确理解 Lua；优先建议类型收窄/nil-safety、模块导出模型、自动注解、项目语义索引、泛型约束/overload。
