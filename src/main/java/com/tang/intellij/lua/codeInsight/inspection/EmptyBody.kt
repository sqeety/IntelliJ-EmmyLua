package com.tang.intellij.lua.codeInsight.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.LuaBlock
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaDoStat
import com.tang.intellij.lua.psi.LuaForAStat
import com.tang.intellij.lua.psi.LuaForBStat
import com.tang.intellij.lua.psi.LuaTypes
import com.tang.intellij.lua.psi.LuaVisitor
import com.tang.intellij.lua.psi.LuaWhileStat
import org.jetbrains.annotations.Nls

class EmptyBody : EmptyBodyBase() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitForAStat(o: LuaForAStat) {
                checkBlock(o, holder, LuaTypes.FOR, "Empty for body", "Remove empty for")
            }

            override fun visitForBStat(o: LuaForBStat) {
                checkBlock(o, holder, LuaTypes.FOR, "Empty for body", "Remove empty for")
            }

            override fun visitDoStat(o: LuaDoStat) {
                checkBlock(o, holder, LuaTypes.DO, "Empty do body", "Remove empty do")
            }

            override fun visitWhileStat(o: LuaWhileStat) {
                checkBlock(o, holder, LuaTypes.WHILE, "Empty while body", "Remove empty do")
            }

            override fun visitClassMethodDef(o: LuaClassMethodDef) {
                checkBlock(o, holder, LuaTypes.FUNCTION, "Empty func body", "Remove empty func")
            }
        }
    }

    private fun checkBlock(o: PsiElement, holder: ProblemsHolder, highlightType: IElementType, message: String, familyName: String) {
        val block = PsiTreeUtil.findChildOfType(o, LuaBlock::class.java)
        if (!isValidBlock(block)) {
            val forNode: ASTNode = checkNotNull(o.node.findChildByType(highlightType))
            val forElement = forNode.psi
            val offset = forElement.node.startOffset - o.node.startOffset
            val textRange = TextRange(offset, offset + forElement.textLength)
            holder.registerProblem(o, textRange, message, Fix(familyName))
        }
    }

    private class Fix(@Nls private val familyName: String) : LocalQuickFix {
        override fun getFamilyName(): String = familyName

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor.endElement?.delete()
        }
    }
}
