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

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class MatchMemberInspection : StrictInspection() {
    override fun isAvailableForFile(file: PsiFile): Boolean {
        if (LuaFileUtil.isStdLibFile(file.virtualFile, file.project)) {
            return false
        }
        return true
    }

    fun onlyHaveClassInfo(ty:ITy):Boolean{
        if(ty is TyLazyClass){
            return false
        }
        if(ty is TySerializedClass){
            return true
        }
        if(ty is TyUnion){
            var allClass = true
            ty.each { t->
                if(t is TyLazyClass)
                {
                    allClass = false
                }
                else if(t !is TySerializedClass){
                    allClass = false
                }
            }
            return allClass
        }
        return false
    }

    override fun buildVisitor(
        myHolder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor =
        object : LuaVisitor() {
            fun checkLuaIndexExpr(o:LuaIndexExpr){
                if(o.lastChild == null) return
                val searchContext = SearchContext.get(o.project)
                val nextSibling = o.nextSibling
                var isFunction = false
                if (nextSibling != null) {
                    isFunction = nextSibling is LuaListArgs
                }

                val previousType = o.prefixExpr.guessType(searchContext)
                if (previousType != Ty.UNKNOWN && !onlyHaveClassInfo(previousType)) {
                    val type = o.guessType(searchContext)
                    var parent = o.parent
                    while (parent != null) {
                        if (parent is LuaVarList) {
                            val next = o.nextSibling
                            if(next is LeafPsiElement) {
                                if(next.text == "." || next.text == ":") {
                                    break
                                }
                            }
                            return
                        }
                        if(parent is LuaClassMethodName){
                            break
                        }
                        parent = parent.parent
                    }
                    if (type == Ty.NIL || type == Ty.UNKNOWN) {
                        val psi = o.lastChild
                        if (psi != null) {
                            val nodeType = psi.node.elementType
                            if(nodeType != LuaTypes.ID) {
                                return
                            }
                            if (isFunction)
                            {
                                val funcName = o.name
                                if (funcName != null) {
                                    if (!LuaSettings.isConstructorName(funcName))
                                        myHolder.registerProblem(psi, "Unknown function '%s'.".format(funcName))
                                }
                            }
                            else {
                                myHolder.registerProblem(psi, "Unknown field '%s'.".format(o.name))
                            }
                        }
                    }
                }
            }

            override fun visitIndexExpr(o: LuaIndexExpr) {
                super.visitIndexExpr(o)
                checkLuaIndexExpr(o)
            }
        }
}