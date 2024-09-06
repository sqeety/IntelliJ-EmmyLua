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
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.tang.intellij.lua.LuaBundle
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaVisitor
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TyClass

//同一个文字重复定义函数
class DuplicateMethodDeclaration : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitClassMethodDef(o: LuaClassMethodDef) {
                if (o.useScope !is GlobalSearchScope) return
                val classMethodName = o.classMethodName
                val expr = classMethodName.expr
                val context = SearchContext.get(o.project)
                val ty = expr.guessType(context)
                val className = classMethodName.id?.text
                if (!Ty.isInvalid(ty) && ty is TyClass && className != null) {
                    LuaShortNamesManager.getInstance(o.project).processMembers(ty, className, context) { def ->
                        var continueProcess = true
                        if (def != o && def is LuaClassMethodDef) {
                            if (o.containingFile == def.containingFile && o.classMethodName.expr.guessType(context) == def.classMethodName.expr.guessType(context)) {
                                val path = def.containingFile?.virtualFile?.canonicalPath
                                if (path != null) {
                                    holder.registerProblem(
                                        o.classMethodName,
                                        LuaBundle.message("inspection.duplicate_class", path),
                                        ProblemHighlightType.GENERIC_ERROR
                                    )
                                    continueProcess = false
                                }
                            }

                        }
                        continueProcess
                    }
                }
            }
        }
    }
}