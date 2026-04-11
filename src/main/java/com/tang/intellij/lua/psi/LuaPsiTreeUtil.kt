package com.tang.intellij.lua.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.Ty

object LuaPsiTreeUtil {
    private val WS = arrayOf(PsiWhiteSpace::class.java)
    private val WS_COMMENTS = arrayOf(PsiWhiteSpace::class.java, PsiComment::class.java)

    @JvmStatic
    fun walkUpLabel(current: PsiElement, processor: Processor<LuaLabelStat>) {
        var node = current
        var prev = node.prevSibling
        while (true) {
            if (prev == null) {
                prev = node.parent
            }
            if (prev == null || prev is PsiFile) {
                break
            }
            if (prev is LuaLabelStat && !processor.process(prev)) {
                break
            }
            node = prev
            prev = node.prevSibling
        }
    }

    @JvmStatic
    fun <T : PsiElement> walkTopLevelInFile(element: PsiElement?, cls: Class<T>, processor: Processor<T>?) {
        if (element == null || processor == null) {
            return
        }
        var parent: PsiElement = element
        while (parent.parent !is PsiFile) {
            parent = parent.parent
        }

        var child: PsiElement? = parent
        while (child != null) {
            if (cls.isInstance(child) && !processor.process(cls.cast(child))) {
                break
            }
            child = child.prevSibling
        }
    }

    @JvmStatic
    fun <T : PsiElement> findElementOfClassAtOffset(file: PsiFile, offset: Int, clazz: Class<T>, strictStart: Boolean): T? {
        return PsiTreeUtil.findElementOfClassAtOffset(file, offset, clazz, strictStart)
            ?: PsiTreeUtil.findElementOfClassAtOffset(file, offset - 1, clazz, strictStart)
    }

    @JvmStatic
    fun <T : PsiElement> getParentOfType(element: PsiElement?, aClass: Class<T>, vararg skips: Class<*>): T? {
        if (element == null) {
            return null
        }
        var current = element.parent
        while (current != null && !aClass.isInstance(current) && PsiTreeUtil.instanceOf(current, *skips)) {
            if (current is PsiFile) {
                return null
            }
            current = current.parent
        }
        return aClass.cast(current)
    }

    @JvmStatic
    fun skipWhitespacesBackward(element: PsiElement?): PsiElement? {
        return PsiTreeUtil.skipSiblingsBackward(element, *WS)
    }

    @JvmStatic
    fun skipWhitespacesAndCommentsBackward(element: PsiElement?): PsiElement? {
        return PsiTreeUtil.skipSiblingsBackward(element, *WS_COMMENTS)
    }

    @JvmStatic
    fun skipWhitespacesForward(element: PsiElement?): PsiElement? {
        return PsiTreeUtil.skipSiblingsForward(element, *WS)
    }

    @JvmStatic
    fun skipWhitespacesAndCommentsForward(element: PsiElement?): PsiElement? {
        return PsiTreeUtil.skipSiblingsForward(element, *WS_COMMENTS)
    }

    @JvmStatic
    fun findContextClass(current: PsiElement): ITy {
        var node = current
        while (true) {
            if (node is PsiFile) {
                break
            }
            if (node is LuaClassMethod) {
                return node.guessParentType(SearchContext.get(node.project))
            }
            node = node.parent
        }
        return Ty.UNKNOWN
    }

    @JvmStatic
    fun processChildren(parent: PsiElement, processor: PsiElementProcessor<PsiElement>) {
        var child = parent.firstChild
        while (child != null) {
            if (processor.execute(child)) {
                child = child.nextSibling
            } else {
                break
            }
        }
    }
}
