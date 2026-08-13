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

package com.tang.intellij.test.editor

import com.tang.intellij.test.LuaTestBase

class LuaParameterHintsTest : LuaTestBase() {
    fun `test dot function called with colon maps receiver to first parameter`() {
        myFixture.configureByText("test.lua", """
            ---@class T
            local t = {}

            function t.gsub(s, pattern, repl, n)
            end

            ---@type T
            local a = {}
            a:gsub("", "")
        """.trimIndent())

        myFixture.doHighlighting()
        myFixture.checkResultWithInlays("""
            ---@class T
            local t = {}

            function t.gsub(s, pattern, repl, n)
            end

            ---@type T
            local a = {}
            a:gsub(<hint text="pattern : "/>"", <hint text="repl : "/>"")
        """.trimIndent())
    }
}
