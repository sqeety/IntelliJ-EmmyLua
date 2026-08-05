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

import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaNameDef
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyGeneric
import com.tang.intellij.lua.ty.TyUnion
import com.tang.intellij.test.LuaTestBase

class LuaConstructorReferenceTest : LuaTestBase() {
    private lateinit var originalConstructorNames: Array<String>

    override fun setUp() {
        super.setUp()
        originalConstructorNames = LuaSettings.instance.constructorNames.copyOf()
        LuaSettings.instance.constructorNamesString = "new=ctor"
    }

    override fun tearDown() {
        try {
            LuaSettings.instance.constructorNames = originalConstructorNames
        } finally {
            super.tearDown()
        }
    }

    fun `test constructor configuration supports initializer mappings and legacy names`() {
        val settings = LuaSettings.instance
        settings.constructorNamesString = " new ; create = init ; ; build= "

        assertEquals("new;create=init;build", settings.constructorNamesString)
        assertTrue(LuaSettings.isConstructorName("new"))
        assertTrue(LuaSettings.isConstructorName("create"))
        assertTrue(LuaSettings.isConstructorName("build"))
        assertNull(LuaSettings.getConstructorInitializerName("new"))
        assertEquals("init", LuaSettings.getConstructorInitializerName("create"))
        assertNull(LuaSettings.getConstructorInitializerName("build"))
    }

    fun `test initializer references include constructor aliases`() {
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            function a:<caret>ctor(cnt)
            end

            a.new(111)
            a["new"](222)

            ---@class B
            local b = {}

            function b:ctor(cnt)
            end

            b.new(333)
        """.trimIndent())

        val target = PsiTreeUtil.findChildrenOfType(myFixture.file, LuaClassMethodDef::class.java)
            .first { it.textOffset <= myFixture.caretOffset && myFixture.caretOffset <= it.textRange.endOffset }
        val references = ReferencesSearch.search(target).findAll()
        val referencedExpressions = references.map { it.element.text }.toSet()
        val message = referencedExpressions.joinToString(", ")

        assertEquals(message, 2, references.size)
        assertTrue(message, referencedExpressions.contains("a.new"))
        assertTrue(message, referencedExpressions.contains("a[\"new\"]"))
        assertFalse(message, referencedExpressions.contains("b.new"))
    }

    fun `test constructor name resolves to configured initializer`() {
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            ---@param cnt number
            function a:ctor(cnt)
            end

            local b = a.<caret>new(111)
        """.trimIndent())

        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
        assertInstanceOf(target, LuaClassMethodDef::class.java)
        assertEquals("ctor", (target as LuaClassMethodDef).name)
    }

    fun `test real constructor member has priority over configured initializer`() {
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            function a.new(value)
            end

            function a:ctor(cnt)
            end

            a.<caret>new(111)
        """.trimIndent())

        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
        assertInstanceOf(target, LuaClassMethodDef::class.java)
        assertEquals("new", (target as LuaClassMethodDef).name)
    }

    fun `test constructor call returns class type`() {
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            ---@param cnt number
            function a:ctor(cnt)
            end

            local b = a.new(111)
        """.trimIndent())

        val result = PsiTreeUtil.findChildrenOfType(myFixture.file, LuaNameDef::class.java)
            .first { it.name == "b" }
            .guessType(SearchContext.get(project))
        assertEquals("A", TyUnion.getPerfectClass(result)?.className)
    }

    fun `test legacy constructor name still returns class type`() {
        LuaSettings.instance.constructorNamesString = "new"
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            local b = a.new(111)
        """.trimIndent())

        val result = PsiTreeUtil.findChildrenOfType(myFixture.file, LuaNameDef::class.java)
            .first { it.name == "b" }
            .guessType(SearchContext.get(project))
        assertEquals("A", TyUnion.getPerfectClass(result)?.className)
    }

    fun `test generic constructor preserves instantiated class type`() {
        myFixture.configureByText("test.lua", """
            ---@class Box<T>
            local Box = {}

            ---@param value T
            function Box:ctor(value)
            end

            ---@type Box<number>
            local box = Box
            local result = box.new(1)
        """.trimIndent())

        val result = PsiTreeUtil.findChildrenOfType(myFixture.file, LuaNameDef::class.java)
            .first { it.name == "result" }
            .guessType(SearchContext.get(project))
        assertEquals("Box&lt;number&gt;", TyUnion.find(result, ITyGeneric::class.java)?.displayName)
    }

    fun `test inherited initializer resolves and returns child type`() {
        myFixture.configureByText("test.lua", """
            ---@class Base
            local Base = {}

            ---@param cnt number
            function Base:ctor(cnt)
            end

            ---@class Child: Base
            local Child = {}

            local child = Child.<caret>new(111)
        """.trimIndent())

        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
        assertInstanceOf(target, LuaClassMethodDef::class.java)
        assertEquals("ctor", (target as LuaClassMethodDef).name)

        val result = PsiTreeUtil.findChildrenOfType(myFixture.file, LuaNameDef::class.java)
            .first { it.name == "child" }
            .guessType(SearchContext.get(project))
        assertEquals("Child", TyUnion.getPerfectClass(result)?.className)
    }

    fun `test bracket constructor reference resolves to configured initializer`() {
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            function a:ctor(cnt)
            end

            local b = a["ne<caret>w"](111)
        """.trimIndent())

        val target = myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
        assertInstanceOf(target, LuaClassMethodDef::class.java)
        assertEquals("ctor", (target as LuaClassMethodDef).name)
    }

    fun `test constructor argument uses initializer parameter name`() {
        myFixture.configureByText("test.lua", """
            local a = class("A")

            ---@param cnt number
            function a:ctor(cnt)
            end

            local b = a.new(111)
        """.trimIndent())

        myFixture.doHighlighting()
        myFixture.checkResultWithInlays("""
            local a = class("A")

            ---@param cnt number
            function a:ctor(cnt)
            end

            local b = a.new(<hint text="cnt : "/>111)
        """.trimIndent())
    }

    fun `test explicit self parameter is omitted from constructor arguments`() {
        myFixture.configureByText("test.lua", """
            local a = class("A")

            ---@param cnt number
            function a:ctor(self, cnt)
            end

            local b = a.new(111)
        """.trimIndent())

        myFixture.doHighlighting()
        myFixture.checkResultWithInlays("""
            local a = class("A")

            ---@param cnt number
            function a:ctor(self, cnt)
            end

            local b = a.new(<hint text="cnt : "/>111)
        """.trimIndent())
    }

    fun `test renaming initializer does not rename constructor alias`() {
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            function a:<caret>ctor(cnt)
            end

            local b = a.new(111)
        """.trimIndent())

        myFixture.renameElementAtCaret("initialize")
        myFixture.checkResult("""
            ---@class A
            local a = {}

            function a:initialize(cnt)
            end

            local b = a.new(111)
        """.trimIndent())
    }

    fun `test renaming initializer does not rename bracket constructor alias`() {
        myFixture.configureByText("test.lua", """
            ---@class A
            local a = {}

            function a:<caret>ctor(cnt)
            end

            local b = a["new"](111)
        """.trimIndent())

        myFixture.renameElementAtCaret("initialize")
        myFixture.checkResult("""
            ---@class A
            local a = {}

            function a:initialize(cnt)
            end

            local b = a["new"](111)
        """.trimIndent())
    }
}
