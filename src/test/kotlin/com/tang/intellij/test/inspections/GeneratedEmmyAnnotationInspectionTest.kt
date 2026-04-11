package com.tang.intellij.test.inspections

import com.tang.intellij.lua.codeInsight.inspection.MissingLocalTypeAnnotationInspection
import com.tang.intellij.lua.codeInsight.inspection.MissingParameterAnnotationInspection
import com.tang.intellij.lua.codeInsight.inspection.MissingReturnAnnotationInspection
import com.tang.intellij.lua.codeInsight.inspection.MissingSelfFieldAnnotationInspection
import com.tang.intellij.test.LuaTestBase
import com.tang.intellij.test.fileTreeFromText

class GeneratedEmmyAnnotationInspectionTest : LuaTestBase() {

    fun `test unresolved local gets weak warning`() {
        myFixture.configureByText(
            "main.lua",
            """
            local <weak_warning descr="Type cannot be inferred. Add explicit annotation.">value</weak_warning> = unknown()
            """.trimIndent()
        )

        myFixture.enableInspections(MissingLocalTypeAnnotationInspection())
        myFixture.checkHighlighting(false, false, true)
    }

    fun `test inferred local does not get weak warning`() {
        myFixture.configureByText(
            "main.lua",
            """
            local value = 1
            """.trimIndent()
        )

        myFixture.enableInspections(MissingLocalTypeAnnotationInspection())
        myFixture.checkHighlighting(false, false, true)
    }

    fun `test create parameter annotation intention uses inferred type`() {
        checkByDirectory(
            before = """
            --- main.lua
            ---@class User
            local User = {}
            
            ---@class Base
            local Base = {}
            
            ---@param user User
            function Base:consume(user)
            end
            
            ---@class Derived:Base
            local Derived = {}
            
            function Derived:consume(us<caret>er)
            end
            """.trimIndent(),
            after = """
            --- main.lua
            ---@class User
            local User = {}
            
            ---@class Base
            local Base = {}
            
            ---@param user User
            function Base:consume(user)
            end
            
            ---@class Derived:Base
            local Derived = {}
            
            ---@param user User
            function Derived:consume(user)
            end
            """.trimIndent()
        ) {
            myFixture.configureFromTempProjectFile("main.lua")
            val intention = myFixture.findSingleIntention("Create parameter annotation")
            myFixture.launchAction(intention)
        }
    }

    fun `test create return annotation intention keeps param order`() {
        checkByDirectory(
            before = """
            --- main.lua
            ---@class User
            local User = {}
            
            ---@param user User
            local function cons<caret>ume(user)
                return user
            end
            """.trimIndent(),
            after = """
            --- main.lua
            ---@class User
            local User = {}
            
            ---@param user User
            ---@return User
            local function consume(user)
                return user
            end
            """.trimIndent()
        ) {
            myFixture.configureFromTempProjectFile("main.lua")
            val intention = myFixture.findSingleIntention("Create return annotation")
            myFixture.launchAction(intention)
        }
    }

    fun `test unresolved self field gets weak warning`() {
        myFixture.configureByText(
            "main.lua",
            """
            ---@class Foo
            local Foo = {}
            
            function Foo:init()
                self.<weak_warning descr="Field type cannot be inferred. Generate annotation.">bar</weak_warning> = unknown()
            end
            """.trimIndent()
        )

        myFixture.enableInspections(MissingSelfFieldAnnotationInspection())
        myFixture.checkHighlighting(false, false, true)
    }
}
