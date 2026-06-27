package com.tang.intellij.test.inspections

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.api.LuaComment
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

    fun `test numeric for parameter does not get weak warning`() {
        myFixture.configureByText(
            "main.lua",
            """
            local childCount = 10
            
            for i = 1, childCount do
                print(i)
            end
            """.trimIndent()
        )

        myFixture.enableInspections(MissingParameterAnnotationInspection())
        myFixture.checkHighlighting(false, false, true)
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

    fun `test override return does not get weak warning`() {
        myFixture.configureByText(
            "main.lua",
            """
            ---@class Base
            local Base = {}
            
            ---@return string
            function Base:name()
                return "base"
            end
            
            ---@class Derived:Base
            local Derived = {}
            
            ---@override
            function Derived:name()
                return "derived"
            end
            """.trimIndent()
        )

        myFixture.enableInspections(MissingReturnAnnotationInspection())
        myFixture.checkHighlighting(false, false, true)
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

    fun `test create self type annotation quick fix`() {
        checkByDirectory(
            before = """
            --- main.lua
            ---@class Foo
            local Foo = {}
            
            function Foo:init()
                self.ba<caret>r = unknown()
            end
            """.trimIndent(),
            after = """
            --- main.lua
            ---@class Foo
            local Foo = {}
            
            function Foo:init()
                ---@type table
                self.bar = unknown()
            end
            """.trimIndent()
        ) {
            myFixture.configureFromTempProjectFile("main.lua")
            myFixture.enableInspections(MissingSelfFieldAnnotationInspection())
            myFixture.findSingleIntention("Generate field annotation")
            val intention = myFixture.findSingleIntention("Generate type annotation")
            myFixture.launchAction(intention)
        }
    }

    fun `test field annotation inserts after last field`() {
        checkByDirectory(
            before = """
            --- main.lua
            ---@class Foo
            ---@field public existing string
            local Foo = {}
            
            function Foo:init()
                self.bar = unknown()
            end
            """.trimIndent(),
            after = """
            --- main.lua
            ---@class Foo
            ---@field public existing string
            ---@field bar table
            local Foo = {}
            
            function Foo:init()
                self.bar = unknown()
            end
            """.trimIndent()
        ) {
            myFixture.configureFromTempProjectFile("main.lua")
            val comment = PsiTreeUtil.findChildOfType(myFixture.file, LuaComment::class.java)!!
            WriteCommandAction.runWriteCommandAction(project) {
                LuaCommentUtil.insertFieldAnnotation(comment, "bar", "table")
            }
        }
    }

    fun `test field annotation inserts after trailing field text`() {
        val before = """
            --- main.lua
            ---@class Foo
            ---@field boxCollider UnityEngine.BoxCollider<VT>---box collider data
            local Foo = {}

            function Foo:init()
                self.flowEffectGo = unknown()
            end
            """.trimIndent().replace("<VT>", "\u000B")
        val after = """
            --- main.lua
            ---@class Foo
            ---@field boxCollider UnityEngine.BoxCollider<VT>---box collider data
            ---@field flowEffectGo table
            local Foo = {}

            function Foo:init()
                self.flowEffectGo = unknown()
            end
            """.trimIndent().replace("<VT>", "\u000B")

        checkByDirectory(before, after) {
            myFixture.configureFromTempProjectFile("main.lua")
            val comment = PsiTreeUtil.findChildOfType(myFixture.file, LuaComment::class.java)!!
            WriteCommandAction.runWriteCommandAction(project) {
                LuaCommentUtil.insertFieldAnnotation(comment, "flowEffectGo", "table")
            }
        }
    }
}
