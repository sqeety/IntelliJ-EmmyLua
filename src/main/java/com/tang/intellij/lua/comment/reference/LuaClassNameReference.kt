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

package com.tang.intellij.lua.comment.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.*
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.LuaClassMethodName
import com.tang.intellij.lua.psi.LuaElementFactory
import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext

/**

 * Created by TangZX on 2016/11/29.
 */
class LuaClassNameReference(element: LuaDocClassNameRef) : PsiReferenceBase<LuaDocClassNameRef>(element) {

    override fun getRangeInElement() = TextRange(0, myElement.textLength)

    override fun isReferenceTo(element: PsiElement): Boolean {
        return myElement.manager.areElementsEquivalent(element, resolve())
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val element = LuaElementFactory.createWith(myElement.project, "---@type $newElementName")
        val classNameRef = PsiTreeUtil.findChildOfType(element, LuaDocClassNameRef::class.java)
        return myElement.replace(classNameRef!!)
    }

    fun getGenericClass(findName:String, comment:LuaComment):Pair<Boolean, LuaDocGenericParameter?> {
        //class field
        val children = comment.children
        for (child in children) {
            if (child is LuaDocTagClass) {
                val parameterList = child.genericParameters?.genericParameterList;
                if (!parameterList.isNullOrEmpty()) {
                    for (type in parameterList) {
                        if (type.name == findName) {
                            return Pair(true, type)
                        }
                    }
                }
            }
        }
        return Pair(false, null)
    }
    fun getGenericClassDefine(findName:String):Pair<Boolean, LuaDocGenericParameter?>{
        //class field
        var comment = PsiTreeUtil.getParentOfType(myElement, LuaComment::class.java)
        if(comment != null){
            val result = getGenericClass(findName, comment)
            if(result.first){
                return result
            }
        }

        val methodDef = PsiTreeUtil.getParentOfType(myElement, LuaClassMethodDef::class.java)
        if(methodDef != null){
            for (child in methodDef.children){
                if(child is LuaClassMethodName){
                    val firstChild = child.firstChild
                    if(firstChild is LuaNameExpr){
                        comment = LuaCommentUtil.findMethodClassComment(firstChild)
                        if(comment != null){
                            val result = getGenericClass(findName, comment)
                            if(result.first){
                                return result
                            }
                        }
                    }
                    break
                }
            }
//            for (child in fn.children){
//                if(child is LuaLocalDef){
//                    for (localChild in child.children)
//                        if(localChild is LuaComment){
//                            val result = getGenericClass(findName, localChild)
//                            if(result.first){
//                                return result
//                            }
//                        }
//                }
//            }
        }
        return Pair(false, null)
    }

    override fun resolve(): PsiElement? {
        val name = myElement.text
        // generic in docFunction
        val fn = PsiTreeUtil.getParentOfType(myElement, LuaDocFunctionTy::class.java)
        var genericDefList: Collection<LuaDocGenericDef>? = fn?.genericDefList
        if (genericDefList == null || genericDefList.isEmpty()) {
            // generic in comments ?
            val comment = LuaCommentUtil.findComment(myElement)
            if(comment != null){
                genericDefList = comment.findTags(LuaDocGenericDef::class.java)
            }
        }

        if (genericDefList != null) {
            for (genericDef in genericDefList) {
                if (genericDef.name == name)
                    return genericDef
            }
        }

        val findPair = getGenericClassDefine(name)
        if(findPair.first){
            return findPair.second
        }
        return LuaShortNamesManager.getInstance(myElement.project).findTypeDef(name, SearchContext.get(myElement.project))
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
