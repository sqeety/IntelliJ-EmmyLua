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

package com.tang.intellij.lua.psi

import com.intellij.util.Processor
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.ITyGeneric
import com.tang.intellij.lua.ty.TyTuple

data class LuaConstructorTarget(val instanceType: ITy, val initializer: LuaClassMember)

fun findConstructorTargets(indexExpr: LuaIndexExpr, context: SearchContext): List<LuaConstructorTarget> {
    val constructorName = indexExpr.name ?: return emptyList()
    return findConstructorTargets(indexExpr, constructorName, context)
}

fun findConstructorTargets(indexExpr: LuaIndexExpr, constructorName: String, context: SearchContext): List<LuaConstructorTarget> {
    val initializerName = LuaSettings.getConstructorInitializerName(constructorName) ?: return emptyList()
    val targets = mutableListOf<LuaConstructorTarget>()

    fun addTarget(instanceType: ITy) {
        if (instanceType is TyTuple) {
            instanceType.list.firstOrNull()?.let(::addTarget)
            return
        }

        val lookupType = (instanceType as? ITyGeneric)?.base ?: instanceType
        lookupType.eachTopClass(Processor { type ->
            val initializer = type.findMember(initializerName, context)
            if (initializer != null) {
                val target = LuaConstructorTarget(instanceType, initializer)
                if (!targets.contains(target))
                    targets.add(target)
            }
            true
        })
    }

    indexExpr.guessParentType(context).each(::addTarget)
    return targets
}
