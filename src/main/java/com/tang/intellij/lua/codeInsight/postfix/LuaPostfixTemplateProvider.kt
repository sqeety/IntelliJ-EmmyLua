package com.tang.intellij.lua.codeInsight.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaCheckIfNotNilPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaCheckNilPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaDecreasePostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaForAPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaForIPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaForPPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaIfNotPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaIfPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaIncreasePostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaLocalPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaParPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaPrintPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaReturnPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaToNumberPostfixTemplate
import com.tang.intellij.lua.codeInsight.postfix.templates.LuaToStringPostfixTemplate

class LuaPostfixTemplateProvider : PostfixTemplateProvider {
    private val templates = ContainerUtil.newHashSet<PostfixTemplate>(
        LuaLocalPostfixTemplate(),
        LuaForAPostfixTemplate(),
        LuaForIPostfixTemplate(),
        LuaForPPostfixTemplate(),
        LuaIfPostfixTemplate(),
        LuaIfNotPostfixTemplate(),
        LuaCheckNilPostfixTemplate(),
        LuaCheckIfNotNilPostfixTemplate(),
        LuaReturnPostfixTemplate(),
        LuaPrintPostfixTemplate(),
        LuaIncreasePostfixTemplate(),
        LuaDecreasePostfixTemplate(),
        LuaParPostfixTemplate(),
        LuaToNumberPostfixTemplate(),
        LuaToStringPostfixTemplate()
    )

    override fun getTemplates(): Set<PostfixTemplate> = templates

    override fun isTerminalSymbol(currentChar: Char): Boolean = currentChar == '.'

    override fun preExpand(psiFile: PsiFile, editor: Editor) = Unit

    override fun afterExpand(psiFile: PsiFile, editor: Editor) = Unit

    override fun preCheck(psiFile: PsiFile, editor: Editor, currentOffset: Int): PsiFile = psiFile
}
