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
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class MatchFieldInspection : StrictInspection() {
    override fun buildVisitor(
        myHolder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor =
        object : LuaVisitor() {
            override fun visitIndexExpr(o: LuaIndexExpr) {
                super.visitIndexExpr(o)
                val searchContext = SearchContext.get(o.project)
                val type = o.guessType(searchContext)
                val previousType = o.prefixExpr.guessType(searchContext)
                if(previousType != Ty.UNKNOWN) {
                    if (type == Ty.NIL || type == Ty.UNKNOWN) {
                        val nextSibling = o.nextSibling
                        if (nextSibling != null) {
                            if (nextSibling.text == "." || nextSibling.text == ":") {
                                myHolder.registerProblem(o.lastChild, "Unknown field '%s'.".format(o.name))
                            } else {
                                myHolder.registerProblem(o.lastChild, "Unknown function '%s'.".format(o.name))
                            }
                        } else {
                            myHolder.registerProblem(o.lastChild, "Unknown field '%s'.".format(o.name))
                        }
                    }
                }
            }
        }
}