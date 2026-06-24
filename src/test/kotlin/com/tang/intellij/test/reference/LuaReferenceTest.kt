/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.test.reference

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.LuaTableField
import com.tang.intellij.test.LuaTestBase

class LuaReferenceTest : LuaTestBase() {

    fun `test table field literal references annotated class field`() {
        myFixture.configureByText("test.lua", """
            ---@class AAAA
            ---@field <caret>a number
            ---@field b number

            ---@type AAAA
            local test = {
                a = 1,
            }
            test.a = 1
        """.trimIndent())

        val target = myFixture.elementAtCaret as PsiNameIdentifierOwner
        val tableField = PsiTreeUtil.findChildrenOfType(myFixture.file, LuaTableField::class.java).first { it.name == "a" }
        assertSame(target, tableField.references.single().resolve())
        val references = ReferencesSearch.search(target).findAll().map { it.element.text }.toSet()
        val message = references.joinToString(", ")

        assertTrue(message, references.contains("a = 1"))
        assertTrue(message, references.contains("test.a"))
    }
}
