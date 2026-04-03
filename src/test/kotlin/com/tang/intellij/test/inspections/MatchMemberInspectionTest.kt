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

    fun `test unknown field still warns`() = checkByText("""
        local t = {}

        print(t.<warning>foo</warning>)
    """)

    fun `test unknown function still warns`() = checkByText("""
        local t = {}

        t.<warning>foo</warning>()
    """)
}
