package com.tang.intellij.lua.codeInsight.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.tang.intellij.lua.psi.LuaBlock
import com.tang.intellij.lua.psi.LuaLocalDef
import com.tang.intellij.lua.psi.LuaLocalFuncDef

abstract class EmptyBodyBase : LocalInspectionTool() {
    fun isValidBlock(block: LuaBlock?): Boolean {
        if (block != null) {
            var child: PsiElement? = block.firstChild
            while (child != null) {
                if (invalidClasses.none { it.isInstance(child) }) {
                    return true
                }
                child = child.nextSibling
            }
        }
        return block == null
    }

    companion object {
        private val invalidClasses = arrayOf(
            PsiWhiteSpace::class.java,
            LuaLocalFuncDef::class.java,
            LuaLocalDef::class.java
        )
    }
}
