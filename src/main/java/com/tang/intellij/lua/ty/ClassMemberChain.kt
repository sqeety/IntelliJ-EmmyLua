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

import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.comment.psi.impl.LuaCommentImpl
import com.tang.intellij.lua.comment.psi.impl.LuaDocTagClassImpl
import com.tang.intellij.lua.ext.stubOrPsiParent
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.impl.LuaClassMethodDefImpl
import com.tang.intellij.lua.psi.impl.LuaClassMethodNameImpl
import com.tang.intellij.lua.psi.impl.LuaLocalDefImpl
import com.tang.intellij.lua.psi.impl.LuaNameExprImpl
import kotlinx.html.Entities

class ClassMemberChain(val ty: ITyClass, var superChain: Array<ClassMemberChain>) {
    private val members = mutableMapOf<String, LuaClassMember>()
    private val builtinMembers = mutableSetOf<String>()
    fun add(member: LuaClassMember) {
        val name = member.name ?: return
        val superExist = findSuperMember(name)
        val override = superExist == null || canOverride(member, superExist)
        if (override) {
            val selfExist = members[name]
            if (selfExist == null)
            {
                members[name] = member
                if(isClassDefineMember(member)){
                    builtinMembers.add(name)
                }
            }
            else{
                if(member.worth > selfExist.worth){
                    members[name] = member
                }
                if(!builtinMembers.contains(name)){
                    if(isClassDefineMember(member)){
                        builtinMembers.add(name)
                        members[name] = member
                    }
                }
            }
        }
    }

    private fun isClassDefineMember(member: LuaClassMember):Boolean{
        val className = getClassDefineName(member)
        var prev = member.prevSibling
        while(prev != null) {
            if(prev is LuaLocalDef) {
                val localName = prev.nameList?.firstChild?.text
                if(localName == className){
                    prev.children.forEach { psiElement ->
                        if(psiElement is LuaCommentImpl) {
                            val docTag = psiElement.findTag(LuaDocTagClass::class.java)
                            if (docTag != null) {
                                val type = docTag.type
                                if(type.className == ty.className) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false
                }
            }
            prev = prev.prevSibling
        }
        return false
    }

    private fun getClassDefineName(member: LuaClassMember):String?{
        if(member is LuaClassMethodDefImpl){
            val luaClassMethodName = member.children.find { it is LuaClassMethodNameImpl }
            val luaName = luaClassMethodName?.firstChild
            if (luaName is LuaNameExprImpl) {
                return luaName.text
            }
        }
        return null
    }

    fun findSuperMember(name: String): LuaClassMember? {
        if(superChain.isNotEmpty()){
            for (classMemberChain in superChain) {
                val find = classMemberChain.findMember(name)
                if(find != null){
                    return find
                }
            }
        }
       return null
    }

    fun findMember(name: String): LuaClassMember? {
        return members.getOrElse(name) { findSuperMember(name) }
    }

    private fun superProcess(deep: Boolean, processor: (ITyClass, String, LuaClassMember) -> Unit) {
        if(superChain.isNotEmpty()){
            for (classMemberChain in superChain) {
                classMemberChain.process(deep, processor)
            }
        }
    }
    private fun process(deep: Boolean, processor: (ITyClass, String, LuaClassMember) -> Unit) {
        for ((t, u) in members) {
            processor(ty, t, u)
        }
        if (deep)
            superProcess(true, processor)
    }

    fun process(deep: Boolean, processor: (ITyClass, LuaClassMember) -> Unit) {
        val cache = mutableSetOf<String>()
        process(deep) { clazz, name, member ->
            if (cache.add(name))
                processor(clazz, member)
        }
    }

    private fun canOverride(member: LuaClassMember, superMember: LuaClassMember): Boolean {
        return member.worth > superMember.worth || (member.worth == superMember.worth && member.worth > LuaClassMember.WORTH_ASSIGN)
    }
}