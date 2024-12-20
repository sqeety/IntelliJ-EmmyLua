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

package com.tang.intellij.lua.comment

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import com.intellij.refactoring.suggested.startOffset
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.comment.psi.api.LuaComment
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.impl.LuaLocalDefImpl
import com.tang.intellij.lua.search.SearchContext
import org.mozilla.javascript.ast.StringLiteral

/**
 *
 * Created by TangZX on 2016/11/24.
 */
object LuaCommentUtil {

    fun findOwner(element: LuaDocPsiElement): LuaCommentOwner? {
        val comment = findContainer(element)
        return if (comment.parent is LuaCommentOwner) comment.parent as LuaCommentOwner else null
    }

    fun findContainer(psi: LuaDocPsiElement): LuaComment {
        var element = psi
        while (true) {
            if (element is LuaComment) {
                return element
            }
            element = element.parent as LuaDocPsiElement
        }
    }

    fun findComment(element: LuaCommentOwner): LuaComment? {
        val comment = PsiTreeUtil.getChildOfType(element, LuaComment::class.java)
        if(element is LuaLocalDefImpl){
            if(comment != null){
                if(comment.tagClass != null)
                    return comment
            }
            var prevElement = element.prevSibling
            while (prevElement != null) {
                prevElement = when (prevElement) {
                    is LuaComment -> {
                        if(prevElement.tagClass != null)
                            return prevElement
                        prevElement.prevSibling
                    }

                    is PsiComment->{
                        prevElement.prevSibling
                    }

                    is PsiWhiteSpace -> {
                        prevElement.prevSibling
                    }

                    else -> {
                        break
                    }
                }
            }
        }
        return comment
    }

    fun insertTemplate(commentOwner: LuaCommentOwner, editor: Editor, action:(TemplateManager, Template) -> Unit) {
        val comment = commentOwner.comment
        val project = commentOwner.project

        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "")
        if (comment != null)
            template.addTextSegment("\n")

        action(templateManager, template)
        //template.addTextSegment(String.format("---@param %s ", parDef.name))
        //val name = MacroCallNode(SuggestTypeMacro())
        //template.addVariable("type", name, TextExpression("table"), true)
        //template.addEndVariable()

        if (comment != null) {
            editor.caretModel.moveToOffset(comment.textOffset + comment.textLength)
        } else {
            editor.caretModel.moveToOffset(commentOwner.node.startOffset)
            template.addTextSegment("\n")
        }

        templateManager.startTemplate(editor, template)
    }

    fun findComment(psi: PsiElement): LuaComment? {
        return PsiTreeUtil.getParentOfType(psi, LuaComment::class.java)
    }
    fun findMethodClassComment(psi: LuaNameExpr): LuaComment? {
        val findTypeName = psi.name
        val file = psi.containingFile
        var children = file.children
        for (i in children.size - 1 downTo 0 step 1) {
            val child = children[i]
            if(child.textOffset < psi.textOffset){
                if(child is LuaLocalDef){
                    if(getLocalDefName(child) == findTypeName){
                        return getLocalDefComment(child)
                    }
                }
            }
        }
        return PsiTreeUtil.getParentOfType(psi, LuaComment::class.java)
    }

    private fun getLocalDefComment(def: LuaLocalDef): LuaComment? {
        val children = def.children
        for (child in children) {
            if (child is LuaComment) {
                return child
            }
        }
        return null
    }

    private fun getLocalDefName(def: LuaLocalDef): String? {
        val children = def.children
        for (child in children) {
            if (child is LuaNameList) {
                val defList = child.nameDefList
                if(defList.size > 0)
                    return defList[0].name;
            }
        }
        return null
    }

    fun isComment(psi: PsiElement): Boolean {
        return findComment(psi) != null
    }

    fun getLuaTableFieldValue(tableField: LuaTableField): String {
        val exprList = tableField.exprList
        if (exprList.size == 1) {
            val expr = exprList[0]
            when (expr) {
                is LuaLiteralExpr -> {
                    return expr.text
                }
            }
        }
        return ""
    }

    fun getPsiLineNumber(element: PsiElement): Number {
        val file = element.containingFile
        if (file == null) return -1
        val doc = file.fileDocument
        if (doc == null) return -1

        return doc.getLineNumber(element.textOffset)
    }

    fun isCommentLineHasOtherPsi(comment: PsiComment): Boolean {
        var prevElement = comment.prevSibling
        var checkLine = getPsiLineNumber(comment)
        while (prevElement != null) {
            prevElement = when (prevElement) {
                is PsiWhiteSpace -> {
                    prevElement.prevSibling
                }

                else -> {
                    if (checkLine == getPsiLineNumber(prevElement)) {
                        return true
                    }
                    break
                }
            }
        }
        return false
    }

    fun getCommentStr(tableField: LuaTableField): String? {
        val luaComment = tableField.comment
        if (luaComment != null) {
            return luaComment.text.trimStart('-')
        }
        var prevElement = tableField.prevSibling
        while (prevElement != null) {
            prevElement = when (prevElement) {
                is PsiComment -> {
                    if (!isCommentLineHasOtherPsi(prevElement)) {
                        return prevElement.text.trimStart('-')
                    }
                    break
                }

                is PsiWhiteSpace -> {
                    prevElement.prevSibling
                }

                else -> {
                    break
                }
            }
        }
        var nextElement = tableField.nextSibling
        while (nextElement != null) {
            nextElement = when (nextElement) {
                is PsiComment -> {
                    return nextElement.text.trimStart('-')
                }

                is PsiWhiteSpace -> {
                    nextElement.nextSibling
                }

                is LuaTableFieldSep -> {
                    nextElement.nextSibling
                }
                else -> {
                    break
                }
            }
        }
        return null
    }
}
