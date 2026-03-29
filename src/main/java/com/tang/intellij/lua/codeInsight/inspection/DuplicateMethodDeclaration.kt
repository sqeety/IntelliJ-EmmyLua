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
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassMemberIndex
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TyClass

//同一个类中函数重复定义报错
class DuplicateMethodDeclaration : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitClassMethodDef(o: LuaClassMethodDef) {
                if (o.useScope !is GlobalSearchScope) return
                val context = SearchContext.get(o.project)
                
                // 获取方法名
                val methodName = o.classMethodName.id?.text ?: return
                
                // 获取方法所属的类类型
                val ty = o.classMethodName.expr.guessType(context)
                if (Ty.isInvalid(ty) || ty !is TyClass) return
                
                // 类名
                val className = ty.className
                
                // 检查同类中是否有同名方法（使用 CLASS_MEMBER 索引）
                val key = "$className**$methodName"
                val hashCode = key.hashCode()
                val all = LuaClassMemberIndex.instance.get(hashCode, o.project, context.scope)
                
                for (def in all) {
                    if (def != o && def is LuaClassMethodDef) {
                        // 确保是同一个类定义的方法（检查文件）
                        val defClassName = def.classMethodName.expr.guessType(context)
                        if (defClassName == ty) {
                            val path = def.containingFile?.virtualFile?.canonicalPath
                            if (path != null) {
                                holder.registerProblem(
                                    o.classMethodName,
                                    LuaBundle.message("inspection.duplicate_method", path),
                                    ProblemHighlightType.GENERIC_ERROR
                                )
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}