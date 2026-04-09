package com.tang.intellij.test.editor

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.tang.intellij.lua.codeInsight.intention.CreateFieldFromParameterIntention
import com.tang.intellij.test.LuaTestBase
import com.tang.intellij.test.fileTreeFromText

class CreateFieldFromParameterIntentionTest : LuaTestBase() {

    fun `test create field intention disables preview`() {
        fileTreeFromText(
            """
            --- test.lua
            ---@class Foo
            local Foo = {}
            
            function Foo:bar(na--[[caret]]me)
            end
            """.trimIndent()
        ).createAndOpenFileWithCaretMarker()

        val intention = CreateFieldFromParameterIntention()
        assertTrue(intention.isAvailable(project, myFixture.editor, myFixture.file))
        assertSame(IntentionPreviewInfo.EMPTY, intention.generatePreview(project, myFixture.editor, myFixture.file))
    }
}
