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

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.project.StrictGlobalNamesManager
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext

class StrictGlobalName: StrictInspection() {
    private fun registerStrictGlobalNameProblem(holder: ProblemsHolder, name: String, id: PsiElement) {
        holder.registerProblem(id, "Global name \"$name\" not in strict names", AddToStrictGlobalNamesQuickFix(name))
    }

    private fun isCallExprName(o: LuaNameExpr): Boolean {
        val parent = o.parent as? LuaCallExpr ?: return false
        return parent.expr == o
    }

    private fun isStdBuiltinFunction(o: LuaNameExpr, context: SearchContext): Boolean {
        if (!isCallExprName(o)) {
            return false
        }

        var isBuiltin = false
        LuaShortNamesManager.getInstance(o.project).processMembers(Constants.WORD_G, o.name, context, {
            val containingFile = it.containingFile
            if (LuaFileUtil.isStdLibFile(containingFile.virtualFile, o.project) && it is LuaFuncDef) {
                isBuiltin = true
                false
            } else {
                true
            }
        })
        return isBuiltin
    }

    private fun shouldIgnoreStrictGlobalName(o: LuaNameExpr, context: SearchContext): Boolean {
        val psiFile = o.containingFile
        val isModuleFile = if (psiFile is LuaPsiFile) {
            psiFile.moduleName != null
        } else false

        if (isModuleFile) {
            return true
        }

        if (isCallExprName(o) && LuaSettings.isRequireLikeFunctionName(o.name)) {
            return true
        }

        return isStdBuiltinFunction(o, context)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        var strictGlobalNames: Set<String>? = null
        fun containsStrictGlobalName(project: Project, name: String): Boolean {
            if (strictGlobalNames == null) {
                strictGlobalNames = StrictGlobalNamesManager.getStrictGlobalNames(project)
            }
            return strictGlobalNames.contains(name)
        }

        return object : LuaVisitor() {
            override fun visitNameExpr(o: LuaNameExpr) {
                val searchContext = SearchContext.get(o.project)
                if (shouldIgnoreStrictGlobalName(o, searchContext)) {
                    return
                }

                val id = o.firstChild
                var containingFile = o.containingFile
                if (LuaFileUtil.isStdLibFile(containingFile.virtualFile, o.project)) {
                    return
                }
                val res = resolve(o, searchContext)
                if (res != null) { //std api highlighting
                    containingFile = res.containingFile
                    if (LuaFileUtil.isStdLibFile(containingFile.virtualFile, o.project)) {
                        return
                    }
                    val name = id.text
                    if (res is LuaParamNameDef) {

                    } else if (res is LuaFuncDef) {

                    } else {
                        if (id.textMatches(Constants.WORD_SELF)) {

                        } else if (res is LuaNameDef) {

                        } else if (res is LuaLocalFuncDef) {

                        } else {
                            if (!containsStrictGlobalName(o.project, name))
                                registerStrictGlobalNameProblem(holder, name, id)
                        }
                    }
                } else {
                    val name = id.text
                    if (!containsStrictGlobalName(o.project, name))
                        registerStrictGlobalNameProblem(holder, name, id)
                }
            }
        }
    }

    private class AddToStrictGlobalNamesQuickFix(private val name: String) : LocalQuickFix {
        override fun getFamilyName() = "Add \"$name\" to strict global names"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            StrictGlobalNamesManager.add(project, name)
        }
    }
}
