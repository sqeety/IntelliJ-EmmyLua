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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.LuaClassMethod
import com.tang.intellij.lua.psi.LuaIndexExpr

class LuaConstructorReferencesSearcher : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ): Boolean {
        return ApplicationManager.getApplication().runReadAction<Boolean> {
            executeInReadAction(queryParameters, consumer)
        }
    }

    private fun executeInReadAction(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ): Boolean {
        val target = queryParameters.elementToSearch as? LuaClassMethod ?: return true
        val initializerName = target.name ?: return true
        val constructorNames = LuaSettings.getConstructorNamesForInitializer(initializerName)
        if (constructorNames.isEmpty())
            return true

        val processed = mutableSetOf<Pair<LuaIndexExpr, IntRange>>()
        val searchHelper = PsiSearchHelper.getInstance(target.project)
        for (constructorName in constructorNames) {
            if (constructorName == initializerName)
                continue

            val completed = searchHelper.processElementsWithWord(
                { element, _ ->
                    val indexExpr = PsiTreeUtil.getParentOfType(element, LuaIndexExpr::class.java, false)
                        ?: return@processElementsWithWord true
                    if (indexExpr.name != constructorName)
                        return@processElementsWithWord true

                    for (reference in indexExpr.references) {
                        val key = indexExpr to reference.rangeInElement.let { it.startOffset until it.endOffset }
                        if (processed.add(key) && reference.isReferenceTo(target) && !consumer.process(reference))
                            return@processElementsWithWord false
                    }
                    true
                },
                queryParameters.effectiveSearchScope,
                constructorName,
                UsageSearchContext.ANY,
                true
            )
            if (!completed)
                return false
        }
        return true
    }
}
