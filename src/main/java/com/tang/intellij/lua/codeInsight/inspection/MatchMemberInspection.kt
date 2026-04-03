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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class MatchMemberInspection : StrictInspection() {
    override fun isAvailableForFile(file: PsiFile): Boolean {
        return !LuaFileUtil.isStdLibFile(file.virtualFile, file.project)
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
            fun isFieldInCondition(o: PsiElement): Boolean {
                val parent = o.parent
                when (parent) {

                    is LuaIfStat -> {
                        return true
                    }

                    is LuaWhileStat -> {
                        return true
                    }
                    //if a and a.b and a.b.c then
                    is LuaIndexExpr -> {
                        val binaryExpr = PsiTreeUtil.getParentOfType(parent, LuaBinaryExpr::class.java)
                        if (binaryExpr != null) {
                            return isBinaryExprContainText(binaryExpr, o.text)
                        }
                    }

                    is LuaBinaryExpr -> {
                        return isFieldInCondition(parent)
                    }
                }
                return false
            }

            fun isBinaryExprContainText(binaryExpr: LuaBinaryExpr, text: String): Boolean {
                val left = binaryExpr.left
                if (left != null) {
                    if (left is LuaBinaryExpr) {
                        return isBinaryExprContainText(left, text)
                    }
                    if (left.text == text) {
                        return true
                    }
                }
                val right = binaryExpr.right
                if (right != null) {
                    if (right is LuaBinaryExpr) {
                        return isBinaryExprContainText(right, text)
                    }
                    if (right.text == text) {
                        return true
                    }
                }
                return false
            }

            fun isMemberAfterCondition(o: LuaIndexExpr): Boolean {
                val block = PsiTreeUtil.getParentOfType(o, LuaBlock::class.java)
                if (block != null) {
                    var text = o.text
                    //if function ,then check field
                    if (o.colon != null) {
                        val index = text.lastIndexOf(":")
                        text = text.substring(0, index) + "." + text.substring(index + 1)
                    }
                    var previousPsi = block.prevSibling
                    while (previousPsi != null) {
                        when (previousPsi) {
                            is LuaIndexExpr -> {
                                if (previousPsi.text == text) {
                                    return true
                                }
                                previousPsi = previousPsi.prevSibling
                            }

                            is LuaBinaryExpr -> {
                                if (isBinaryExprContainText(previousPsi, text)) {
                                    return true
                                }
                                previousPsi = previousPsi.prevSibling
                            }

                            is LeafPsiElement -> {
                                if (previousPsi.text == "if") return false
                                if (previousPsi.text == "when") return false
                                if (previousPsi.text == "elseif") return false
                                previousPsi = previousPsi.prevSibling
                            }

                            else -> {
                                previousPsi = previousPsi.prevSibling
                            }
                        }
                    }
                }
                return false
            }

            fun checkLuaIndexExpr(o: LuaIndexExpr) {
                if (o.lastChild == null) return
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
                        if (resolve(o, searchContext) != null) {
                            return
                        }
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
                                    if (!LuaSettings.isConstructorName(funcName) && !isMemberAfterCondition(o))
                                        myHolder.registerProblem(psi, "Unknown function '%s'.".format(funcName))
                                }
                            }
                            else {
                                if (!isFieldInCondition(o) && !isMemberAfterCondition(o))
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
