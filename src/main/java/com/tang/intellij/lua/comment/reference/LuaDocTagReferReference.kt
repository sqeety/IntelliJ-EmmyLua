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
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.comment.psi.LuaDocTagRefer
import com.tang.intellij.lua.psi.LuaElementFactory
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITyClass

class LuaDocTagReferReference(refer: LuaDocTagRefer) :
    PsiPolyVariantReferenceBase<LuaDocTagRefer>(refer){

    val id = refer.id!!

    override fun getRangeInElement(): TextRange {
        val start = id.node.startOffset - myElement.node.startOffset
        return TextRange(start, start + id.textLength)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val id = LuaElementFactory.createDocIdentifier(myElement.project, newElementName)
        this.id.replace(id)
        return id
    }

    override fun getVariants(): Array<Any> = emptyArray()

    override fun multiResolve(incomplete: Boolean): Array<ResolveResult> {
        val list = mutableListOf<ResolveResult>()
        val nameRef = myElement.classOrGlobalNameRef
        if(nameRef != null){
            val refName = nameRef.name
            val context = SearchContext.get(myElement.project)
            if(refName != null){
                val type = LuaShortNamesManager.getInstance(myElement.project)
                    .findTypeDef(refName, SearchContext.get(myElement.project))
                if (type == null || type.name == Constants.WORD_G) {
                    if(refName != Constants.WORD_G){
                        LuaShortNamesManager.getInstance(myElement.project)
                            .processMembers("$$refName", id.text, context, {
                                list.add(PsiElementResolveResult(it))
                                false
                            })
                    }else{
                        LuaShortNamesManager.getInstance(myElement.project)
                            .processMembers(refName, id.text, context, {
                                list.add(PsiElementResolveResult(it))
                                false
                            })
                    }
                } else {
                    var ty:ITyClass? = null

                    if(type is ITyClass){
                        ty = type
                    }else if(type is LuaDocTagClass){
                        ty = type.type
                    }
                    if(ty != null){
                        LuaShortNamesManager.getInstance(myElement.project)
                            .processMembers(ty, id.text, context) {
                                list.add(PsiElementResolveResult(it))
                                true
                            }
                    }
                }
            }
        }

        return list.toTypedArray()
    }
}