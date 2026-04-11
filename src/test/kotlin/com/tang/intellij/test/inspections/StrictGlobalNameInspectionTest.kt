package com.tang.intellij.test.inspections

import com.tang.intellij.lua.codeInsight.inspection.StrictGlobalName
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.test.LuaTestBase
import com.tang.intellij.test.fileTreeFromText

class StrictGlobalNameInspectionTest : LuaTestBase() {

    fun `test unknown global errors when not configured`() {
        fileTreeFromText(
            """
            --- main.lua
            <error descr="Global name \"game\" not in strict names">ga--[[caret]]me</error> = {}
            """.trimIndent()
        ).createAndOpenFileWithCaretMarker()

        myFixture.enableInspections(StrictGlobalName())
        myFixture.checkHighlighting(true, false, false)
    }

    fun `test configured strict global name suppresses error`() {
        val previousStrictNames = LuaSettings.instance.strictGlobalNames
        LuaSettings.instance.strictGlobalNames = arrayOf("game")
        try {
            fileTreeFromText(
                """
                --- main.lua
                ga--[[caret]]me = {}
                """.trimIndent()
            ).createAndOpenFileWithCaretMarker()

            myFixture.enableInspections(StrictGlobalName())
            myFixture.checkHighlighting(true, false, false)
        } finally {
            LuaSettings.instance.strictGlobalNames = previousStrictNames
        }
    }
}
