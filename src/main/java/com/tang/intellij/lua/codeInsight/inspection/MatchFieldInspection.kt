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
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class MatchFieldInspection : StrictInspection() {
    override fun isAvailableForFile(file: PsiFile): Boolean {
        if (LuaFileUtil.isStdLibFile(file.virtualFile, file.project)) {
            return false
        }
        return true
    }

    fun onlyHaveClassInfo(ty:ITy):Boolean{
        if(ty is TySerializedClass){
            return true
        }
        if(ty is TyUnion){
            var allClass = true
            ty.each { t->
                if(t !is TySerializedClass){
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
            override fun visitIndexExpr(o: LuaIndexExpr) {
                super.visitIndexExpr(o)
                if(o.lastChild == null) return
                val searchContext = SearchContext.get(o.project)
                val nextSibling = o.nextSibling
                var isFunction = false
                if (nextSibling != null) {
                    isFunction = nextSibling.text == "("
                }
                val previousType = o.prefixExpr.guessType(searchContext)
                if (previousType != Ty.UNKNOWN && !onlyHaveClassInfo(previousType)) {
                    val type = o.guessType(searchContext)
                    if (type == Ty.NIL || type == Ty.UNKNOWN) {
                            if(isFunction)
                                myHolder.registerProblem(o.lastChild, "Unknown function '%s'.".format(o.name))
                            else
                            {
                                if (o.lastChild.text != "]") {
                                    myHolder.registerProblem(o.lastChild, "Unknown field '%s'.".format(o.name))
                            }
                        }
                    }
                }
            }
        }
}