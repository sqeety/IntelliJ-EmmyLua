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

package com.tang.intellij.lua.util

import com.intellij.psi.codeStyle.NameUtil
import com.tang.intellij.lua.editor.LuaNameSuggestionProvider.Companion.fixName
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.LuaExpr
import com.tang.intellij.lua.psi.LuaIndexExpr
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.*

class LuaNameUtil {
    companion object {
        fun getNames(expr: LuaExpr?, set: MutableSet<String>) {
            if(expr == null) return
            when (expr) {
                is LuaCallExpr -> {
                    return getNames(expr.expr, set)
                }

                is LuaIndexExpr -> {
                    val id = expr.id?.text
                    if (id != null) {
                        val strings = NameUtil.getSuggestionsByName(id, "", "", false, true, false)
                        set.addAll(strings)
                    }

                }
            }
        }

        fun collectNames(type: ITy, context: SearchContext, collector: (name: String, suffix: String, preferLonger: Boolean) -> Unit) {
            when (type) {
                is ITyClass -> {
                    if (!type.isAnonymous && type !is TyDocTable)
                        collector(fixName(type.className), "", false)
                    TyClass.processSuperClass(type, context, mutableSetOf()) { superType ->
                        if (!superType.isAnonymous)
                            collector(fixName(superType.className), "", false)
                        true
                    }
                }
                is ITyArray -> collectNames(type.base, context) { name, _, _ ->
                    collector(name, "List", false)
                }
                is ITyGeneric -> {
                    val paramTy = type.getParamTy(1)
                    collectNames(paramTy, context) { name, _, _ ->
                        collector(name, "Map", false)
                    }
                }
            }
        }
    }
}