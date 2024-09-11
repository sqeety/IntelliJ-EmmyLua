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

package com.tang.intellij.lua.ty

import com.intellij.openapi.project.Project
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.psi.LuaCallExpr
import com.tang.intellij.lua.psi.prefixExpr
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassIndex

interface ITySubstitutor {
    fun substitute(function: ITyFunction, context: SearchContext): ITy
    fun substitute(clazz: ITyClass, context: SearchContext): ITy
    fun substitute(generic: ITyGeneric, context: SearchContext): ITy
    fun substitute(ty: ITy, context: SearchContext): ITy
}

class GenericAnalyzer(arg: ITy, private val par: ITy) : TyVisitor() {
    var cur: ITy = arg

    var map:MutableMap<String, ITy>? = null

    fun analyze(result: MutableMap<String, ITy>) {
        map = result
        warp(cur) { par.accept(this) }
        map = null
    }

    override fun visitClass(clazz: ITyClass) {
        map?.get(clazz.className) ?: return
        map?.merge(clazz.className, cur) { a, b -> a.union(b) }
    }

    override fun visitUnion(u: TyUnion) {
        TyUnion.each(u) { it.accept(this) }
    }

    override fun visitArray(array: ITyArray) {
        TyUnion.each(cur) {
            if (it is ITyArray) {
                warp(it.base) {
                    array.base.accept(this)
                }
            }
        }
    }

    override fun visitFun(f: ITyFunction) {
        TyUnion.each(cur) {
            if (it is ITyFunction) {
                visitSig(it.mainSignature, f.mainSignature)
            }
        }
    }

    override fun visitGeneric(generic: ITyGeneric) {
        TyUnion.each(cur) {
            if (it is ITyGeneric) {
                warp(it.base) {
                    generic.base.accept(this)
                }
                it.params.forEachIndexed { index, iTy ->
                    warp(iTy) {
                        generic.getParamTy(index).accept(this)
                    }
                }
            }
        }
    }

    private fun visitSig(arg: IFunSignature, par: IFunSignature) {
        warp(arg.returnTy) { par.returnTy.accept(this) }
    }

    private fun warp(ty:ITy, action: () -> Unit) {
        if (Ty.isInvalid(ty))
            return
        val arg = cur
        cur = ty
        action()
        cur = arg
    }
}

open class TySubstitutor : ITySubstitutor {
    override fun substitute(ty: ITy, context: SearchContext) = ty

    override fun substitute(clazz: ITyClass, context: SearchContext): ITy {
        return clazz
    }

    override fun substitute(generic: ITyGeneric, context: SearchContext): ITy {
        return generic
    }

    override fun substitute(function: ITyFunction, context: SearchContext): ITy {
        return TySerializedFunction(function.mainSignature.substitute(this, context),
                function.signatures.map { it.substitute(this, context) }.toTypedArray(),
                function.flags)
    }
}

class TyAliasSubstitutor private constructor() : ITySubstitutor {
    private val alreadyProcessed = hashSetOf<String>()

    companion object {
        fun substitute(ty: ITy, context: SearchContext): ITy {
            return ty.substitute(TyAliasSubstitutor(), context)
        }
    }

    override fun substitute(function: ITyFunction, context: SearchContext): ITy {
        return TySerializedFunction(function.mainSignature.substitute(this, context),
                function.signatures.map { it.substitute(this, context) }.toTypedArray(),
                function.flags)
    }

    override fun substitute(clazz: ITyClass, context: SearchContext): ITy {
        if (!alreadyProcessed.add(clazz.className))
            return clazz
        if (clazz is TySerializedClass) {
            val result = LuaClassIndex.find(clazz.className, context)
            if (result != null) {
                return clazz.recoverAlias(context, this).union(result.type)
            }
            return clazz.recoverAlias(context, this)
        } else {
            return clazz.recoverAlias(context, this).union(clazz)
        }
    }

    override fun substitute(generic: ITyGeneric, context: SearchContext): ITy {
        return generic
    }

    override fun substitute(ty: ITy, context: SearchContext): ITy {
        return ty
    }
}

class TySelfSubstitutor(val project: Project, val call: LuaCallExpr?, val self: ITy? = null) : TySubstitutor() {
    private val selfType: ITy by lazy {
        self ?: (call?.prefixExpr?.guessType(SearchContext.get(project)) ?: Ty.UNKNOWN)
    }

    override fun substitute(clazz: ITyClass, context: SearchContext): ITy {
        if (clazz.className == Constants.WORD_SELF) {
            return selfType
        }
        return super.substitute(clazz, context)
    }
}