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

package com.tang.intellij.lua.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.IntStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass
import com.tang.intellij.lua.ty.TyClass
import com.tang.intellij.lua.ty.TyParameter

class LuaClassMemberIndex : IntStubIndexExtension<LuaClassMember>() {
    override fun getKey() = StubKeys.CLASS_MEMBER

    override fun get(s: Int, project: Project, scope: GlobalSearchScope): Collection<LuaClassMember> =
            StubIndex.getElements(getKey(), s, project, scope, LuaClassMember::class.java)

    companion object {
        val instance = LuaClassMemberIndex()

        private fun process(key: String, context: SearchContext, processor: Processor<LuaClassMember>): Boolean {
            if (context.isDumb)
                return false
            val hashCode = key.hashCode()
            val all = instance.get(hashCode, context.project, context.scope)
            if (all.isEmpty()) return true
            val index = key.indexOf("*")
            if (index == -1) {
                //多个选择直接全部执行
                for (member in all) {
                    if (!processor.process(member)) return false
                }
            } else {
                val projectTys = mutableSetOf<LuaClassMember>()
                val className = key.substring(0, index)
                val leftMembers = mutableSetOf<LuaClassMember>()
                all.forEach {
                    if (LuaFileUtil.isStdLibFile(it.containingFile.virtualFile, it.project)) {
                        if (it is LuaDocTagField || it is LuaClassMethodDef || LuaPsiTreeUtilEx.isClassDefineMember(it, className)) {
                            if (!processor.process(it))
                                return false
                        } else {
                            leftMembers.add(it)
                        }
                    } else {
                        projectTys.add(it)
                    }
                }
                projectTys.forEach {
                    if (it is LuaDocTagField || it is LuaClassMethodDef || LuaPsiTreeUtilEx.isClassDefineMember(it, className)) {
                        if (!processor.process(it))
                            return false
                    } else {
                        leftMembers.add(it)
                    }
                }
                for (member in leftMembers) {
                    if (!processor.process(member)) return false
                }
            }
            return true
        }

        private fun processPureField(className: String, fieldName: String, context: SearchContext, processor: Processor<LuaClassMember>): Boolean {
            if (context.isDumb)
                return false
            val key = "$className**$fieldName"
            val hashCode = key.hashCode()
            val all = instance.get(hashCode, context.project, context.scope)
            if (all.isEmpty()) return true
            all.forEach {
                if (it is LuaDocTagField) {
                    if (!processor.process(it))
                        return false
                }
            }
            return true
        }

        fun process(className: String, fieldName: String, context: SearchContext, processor: Processor<LuaClassMember>, deep: Boolean = true, processedList:MutableSet<String>): Boolean {
            if(!processedList.add(className))
                return false
            val key = "$className**$fieldName"
            val classDef = LuaClassIndex.find(className, context)
            if (classDef != null) {
                val type = classDef.type
                //先检查继承数据的字段
                val notFound = TyClass.processSuperClass(type, context) {
                    processPureField(it.className, fieldName, context, processor)
                }
                if(!notFound) return false
            }
            if (!process(key, context, processor))
                return false
            if (deep) {
                if (classDef != null) {
                    val type = classDef.type
                    // from alias
                    type.lazyInit(context)
                    val notFound = type.processAlias {
                        process(it, fieldName, context, processor, false, processedList)
                    }
                    if (!notFound)
                        return false

                    // from supper
                    return TyClass.processSuperClass(type, context) {
                        process(it.className, fieldName, context, processor, false, processedList)
                    }
                }
            }

            return true
        }

        private fun find(type: ITyClass, fieldName: String, context: SearchContext): LuaClassMember? {
            var perfect: LuaClassMember? = null
            var tagField: LuaDocTagField? = null
            var tableField: LuaTableField? = null
            processAll(type, fieldName, context, Processor {
                when (it) {
                    is LuaDocTagField -> {
                        tagField = it
                        false
                    }

                    is LuaTableField -> {
                        tableField = it
                        true
                    }

                    else -> {
                        if (perfect == null)
                            perfect = it
                        true
                    }
                }
            })
            if (tagField != null) return tagField
            if (tableField != null) return tableField
            return perfect
        }

        private fun processAll(type: ITyClass, fieldName: String, context: SearchContext, processor: Processor<LuaClassMember>): Boolean {
            return if (type is TyParameter)
            {
                var result = true
                if(type.superClassNames.isNotEmpty()){
                    type.superClassNames.forEach {
                        result = result && process(it, fieldName, context, processor, true, mutableSetOf())
                    }
                }
                return result
            }
            else process(type.className, fieldName, context, processor, true, mutableSetOf())
        }

        fun processAll(type: ITyClass, context: SearchContext, processor: Processor<LuaClassMember>): Boolean {
            if (process(type.className, context, processor)) {
                type.lazyInit(context)
                return type.processAlias {
                    process(it, context, processor)
                }
            }
            return true
        }

        private fun findMethod(className: String, memberName: String, context: SearchContext, deep: Boolean = true): LuaClassMethod? {
            var target: LuaClassMethod? = null
            process(className, memberName, context, Processor {
                if (it is LuaClassMethod) {
                    target = it
                    return@Processor false
                }
                true
            }, deep, mutableSetOf())
            return target
        }

        fun indexStub(indexSink: IndexSink, className: String, memberName: String) {
            indexSink.occurrence(StubKeys.CLASS_MEMBER, className.hashCode())
            val hashCode = "$className**$memberName".hashCode()
            indexSink.occurrence(StubKeys.CLASS_MEMBER, hashCode)
        }
    }
}
