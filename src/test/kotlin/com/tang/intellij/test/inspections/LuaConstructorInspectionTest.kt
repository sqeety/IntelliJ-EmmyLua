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

import com.tang.intellij.lua.codeInsight.inspection.MatchFunctionSignatureInspection
import com.tang.intellij.lua.project.LuaSettings

class LuaConstructorInspectionTest : LuaInspectionsTestBase(MatchFunctionSignatureInspection()) {
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

    fun `test constructor arguments use initializer signature`() = checkByText("""
        ---@class A
        local a = {}

        ---@param cnt number
        function a:ctor(cnt)
        end

        a.new(111)
        a.new(<warning descr="Type mismatch. Required: 'number' Found: 'string'">"wrong"</warning>)
        a.new(<warning descr="Missing argument: cnt: number">)</warning>
        a.new(1, <warning descr="Too many arguments.">2</warning>)
    """.trimIndent())

    fun `test generic constructor arguments use instantiated parameter type`() = checkByText("""
        ---@class Box<T>
        local Box = {}

        ---@param value T
        function Box:ctor(value)
        end

        ---@type Box<number>
        local box = Box
        box.new(1)
        box.new(<warning descr="Type mismatch. Required: 'number' Found: 'string'">"wrong"</warning>)
    """.trimIndent())

    fun `test constructor overloads are preserved`() = checkByText("""
        ---@class A
        local a = {}

        ---@overload fun(cnt: string, exact: boolean)
        ---@param cnt number
        function a:ctor(cnt)
        end

        a.new(1)
        a.new("text", true)
    """.trimIndent())
}
