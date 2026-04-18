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

import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.MacroCallNode
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import com.intellij.refactoring.suggested.startOffset
import com.tang.intellij.lua.codeInsight.template.macro.SuggestTypeMacro
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.comment.psi.LuaDocTagParam
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
    private data class AnnotationInsertion(val offset: Int, val prefix: String = "", val suffix: String = "")

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

    fun findEditor(element: PsiElement): Editor? {
        val project = element.project
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        return if (psiFile == element.containingFile) editor else null
    }

    fun insertEditableTypeAnnotation(localDef: LuaLocalDef, editor: Editor, defaultType: String = "table") {
        insertEditableTypeTag(localDef, editor, defaultType)
    }

    fun insertEditableTypeTag(commentOwner: LuaCommentOwner, editor: Editor, defaultType: String = "table") {
        insertTemplate(commentOwner, editor) { _, template ->
            template.addTextSegment("---@type ")
            val typeSuggest = MacroCallNode(SuggestTypeMacro())
            template.addVariable("type", typeSuggest, TextExpression(defaultType), true)
            template.addEndVariable()
        }
    }

    fun insertTypeAnnotation(localDef: LuaLocalDef, typeText: String) {
        insertTypeTag(localDef, typeText)
    }

    fun insertTypeTag(commentOwner: LuaCommentOwner, typeText: String) {
        val insertion = if (commentOwner.comment != null) {
            AnnotationInsertion(commentOwner.comment!!.textRange.endOffset, prefix = "\n")
        } else {
            AnnotationInsertion(commentOwner.node.startOffset, suffix = "\n")
        }
        insertResolvedText(commentOwner, insertion, "---@type $typeText")
    }

    fun insertParamAnnotation(commentOwner: LuaCommentOwner, paramName: String, typeText: String) {
        val insertion = getParamInsertion(commentOwner, paramName)
        insertResolvedText(commentOwner, insertion, "---@param $paramName $typeText")
    }

    fun insertReturnAnnotation(commentOwner: LuaCommentOwner, typeText: String) {
        val insertion = getReturnInsertion(commentOwner)
        insertResolvedText(commentOwner, insertion, "---@return $typeText")
    }

    fun insertFieldAnnotation(comment: LuaComment, fieldName: String, typeText: String) {
        val insertion = getFieldInsertion(comment)
        insertResolvedText(comment, insertion, "---@field public $fieldName $typeText")
    }

    fun insertParameterTemplate(commentOwner: LuaCommentOwner, editor: Editor, paramName: String, defaultType: String = "table") {
        val insertion = getParamInsertion(commentOwner, paramName)
        insertTemplateAt(commentOwner, editor, insertion) { template ->
            template.addTextSegment("---@param $paramName ")
            template.addVariable("type", MacroCallNode(SuggestTypeMacro()), TextExpression(defaultType), true)
            template.addEndVariable()
        }
    }

    fun insertReturnTemplate(commentOwner: LuaCommentOwner, editor: Editor, defaultType: String = "table") {
        val insertion = getReturnInsertion(commentOwner)
        insertTemplateAt(commentOwner, editor, insertion) { template ->
            template.addTextSegment("---@return ")
            template.addVariable("returnType", MacroCallNode(SuggestTypeMacro()), TextExpression(defaultType), true)
            template.addEndVariable()
        }
    }

    fun insertFieldTemplate(
        comment: LuaComment,
        editor: Editor,
        fieldName: String,
        defaultType: String = "table",
        listener: TemplateEditingAdapter? = null
    ) {
        val insertion = getFieldInsertion(comment)
        insertTemplateAt(comment, editor, insertion, listener) { template ->
            template.addTextSegment("---@field public $fieldName ")
            template.addVariable("type", MacroCallNode(SuggestTypeMacro()), TextExpression(defaultType), true)
            template.addEndVariable()
            template.isToReformat = true
        }
    }

    private fun insertResolvedText(owner: PsiElement, insertion: AnnotationInsertion, text: String) {
        val documentManager = PsiDocumentManager.getInstance(owner.project)
        val document = documentManager.getDocument(owner.containingFile) ?: return
        document.insertString(insertion.offset, insertion.prefix + text + insertion.suffix)
        documentManager.commitDocument(document)
    }

    private fun insertTemplateAt(
        owner: PsiElement,
        editor: Editor,
        insertion: AnnotationInsertion,
        listener: TemplateEditingAdapter? = null,
        buildTemplate: (Template) -> Unit
    ) {
        val project = owner.project
        val documentManager = PsiDocumentManager.getInstance(project)
        val targetEditor = if (documentManager.getPsiFile(editor.document) == owner.containingFile) editor else findEditor(owner) ?: editor
        targetEditor.caretModel.moveToOffset(insertion.offset)

        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("", "")
        template.addTextSegment(insertion.prefix)
        buildTemplate(template)
        template.addTextSegment(insertion.suffix)
        if (listener != null) {
            templateManager.startTemplate(targetEditor, template, listener)
        } else {
            templateManager.startTemplate(targetEditor, template)
        }
    }

    private fun getParamInsertion(commentOwner: LuaCommentOwner, paramName: String): AnnotationInsertion {
        val comment = commentOwner.comment ?: return AnnotationInsertion(commentOwner.node.startOffset, suffix = "\n")
        val owner = commentOwner as? LuaFuncBodyOwner
        val params = owner?.funcBody?.paramNameDefList.orEmpty()
        val targetIndex = params.indexOfFirst { it.name == paramName }
        if (targetIndex >= 0) {
            for (index in targetIndex - 1 downTo 0) {
                val existing = comment.getParamDef(params[index].name)
                if (existing != null) {
                    return AnnotationInsertion(existing.textRange.endOffset, prefix = "\n")
                }
            }
        }

        val returnTag = comment.tagReturn
        if (returnTag != null) {
            return AnnotationInsertion(returnTag.textRange.startOffset, suffix = "\n")
        }
        return AnnotationInsertion(comment.textRange.endOffset, prefix = "\n")
    }

    private fun getReturnInsertion(commentOwner: LuaCommentOwner): AnnotationInsertion {
        val comment = commentOwner.comment ?: return AnnotationInsertion(commentOwner.node.startOffset, suffix = "\n")
        val owner = commentOwner as? LuaFuncBodyOwner
        val params = owner?.funcBody?.paramNameDefList.orEmpty()
        for (index in params.size - 1 downTo 0) {
            val paramTag = comment.getParamDef(params[index].name)
            if (paramTag != null) {
                return AnnotationInsertion(paramTag.textRange.endOffset, prefix = "\n")
            }
        }
        return AnnotationInsertion(comment.textRange.endOffset, prefix = "\n")
    }

    private fun getFieldInsertion(comment: LuaComment): AnnotationInsertion {
        val lastField = comment.findTags(LuaDocTagField::class.java)
            .maxByOrNull { it.textRange.endOffset }
        return if (lastField != null) {
            AnnotationInsertion(getLineEndOffset(lastField), prefix = "\n")
        } else {
            AnnotationInsertion(comment.textRange.endOffset, prefix = "\n")
        }
    }

    private fun getLineEndOffset(element: PsiElement): Int {
        val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)
            ?: return element.textRange.endOffset
        if (document.textLength == 0) {
            return element.textRange.endOffset
        }

        val line = document.getLineNumber(element.textRange.endOffset.coerceAtMost(document.textLength - 1))
        return document.getLineEndOffset(line)
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
