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

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.highlighting.LuaHighlightingData
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext

class StrictGlobalName: StrictInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : LuaVisitor() {
            override fun visitNameExpr(o: LuaNameExpr) {
                val id = o.firstChild
                var containingFile = o.containingFile
                if (LuaFileUtil.isStdLibFile(containingFile.virtualFile, o.project)) {
                    return
                }
                val res = resolve(o, SearchContext.get(o.project))
                if (res != null) { //std api highlighting
                    containingFile = res.containingFile
                    if (LuaFileUtil.isStdLibFile(containingFile.virtualFile, o.project)) {
                        return
                    }
                    val name = id.text
                    if (res is LuaParamNameDef) {

                    }else if (res is LuaFuncDef) {

                    }else {
                        if (id.textMatches(Constants.WORD_SELF)) {

                        } else if (res is LuaNameDef) {

                        } else if (res is LuaLocalFuncDef) {

                        } else {
                            if(!LuaSettings.instance.strictGlobalNames.contains(name))
                                holder.registerProblem(id, "Global name \"$name\" not in strict names")
                        }
                    }
                } else {
                    val name = id.text
                    if(!LuaSettings.instance.strictGlobalNames.contains(name))
                        holder.registerProblem(id, "Global name \"$name\" not in strict names")
                }
            }
        }
    }
}