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
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.Processor
import com.intellij.util.io.StringRef
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.psi.LuaDocTableDef
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.ext.PerformanceUtil
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.search.LuaClassInheritorsSearch
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.readNames
import com.tang.intellij.lua.stubs.writeNames
import java.util.HashSet

interface ITyClass : ITy {
    val className: String
    val varName: String
    val genericNames: Array<String>
    var superClassNames: Array<String>
    var aliasName: String?
    fun processAlias(processor: Processor<String>): Boolean
    fun lazyInit(searchContext: SearchContext)
    fun getMemberChain(context: SearchContext, alreadyProcessed: HashSet<ITy>): ClassMemberChain
    fun processMembers(context: SearchContext, processor: (ITyClass, LuaClassMember) -> Unit, deep: Boolean = true)
    fun processMembers(context: SearchContext, processor: (ITyClass, LuaClassMember) -> Unit) {
        processMembers(context, processor, true)
    }
    fun findMember(name: String, searchContext: SearchContext): LuaClassMember?
    fun findMemberType(name: String, searchContext: SearchContext): ITy? {
        return infer(findMember(name, searchContext), searchContext)
    }
    fun findSuperMember(name: String, searchContext: SearchContext): LuaClassMember?

    fun recoverAlias(context: SearchContext, aliasSubstitutor: TyAliasSubstitutor): ITy {
        return this
    }
}

fun ITyClass.isVisibleInScope(project: Project, contextTy: ITy, visibility: Visibility): Boolean {
    if (visibility == Visibility.PUBLIC)
        return true
    var isVisible = false
    TyUnion.process(contextTy) {
        if (it is ITyClass) {
            if (it == this)
                isVisible = true
            else if (visibility == Visibility.PROTECTED) {
                isVisible = LuaClassInheritorsSearch.isClassInheritFrom(GlobalSearchScope.projectScope(project), project, className, it.className)
            }
        }
        !isVisible
    }
    return isVisible
}

abstract class TyClass(override val className: String,
                       override var genericNames: Array<String> = emptyArray(),
                       override val varName: String = "",
                       override var superClassNames: Array<String> = emptyArray(),
) : Ty(TyKind.Class), ITyClass {

    final override var aliasName: String? = null

    private var _lazyInitialized: Boolean = false

    override fun equals(other: Any?): Boolean {
        return other is ITyClass && other.className == className && other.flags == flags
    }

    override fun hashCode(): Int {
        return className.hashCode()
    }

    override fun processAlias(processor: Processor<String>): Boolean {
        val alias = aliasName
        if (alias == null || alias == className)
            return true
        if (!processor.process(alias))
            return false
        if (!isGlobal && !isAnonymous && LuaSettings.instance.isRecognizeGlobalNameAsType)
            return processor.process(getGlobalTypeName(className))
        return true
    }

    override fun getMemberChain(context: SearchContext, alreadyProcessed: HashSet<ITy>): ClassMemberChain {
        val superClazz = getSuperClass(context)
        val array: Array<ClassMemberChain>
        if (superClazz != null) {
            if (alreadyProcessed.contains(superClazz)) {
                return ClassMemberChain(this, emptyArray())
            }
            alreadyProcessed.add(superClazz)
            if (superClazz is ITyClass) {
                array = arrayOf(superClazz.getMemberChain(context, alreadyProcessed))
            } else if (superClazz is TyUnion) {
                val multiArray = mutableListOf<ClassMemberChain>()
                for (childType in superClazz.getChildTypes()) {
                    if (childType is ITyClass)
                        multiArray.add(childType.getMemberChain(context, alreadyProcessed))
                }
                array = multiArray.toTypedArray()
            } else {
                array = emptyArray()
            }
        } else {
            array = emptyArray()
        }
        val chain = ClassMemberChain(this, array)
        val manager = LuaShortNamesManager.getInstance(context.project)
        val members: Collection<LuaClassMember> = manager.getClassMembers(className, context)
        members.forEach { chain.add(it) }
        processAlias { alias ->
            val classMembers = manager.getClassMembers(alias, context)
            classMembers.forEach { chain.add(it) }
            true
        }
        return chain
    }

    override fun processMembers(context: SearchContext, processor: (ITyClass, LuaClassMember) -> Unit, deep: Boolean) {
        val chain: ClassMemberChain = getMemberChain(context, HashSet())
        chain.process(deep, processor)
    }

    override fun findMember(name: String, searchContext: SearchContext): LuaClassMember? {
        val chain = getMemberChain(searchContext, HashSet())
        return chain.findMember(name)
    }

    override fun findSuperMember(name: String, searchContext: SearchContext): LuaClassMember? {
        val chain = getMemberChain(searchContext, HashSet())
        return chain.findSuperMember(name)
    }

    override fun accept(visitor: ITyVisitor) {
        visitor.visitClass(this)
    }

    override fun lazyInit(searchContext: SearchContext) {
        if (!_lazyInitialized) {
            _lazyInitialized = true
            doLazyInit(searchContext)
        }
    }

    open fun doLazyInit(searchContext: SearchContext) {
        val classDef = LuaShortNamesManager.getInstance(searchContext.project).findClass(className, searchContext)
        if (classDef != null && aliasName == null) {
            val tyClass = classDef.type
            aliasName = tyClass.aliasName
            superClassNames = tyClass.superClassNames
            genericNames = tyClass.genericNames
        }
    }

    override fun getSuperClass(context: SearchContext): ITy? {
        lazyInit(context)
        val clsNames = superClassNames
        if(clsNames.isNotEmpty()){
            var clsName = clsNames[0]
            var result = getBuiltin(clsName) ?: LuaShortNamesManager.getInstance(context.project).findClass(clsName, context)?.type
            for (i in 1 until clsNames.size) {
                clsName = clsNames[i]
                val ty = getBuiltin(clsName) ?: LuaShortNamesManager.getInstance(context.project).findClass(clsName, context)?.type
                if(ty != null){
                    if(result == null){
                        result = ty
                    }else{
                        result = result.union(ty)
                    }
                }
            }
            return result
        }
        return null
    }

    override fun subTypeOf(other: ITy, context: SearchContext, strict: Boolean): Boolean {
        // class extends table
        if (other == TABLE) return true
        if (super.subTypeOf(other, context, strict)) return true
        if(other is ITyGeneric)
        {
            return subTypeOf(other.base, context, strict)
        }
        // Lazy init for superclass
        this.doLazyInit(context)
        // Check if any of the superclasses are type
        return !processSuperClass(this, context) { superType ->
            superType != other
        }
    }

    override fun substitute(substitutor: ITySubstitutor, context: SearchContext): ITy {
        return substitutor.substitute(this, context)
    }

    companion object {
        /*
         * WARNING: Risk of classloading deadlock
         *
         * Calling `createSerializedClass` uses the type `TyClass`.
         * However, using the type `TyClass` requires all its static fields
         * to be initialized. So the JVM can't run `createSerializedClass` without
         * having `TyClass`, and it can't use `TyClass` without running `createSerializedClass`.
         * Thus the JVM deadlocks during classloading, resulting in frozen indexing...
         * 
         * Workaround this by using Kotlin lazy properties,
         * so `createSerializedClass` is not run until TyClass.G is actually accessed.
         *
         * See issue #510 and Ty.kt for more info on this bug.
         * -- Techcable
         */

        // for _G
        val G: TyClass by lazy { createSerializedClass(Constants.WORD_G) }

        fun createAnonymousType(nameDef: LuaNameDef): TyClass {
            val stub = nameDef.stub
            val tyName = stub?.anonymousType ?: getAnonymousType(nameDef)
            return createSerializedClass(tyName, emptyArray(), nameDef.name, emptyArray(), null, TyFlags.ANONYMOUS)
        }

        fun createGlobalType(nameExpr: LuaNameExpr, store: Boolean): ITy {
            val name = nameExpr.name
            val g = createSerializedClass(getGlobalTypeName(nameExpr), emptyArray(),name, emptyArray(), null, TyFlags.GLOBAL)
            if (!store && LuaSettings.instance.isRecognizeGlobalNameAsType)
                return createSerializedClass(name, emptyArray(), name, emptyArray(), null, TyFlags.GLOBAL).union(g)
            return g
        }

        fun createGlobalType(name: String): ITy {
            val g = createSerializedClass(getGlobalTypeName(name), emptyArray(), name, emptyArray(), null, TyFlags.GLOBAL)
            if (LuaSettings.instance.isRecognizeGlobalNameAsType)
                return createSerializedClass(name, emptyArray(), name, emptyArray(), null, TyFlags.GLOBAL).union(g)
            return g
        }

        fun processSuperClass(start: ITyClass, searchContext: SearchContext, processor: (ITyClass) -> Boolean): Boolean {
            val processedName = mutableSetOf<String>()
            var cur: ITy? = start
            while (cur != null) {
                val cls = cur.getSuperClass(searchContext)
                if (cls is ITyClass) {
                    if (!processedName.add(cls.className)) {
                        // todo: Infinite inheritance
                        return true
                    }
                    if (!processor(cls))
                        return false
                    cur = cls
                    continue
                } else if (cls is TyUnion) {
                    for (childType in cls.getChildTypes()) {
                        if (childType is ITyClass) {
                            if (!processedName.add(childType.className)) {
                                return false
                            }
                            if (!processor(childType)) {
                                return false
                            }
                            if(!processSuperClass(childType, searchContext, processor)){
                                return false
                            }
                        }
                    }
                }
                break
            }
            return true
        }
    }
}

class TyPsiDocClass(tagClass: LuaDocTagClass) : TyClass(tagClass.name) {

    init {
        val supperRef = tagClass.superClassNameRef
        if (supperRef != null)
        {
            val supperNames = mutableListOf<String>()
            if(supperRef.classNameRefList.isNotEmpty()){
                supperRef.classNameRefList.forEach {
                    val genericName = it.name
                    if(genericName != null) {
                        supperNames.add(genericName)
                    }
                }
            }
            superClassNames = supperNames.toTypedArray()
        }else{
            superClassNames = emptyArray()
        }
        val genericTypes = mutableListOf<String>()
        val genericParameters = tagClass.genericParameters
        if(genericParameters != null){
            if(genericParameters.genericParameterList.isNotEmpty()){
                genericParameters.genericParameterList.forEach {
                    val genericName = it.name
                    if(genericName != null) {
                        genericTypes.add(genericName)
                    }
                }
            }
        }
        genericNames = genericTypes.toTypedArray()
        aliasName = tagClass.aliasName
    }

    override fun doLazyInit(searchContext: SearchContext) {}
}

open class TySerializedClass(name: String,
                             genericNames: Array<String> = emptyArray(),
                             varName: String = name,
                             supper: Array<String> = emptyArray(),
                             alias: String? = null,
                             flags: Int = 0)
    : TyClass(name, genericNames, varName, supper) {
    init {
        aliasName = alias
        this.flags = flags
    }

    override fun recoverAlias(context: SearchContext, aliasSubstitutor: TyAliasSubstitutor): ITy {
        if (this.isAnonymous || this.isGlobal)
            return this
        val alias = LuaShortNamesManager.getInstance(context.project).findAlias(className, context)
        return alias?.type?.substitute(aliasSubstitutor, context) ?: this
    }
}

//todo Lazy class ty
class TyLazyClass(name: String) : TySerializedClass(name)

fun createSerializedClass(name: String,
                          genericNames: Array<String> = emptyArray(),
                          varName: String = name,
                          supper: Array<String> = emptyArray(),
                          alias: String? = null,
                          flags: Int = 0): TyClass {
    val list = name.split("|")
    if (list.size == 3) {
        val type = list[0].toInt()
        if (type == 10) {
            return TySerializedDocTable(name)
        }
    }

    return TySerializedClass(name, genericNames, varName, supper, alias, flags)
}

private val PsiFile.uid: String get() {
    if (this is LuaPsiFile)
        return this.uid

    return name
}

fun getTableTypeName(table: LuaTableExpr): String {
    val stub = table.stub
    if (stub != null)
        return stub.tableTypeName

    val fileName = table.containingFile.uid
    return "$fileName@(${table.node.startOffset})table"
}

fun getAnonymousType(nameDef: LuaNameDef): String {
    return "${nameDef.node.startOffset}@${nameDef.containingFile.uid}"
}

fun getGlobalTypeName(text: String): String {
    return if (text == Constants.WORD_G) text else "$$text"
}

fun getGlobalTypeName(nameExpr: LuaNameExpr): String {
    return getGlobalTypeName(nameExpr.name)
}

class TyTable(val table: LuaTableExpr) : TyClass(getTableTypeName(table)) {
    init {
        this.flags = TyFlags.ANONYMOUS or TyFlags.ANONYMOUS_TABLE
    }

    override fun processMembers(context: SearchContext, processor: (ITyClass, LuaClassMember) -> Unit, deep: Boolean) {
        for (field in table.tableFieldList) {
            processor(this, field)
        }
        super.processMembers(context, processor, deep)
    }

    override fun toString(): String = displayName

    override fun doLazyInit(searchContext: SearchContext) = Unit

    override fun subTypeOf(other: ITy, context: SearchContext, strict: Boolean): Boolean {
        // Empty list is a table, but subtype of all lists
        return super.subTypeOf(other, context, strict) || other == TABLE || (other is TyArray && table.tableFieldList.size == 0)
    }
}

fun getDocTableTypeName(table: LuaDocTableDef): String {
    val stub = table.stub
    if (stub != null)
        return stub.className

    val fileName = table.containingFile.uid
    return "10|$fileName|${table.node.startOffset}"
}

class TyDocTable(val table: LuaDocTableDef) : TyClass(getDocTableTypeName(table)) {
    override fun doLazyInit(searchContext: SearchContext) {}

    override fun processMembers(context: SearchContext, processor: (ITyClass, LuaClassMember) -> Unit, deep: Boolean) {
        table.tableFieldList.forEach {
            processor(this, it)
        }
    }

    override fun findMember(name: String, searchContext: SearchContext): LuaClassMember? {
        return table.tableFieldList.firstOrNull { it.name == name }
    }
}

class TySerializedDocTable(name: String) : TySerializedClass(name) {
    override fun recoverAlias(context: SearchContext, aliasSubstitutor: TyAliasSubstitutor): ITy {
        return this
    }
}

object TyClassSerializer : TySerializer<ITyClass>() {

    override fun deserializeTy(flags: Int, stream: StubInputStream): ITyClass {
        val className = stream.readName()
        val genericNames = stream.readNames()
        val varName = stream.readName()
        val superNames = stream.readNames()
        val aliasName = stream.readName()
        return createSerializedClass(StringRef.toString(className),
                genericNames,
                StringRef.toString(varName),
                superNames,
                StringRef.toString(aliasName),
                flags)
    }

    override fun serializeTy(ty: ITyClass, stream: StubOutputStream) {
        stream.writeName(ty.className)
        stream.writeNames(ty.genericNames)
        stream.writeName(ty.varName)
        stream.writeNames(ty.superClassNames)
        stream.writeName(ty.aliasName)
    }
}
