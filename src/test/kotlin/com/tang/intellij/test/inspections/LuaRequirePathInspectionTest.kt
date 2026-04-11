package com.tang.intellij.test.inspections

import com.tang.intellij.lua.codeInsight.inspection.LuaRequirePathInspection
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.project.LuaSourceRootManager
import com.tang.intellij.test.LuaTestBase
import com.tang.intellij.test.fileTreeFromText

class LuaRequirePathInspectionTest : LuaTestBase() {

    fun `test require path outside source root errors`() {
        val previousSeparator = LuaSettings.instance.requirePathSeparator
        LuaSettings.instance.requirePathSeparator = LuaSettings.REQUIRE_PATH_SEPARATOR_DOT
        try {
            val testProject = fileTreeFromText(
                """
                --- src/Foo/Bar.lua
                return {}
                --- src/main.lua
                local value = require(<error descr="Path 'foo.bar' not in SourceRoot.">"foo--[[caret]].bar"</error>)
                """.trimIndent()
            ).createAndOpenFileWithCaretMarker()

            LuaSourceRootManager.getInstance(project).appendRoot(testProject.psiDirectory("src").virtualFile)
            myFixture.enableInspections(LuaRequirePathInspection())
            myFixture.checkHighlighting(true, false, false)
        } finally {
            LuaSettings.instance.requirePathSeparator = previousSeparator
        }
    }

    fun `test require path with matching case passes`() {
        val previousSeparator = LuaSettings.instance.requirePathSeparator
        LuaSettings.instance.requirePathSeparator = LuaSettings.REQUIRE_PATH_SEPARATOR_DOT
        try {
            val testProject = fileTreeFromText(
                """
                --- src/Foo/Bar.lua
                return {}
                --- src/main.lua
                local value = require(--[[caret]]"Foo.Bar")
                """.trimIndent()
            ).createAndOpenFileWithCaretMarker()

            LuaSourceRootManager.getInstance(project).appendRoot(testProject.psiDirectory("src").virtualFile)
            myFixture.enableInspections(LuaRequirePathInspection())
            myFixture.checkHighlighting(true, false, false)
        } finally {
            LuaSettings.instance.requirePathSeparator = previousSeparator
        }
    }
}
