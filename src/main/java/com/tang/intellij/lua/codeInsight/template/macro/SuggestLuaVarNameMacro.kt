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

package com.tang.intellij.lua.codeInsight.template.macro

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Macro
import com.intellij.codeInsight.template.Result
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TyUnion
import com.tang.intellij.lua.util.LuaNameUtil

/**
 *
 * Created by TangZX on 2017/4/8.
 */
class SuggestLuaVarNameMacro : Macro() {
    override fun getName(): String {
        return "SuggestLuaVarNameMacro"
    }

    override fun getPresentableName(): String {
        return "SuggestLuaVarNameMacro()"
    }

    override fun calculateResult(expressions: Array<Expression>, expressionContext: ExpressionContext): Result? {
        return null
    }

    fun nextValidPsiElement(psi: PsiElement): PsiElement? {
        var next = psi
        while (next != null) {
            when (next) {
                is PsiWhiteSpace -> {
                    next = psi.nextSibling
                }

                is LeafPsiElement -> {
                    next = psi.nextSibling
                }

                else -> {
                    return next
                }
            }
        }
        return null
    }

    override fun calculateLookupItems(
        params: Array<out Expression>,
        context: ExpressionContext
    ): Array<LookupElement> {
        val list = mutableListOf<LookupElement>()
        var pin = context.psiElementAtStartOffset
        if (pin != null) {
            pin = nextValidPsiElement(pin)
            if(pin != null){
                when (pin) {
                    is LuaLocalDef -> {
                        val exprList = pin.exprList?.exprList
                        if (exprList != null) {
                            val set = LinkedHashSet<String>()
                            val searchContext = SearchContext.get(pin.getProject())
                            for (expr in exprList) {
                                LuaNameUtil.getNames(expr, set)
                                val type = expr.guessType(searchContext)
                                if (!Ty.isInvalid(type)) {
                                    val names = HashSet<String>()

                                    TyUnion.each(type) { ty ->
                                        LuaNameUtil.collectNames(ty, searchContext) { name, suffix, preferLonger ->
                                            if (names.add(name)) {
                                                val strings = NameUtil.getSuggestionsByName(name, "", suffix, false, preferLonger, false)
                                                for (str in strings) {
                                                    list.add(LookupElementBuilder.create(str))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            for (name in set) {
                                val strings = NameUtil.getSuggestionsByName(name, "", "", false, true, false)
                                for (str in strings) {
                                    list.add(LookupElementBuilder.create(str))
                                }
                            }
                        }
                    }
                }
            }
        }
        return list.toTypedArray()
    }
}
