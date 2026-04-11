package com.tang.intellij.lua.codeInsight.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelectorBase
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.tang.intellij.lua.psi.LuaExpr
import com.tang.intellij.lua.psi.LuaExprStat
import com.tang.intellij.lua.psi.LuaParenExpr

object LuaPostfixUtils {
    val IS_NON_PAR = Condition<PsiElement> { element -> element is LuaExpr && element !is LuaParenExpr }

    @JvmStatic
    fun selectorTopmost(): PostfixTemplateExpressionSelector {
        return selectorTopmost(Conditions.alwaysTrue())
    }

    @JvmStatic
    fun selectorTopmost(additionalFilter: Condition<PsiElement>): PostfixTemplateExpressionSelector {
        return object : PostfixTemplateExpressionSelectorBase(additionalFilter) {
            override fun getNonFilteredExpressions(
                psiElement: PsiElement,
                document: Document,
                offset: Int
            ): List<PsiElement> {
                val stat = PsiTreeUtil.getNonStrictParentOfType(psiElement, LuaExprStat::class.java)
                return ContainerUtil.createMaybeSingletonList(stat?.expr)
            }
        }
    }

    @JvmStatic
    fun selectorAllExpressionsWithCurrentOffset(): PostfixTemplateExpressionSelector {
        return selectorAllExpressionsWithCurrentOffset(Conditions.alwaysTrue())
    }

    @JvmStatic
    fun selectorAllExpressionsWithCurrentOffset(additionalFilter: Condition<PsiElement>): PostfixTemplateExpressionSelector {
        return object : PostfixTemplateExpressionSelectorBase(additionalFilter) {
            override fun getNonFilteredExpressions(
                psiElement: PsiElement,
                document: Document,
                offset: Int
            ): List<PsiElement> {
                var expr = PsiTreeUtil.getNonStrictParentOfType(psiElement, LuaExpr::class.java)
                val list = SmartList<PsiElement>()
                while (expr != null) {
                    if (!PsiTreeUtil.hasErrorElements(expr)) {
                        list.add(expr)
                    }
                    expr = PsiTreeUtil.getParentOfType(expr, LuaExpr::class.java)
                }
                return list
            }
        }
    }
}
