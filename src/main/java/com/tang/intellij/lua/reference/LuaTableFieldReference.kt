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

package com.tang.intellij.lua.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.Processor
import com.tang.intellij.lua.lang.type.LuaString
import com.tang.intellij.lua.psi.LuaAssignStat
import com.tang.intellij.lua.psi.LuaElementFactory
import com.tang.intellij.lua.psi.LuaExprList
import com.tang.intellij.lua.psi.LuaLiteralExpr
import com.tang.intellij.lua.psi.LuaLiteralKind
import com.tang.intellij.lua.psi.LuaLocalDef
import com.tang.intellij.lua.psi.LuaTableExpr
import com.tang.intellij.lua.psi.LuaTableField
import com.tang.intellij.lua.psi.docTy
import com.tang.intellij.lua.psi.exprStubList
import com.tang.intellij.lua.psi.kind
import com.tang.intellij.lua.psi.ty
import com.tang.intellij.lua.psi.valueExpr
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.Ty

class LuaTableFieldReference internal constructor(element: LuaTableField)
    : PsiReferenceBase<LuaTableField>(element), LuaReference {

    private val id: PsiElement = element.nameIdentifier!!

    override fun getRangeInElement(): TextRange {
        var start = id.node.startOffset - myElement.node.startOffset
        val length = if (id is LuaLiteralExpr && id.kind == LuaLiteralKind.String) {
            val content = LuaString.getContent(id.text)
            start += content.start
            content.length
        } else id.textLength
        return TextRange(start, start + length)
    }

    @Throws(IncorrectOperationException::class)
    override fun handleElementRename(newElementName: String): PsiElement {
        if (id is LuaLiteralExpr && id.kind == LuaLiteralKind.String) {
            val content = LuaString.getContent(id.text)
            val text = id.text
            val newText = text.substring(0, content.start) + newElementName + text.substring(content.end)
            val newId = LuaElementFactory.createLiteral(myElement.project, newText)
            id.replace(newId)
            return newId
        }
        val newId = LuaElementFactory.createIdentifier(myElement.project, newElementName)
        id.replace(newId)
        return newId
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        return myElement.manager.areElementsEquivalent(resolve(), element)
    }

    override fun resolve(): PsiElement? {
        return resolve(SearchContext.get(myElement.project))
    }

    override fun resolve(context: SearchContext): PsiElement? {
        val name = myElement.name ?: return null
        val type = guessContainingTableType(myElement, context)
        var ret: PsiElement? = null
        type.eachTopClass(Processor { ty ->
            ret = ty.findMember(name, context)
            if (ret != null) false else true
        })
        if (ret != null && ret!!.containingFile == myElement.containingFile && ret!!.node.textRange == myElement.node.textRange) {
            return null
        }
        return ret
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

private fun guessContainingTableType(field: LuaTableField, context: SearchContext): ITy {
    val table = PsiTreeUtil.getParentOfType(field, LuaTableExpr::class.java) ?: return Ty.UNKNOWN
    val parent = PsiTreeUtil.getParentOfType(table, LuaExprList::class.java)
    if (parent != null) {
        val localDef = parent.parent as? LuaLocalDef
        if (localDef != null) {
            val index = parent.exprStubList.indexOfFirst { PsiTreeUtil.isAncestor(it, table, false) }.takeIf { it >= 0 } ?: 0
            val nameDef = localDef.nameList?.nameDefList?.getOrNull(index)
            val docTy = nameDef?.docTy ?: localDef.comment?.ty
            if (docTy != null)
                return docTy
        }

        val assignStat = parent.parent as? LuaAssignStat
        if (assignStat != null) {
            val index = parent.exprStubList.indexOfFirst { PsiTreeUtil.isAncestor(it, table, false) }.takeIf { it >= 0 } ?: 0
            val target = assignStat.varExprList.exprStubList.getOrNull(index)
            val docTy = target?.guessType(context) ?: assignStat.comment?.docTy ?: Ty.UNKNOWN
            if (!Ty.isInvalid(docTy))
                return docTy
        }
    }

    val ownerField = table.parent as? LuaTableField
    if (ownerField != null && ownerField.valueExpr == table)
        return ownerField.guessType(context)

    return Ty.UNKNOWN
}
