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

package com.tang.intellij.test.inspections

import com.tang.intellij.lua.codeInsight.inspection.MatchMemberInspection
import com.tang.intellij.lua.project.LuaSettings

class MatchMemberInspectionTest : LuaInspectionsTestBase(MatchMemberInspection()) {

    fun `test known field definition with unknown type`() = checkByText("""
        local t = {}
        local value
        t.foo = value

        print(t.foo)
    """)

    fun `test known function definition with unknown type`() = checkByText("""
        local t = {}
        local func
        t.foo = func

        t.foo()
    """)

    fun `test unknown field still errors`() = checkByText("""
        local t = {}

        print(t.<error descr="Unknown field 'foo'.">foo</error>)
    """)

    fun `test unknown function still errors`() = checkByText("""
        local t = {}

        t.<error descr="Unknown function 'foo'.">foo</error>()
    """)

    fun `test mapped constructor without initializer still errors`() {
        withConstructorNames("new=ctor") {
            checkByText("""
                ---@class A
                local a = {}

                a.<error descr="Unknown function 'new'.">new</error>()
            """.trimIndent())
        }
    }

    fun `test legacy constructor without initializer stays compatible`() {
        withConstructorNames("new") {
            checkByText("""
                ---@class A
                local a = {}

                a.new()
            """.trimIndent())
        }
    }

    private fun withConstructorNames(value: String, action: () -> Unit) {
        val settings = LuaSettings.instance
        val original = settings.constructorNames.copyOf()
        try {
            settings.constructorNamesString = value
            action()
        } finally {
            settings.constructorNames = original
        }
    }
}
