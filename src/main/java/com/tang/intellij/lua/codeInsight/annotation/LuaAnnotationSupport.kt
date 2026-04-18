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

package com.tang.intellij.lua.codeInsight.annotation

import com.tang.intellij.lua.psi.LuaClassMethod
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaCommentOwner
import com.tang.intellij.lua.psi.LuaClosureExpr
import com.tang.intellij.lua.psi.LuaFuncBodyOwner
import com.tang.intellij.lua.psi.LuaLocalDef
import com.tang.intellij.lua.psi.LuaParamNameDef
import com.tang.intellij.lua.psi.guessClassType
import com.tang.intellij.lua.psi.owner
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.Ty
import com.tang.intellij.lua.ty.TyTuple
import com.tang.intellij.lua.ty.TyUnion
import com.tang.intellij.lua.ty.isAnonymous
import com.tang.intellij.lua.ty.isGlobal

object LuaAnnotationSupport {
    fun shouldSuggestLocalType(localDef: LuaLocalDef): Boolean {
        if (localDef.comment?.tagType != null) {
            return false
        }

        val nameDef = localDef.nameList?.nameDefList?.singleOrNull() ?: return false
        return getTypeText(nameDef.guessType(SearchContext.get(localDef.project))) == null
    }

    fun canGenerateParameterAnnotation(paramNameDef: LuaParamNameDef): Boolean {
        val owner = paramNameDef.owner
        return owner is LuaFuncBodyOwner && owner is LuaCommentOwner && owner !is LuaClosureExpr
    }

    fun getParameterTypeText(paramNameDef: LuaParamNameDef): String? {
        if (!canGenerateParameterAnnotation(paramNameDef)) {
            return null
        }

        val owner = paramNameDef.owner as LuaCommentOwner
        if (owner.comment?.getParamDef(paramNameDef.name) != null) {
            return null
        }
        return getTypeText(paramNameDef.guessType(SearchContext.get(paramNameDef.project)))
    }

    fun canGenerateReturnAnnotation(bodyOwner: LuaFuncBodyOwner): Boolean {
        if (bodyOwner !is LuaCommentOwner || bodyOwner is LuaClosureExpr) {
            return false
        }

        val comment = bodyOwner.comment
        if (comment?.tagReturn != null) {
            return false
        }

        if (bodyOwner is LuaClassMethodDef && comment?.isOverride() == true) {
            val context = SearchContext.get(bodyOwner.project)
            val classType = bodyOwner.guessClassType(context)
            val methodName = bodyOwner.name
            val superMember = if (classType != null && methodName != null) {
                classType.findSuperMember(methodName, context)
            } else {
                null
            }

            if (superMember is LuaClassMethod && getTypeText(superMember.guessReturnType(context)) != null) {
                return false
            }
        }

        return true
    }

    fun getReturnType(bodyOwner: LuaFuncBodyOwner): ITy {
        if (!canGenerateReturnAnnotation(bodyOwner)) {
            return Ty.UNKNOWN
        }
        return bodyOwner.guessReturnType(SearchContext.get(bodyOwner.project))
    }

    fun getReturnTypeText(bodyOwner: LuaFuncBodyOwner): String? {
        if (bodyOwner is LuaCommentOwner && bodyOwner.comment?.tagReturn != null) {
            return null
        }
        return getTypeText(bodyOwner.guessReturnType(SearchContext.get(bodyOwner.project)))
    }

    fun getTypeText(type: ITy?): String? {
        return when (type) {
            null -> null
            is TyTuple -> {
                val parts = type.list.mapNotNull(::getTypeText)
                if (parts.size == type.list.size && parts.isNotEmpty()) parts.joinToString(", ") else null
            }
            else -> {
                val names = linkedSetOf<String>()
                TyUnion.each(type) { child ->
                    when {
                        Ty.isInvalid(child) -> Unit
                        child is ITyClass && (child.isAnonymous || child.isGlobal) -> Unit
                        child.displayName.isBlank() -> Unit
                        else -> names.add(child.displayName)
                    }
                }
                names.takeIf { it.isNotEmpty() }?.joinToString("|")
            }
        }
    }
}
