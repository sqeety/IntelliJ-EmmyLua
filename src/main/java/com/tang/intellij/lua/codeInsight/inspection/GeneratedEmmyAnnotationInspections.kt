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

package com.tang.intellij.lua.codeInsight.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.codeInsight.annotation.LuaAnnotationSupport
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.psi.LuaClosureExpr
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaCommentOwner
import com.tang.intellij.lua.psi.LuaFuncBody
import com.tang.intellij.lua.psi.LuaFuncBodyOwner
import com.tang.intellij.lua.psi.LuaIndexExpr
import com.tang.intellij.lua.psi.LuaLocalDef
import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.psi.LuaParamNameDef
import com.tang.intellij.lua.psi.LuaStatement
import com.tang.intellij.lua.psi.LuaVisitor
import com.tang.intellij.lua.psi.guessClassType
import com.tang.intellij.lua.psi.prefixExpr
import com.tang.intellij.lua.psi.resolve
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext

class MissingLocalTypeAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitLocalDef(o: LuaLocalDef) {
                if (!LuaAnnotationSupport.shouldSuggestLocalType(o)) {
                    return
                }

                val nameDef = o.nameList?.nameDefList?.singleOrNull() ?: return
                holder.registerProblem(
                    nameDef,
                    "Type cannot be inferred. Add explicit annotation.",
                    ProblemHighlightType.WEAK_WARNING,
                    AddLocalTypeAnnotationQuickFix()
                )
            }
        }
    }
}

class MissingParameterAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitParamNameDef(o: LuaParamNameDef) {
                val typeText = LuaAnnotationSupport.getParameterTypeText(o) ?: return
                holder.registerProblem(
                    o,
                    "Parameter annotation can be generated.",
                    ProblemHighlightType.WEAK_WARNING,
                    GenerateParameterAnnotationQuickFix(typeText)
                )
            }
        }
    }
}

class MissingReturnAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitFuncBody(o: LuaFuncBody) {
                val bodyOwner = o.parent as? LuaFuncBodyOwner ?: return
                if (bodyOwner is LuaClosureExpr) {
                    return
                }

                val commentOwner = bodyOwner as? LuaCommentOwner ?: return
                val typeText = LuaAnnotationSupport.getReturnTypeText(bodyOwner) ?: return
                val anchor = bodyOwner.funcBody?.rparen ?: bodyOwner as PsiElement
                holder.registerProblem(
                    anchor,
                    "Return annotation can be generated.",
                    ProblemHighlightType.WEAK_WARNING,
                    GenerateReturnAnnotationQuickFix(typeText)
                )
            }
        }
    }
}

class MissingSelfFieldAnnotationInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitIndexExpr(o: LuaIndexExpr) {
                val prefix = o.prefixExpr as? LuaNameExpr ?: return
                if (prefix.name != "self") {
                    return
                }

                val method = PsiTreeUtil.getParentOfType(o, LuaClassMethodDef::class.java) ?: return
                val typeText = LuaAnnotationSupport.getTypeText(o.guessType(SearchContext.get(o.project)))
                if (typeText != null || o.name == null) {
                    return
                }

                val hasFallback = PsiTreeUtil.getParentOfType(o, LuaStatement::class.java) is LuaCommentOwner
                val classType = method.guessClassType(SearchContext.get(o.project))
                val classDef = LuaShortNamesManager.getInstance(o.project).findClass(classType?.className ?: "", SearchContext.get(o.project))
                if (classDef == null && !hasFallback) {
                    return
                }

                holder.registerProblem(
                    o.lastChild,
                    "Field type cannot be inferred. Generate annotation.",
                    ProblemHighlightType.WEAK_WARNING,
                    GenerateSelfFieldAnnotationQuickFix()
                )
            }
        }
    }
}

private class AddLocalTypeAnnotationQuickFix : LocalQuickFix {
    override fun getFamilyName(): String = "Add explicit type annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val localDef = PsiTreeUtil.getParentOfType(descriptor.psiElement, LuaLocalDef::class.java) ?: return
        val editor = LuaCommentUtil.findEditor(localDef)
        if (editor != null) {
            LuaCommentUtil.insertEditableTypeAnnotation(localDef, editor)
        } else {
            LuaCommentUtil.insertTypeTag(localDef, "table")
        }
    }
}

private class GenerateParameterAnnotationQuickFix(private val typeText: String) : LocalQuickFix {
    override fun getFamilyName(): String = "Generate parameter annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val paramDef = PsiTreeUtil.getParentOfType(descriptor.psiElement, LuaParamNameDef::class.java) ?: return
        val owner = PsiTreeUtil.getParentOfType(paramDef, LuaCommentOwner::class.java) ?: return
        LuaCommentUtil.insertParamAnnotation(owner, paramDef.name, typeText)
    }
}

private class GenerateReturnAnnotationQuickFix(private val typeText: String) : LocalQuickFix {
    override fun getFamilyName(): String = "Generate return annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val bodyOwner = PsiTreeUtil.getParentOfType(descriptor.psiElement, LuaFuncBodyOwner::class.java) ?: return
        val commentOwner = bodyOwner as? LuaCommentOwner ?: return
        LuaCommentUtil.insertReturnAnnotation(commentOwner, typeText)
    }
}

private class GenerateSelfFieldAnnotationQuickFix : LocalQuickFix {
    override fun getFamilyName(): String = "Generate field annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val indexExpr = PsiTreeUtil.getParentOfType(descriptor.psiElement, LuaIndexExpr::class.java) ?: return
        val method = PsiTreeUtil.getParentOfType(indexExpr, LuaClassMethodDef::class.java) ?: return
        val fieldName = indexExpr.name ?: return
        val context = SearchContext.get(project)
        val classType = method.guessClassType(context)
        val classDef = if (classType != null) {
            LuaShortNamesManager.getInstance(project).findClass(classType.className, context)
        } else {
            null
        }

        val editor = LuaCommentUtil.findEditor(indexExpr)
        if (classDef != null && editor != null) {
            val comment = PsiTreeUtil.getParentOfType(classDef, LuaComment::class.java)
            if (comment != null) {
                LuaCommentUtil.insertFieldTemplate(comment, editor, fieldName)
                return
            }
        }

        val statement = PsiTreeUtil.getParentOfType(indexExpr, LuaStatement::class.java) as? LuaCommentOwner ?: return
        if (editor != null) {
            LuaCommentUtil.insertEditableTypeTag(statement, editor)
        } else {
            LuaCommentUtil.insertTypeTag(statement, "table")
        }
    }
}
