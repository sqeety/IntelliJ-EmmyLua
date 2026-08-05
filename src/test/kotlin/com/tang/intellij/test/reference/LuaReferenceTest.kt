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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import com.tang.intellij.lua.editor.Hints.countLuaUsageReferences
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaTableField
import com.tang.intellij.lua.usages.isLuaUsageReference
import com.tang.intellij.lua.usages.processResolvedUsage
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

    fun `test class method usages exclude non Lua and unresolved dynamic references`() {
        val notesFile = myFixture.addFileToProject("notes.md", "RefreshUI")
        val notesElement = notesFile.findElementAt(0) ?: notesFile
        myFixture.configureByText("test.lua", """
            ---@class View
            local view = {}

            function view:<caret>RefreshUI()
                self:RefreshUI()
            end
        """.trimIndent())

        val target = PsiTreeUtil.findChildOfType(myFixture.file, LuaClassMethodDef::class.java)!!
        val callOffset = myFixture.file.text.lastIndexOf("RefreshUI")
        val callElement = myFixture.file.findElementAt(callOffset)!!
        val dynamicReference = object : PsiReferenceBase<PsiElement>(
            notesElement,
            TextRange(0, notesElement.textLength)
        ) {
            override fun resolve(): PsiElement? = null
            override fun getVariants(): Array<Any> = emptyArray()
        }
        val markdownReference = object : PsiReferenceBase<PsiElement>(
            notesElement,
            TextRange(0, notesElement.textLength)
        ) {
            override fun resolve(): PsiElement = target
            override fun getVariants(): Array<Any> = emptyArray()
        }
        val resolvedReference = object : PsiReferenceBase<PsiElement>(
            callElement,
            TextRange(0, callElement.textLength)
        ) {
            override fun resolve(): PsiElement = target
            override fun getVariants(): Array<Any> = emptyArray()
        }
        val dynamicUsage = UsageInfo(dynamicReference)
        val markdownUsage = UsageInfo(markdownReference)
        val resolvedUsage = UsageInfo(resolvedReference)
        val usages = mutableListOf<UsageInfo>()
        val processor = Processor<UsageInfo> { usage ->
            usages.add(usage)
            true
        }

        assertTrue(dynamicUsage.isDynamicUsage)
        assertFalse(markdownUsage.isDynamicUsage)
        assertFalse(resolvedUsage.isDynamicUsage)
        assertFalse(isLuaUsageReference(markdownReference))
        assertTrue(isLuaUsageReference(resolvedReference))
        assertEquals(1, countLuaUsageReferences(listOf(markdownReference, resolvedReference)))
        assertTrue(processResolvedUsage(dynamicUsage, processor))
        assertTrue(processResolvedUsage(markdownUsage, processor))
        assertTrue(processResolvedUsage(resolvedUsage, processor))
        assertEquals(1, usages.size)
        assertSame(callElement, usages.single().element)
    }
}
