package com.tang.intellij.lua.codeInsight

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.psi.LuaDocTypes
import com.tang.intellij.lua.comment.psi.api.LuaComment

class LuaBackspaceHandlerDelegate : BackspaceHandlerDelegate() {
    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) = Unit

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (c == '-') {
            val offset = editor.caretModel.offset
            val element = file.findElementAt(offset)
            if (element != null) {
                val node = element.node
                if (node.elementType == LuaDocTypes.DASHES) {
                    val end = node.startOffset + node.textLength
                    val start = node.startOffset
                    if (offset == end - 1 && node.textLength == 3) {
                        checkNotNull(PsiTreeUtil.getParentOfType(element, LuaComment::class.java))
                        editor.document.deleteString(start, offset)
                        editor.caretModel.moveToOffset(start)
                    }
                }
            }
        }
        return false
    }
}
