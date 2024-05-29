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

package com.tang.intellij.lua.editor.Hints

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.find.actions.ShowUsagesAction
import com.intellij.find.actions.ShowUsagesTable
import com.intellij.find.impl.UsagePresentation
import com.intellij.formatting.visualLayer.InlayPresentation
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import com.intellij.ui.awt.RelativePoint
import com.intellij.usages.UsageViewManager
import com.intellij.usages.impl.UsageViewImpl
import com.tang.intellij.lua.psi.LuaClassMethod
import com.tang.intellij.lua.psi.LuaClassMethodDef
import com.tang.intellij.lua.psi.impl.LuaClassMethodDefImpl
import com.tang.intellij.lua.psi.impl.LuaClassMethodNameImpl
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel


class LuaInlayHintsProvider: InlayHintsProvider<NoSettings> {
    override val name: String = "Lua Inlay Hints"
    override val previewText: String = "exampleMethod(param1, param2)"
    override val key: SettingsKey<NoSettings> = SettingsKey("Lua.inlay.hints")

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent {
            return JPanel()
        }
    }

    override fun isLanguageSupported(language: Language): Boolean{
        return language == Language.findLanguageByID("Lua")
    }

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if(element is LuaClassMethodNameImpl) {
                    val methodDef = element.parentOfType<LuaClassMethodDefImpl>()
                    if (methodDef != null) {
                        val usageCount = findUsageCount(file.project, methodDef)
                        val hintText = "Usages: $usageCount"
                        val offset = element.textRange.startOffset
                        val factory = PresentationFactory(editor)
                        val textPresentation = factory.smallText(hintText)

                        val clickablePresentation = factory.referenceOnHover(textPresentation, object : InlayPresentationFactory.ClickListener {
                            override fun onClick(event: MouseEvent, translated: Point) {
                                ShowUsagesAction.startFindUsages(methodDef, RelativePoint(event), editor)
                            }
                        })

                        sink.addBlockElement(offset, true, true, 0, clickablePresentation)
                    }
                }
                return true
            }
        }
    }


    private fun findUsageCount(project: Project, method: PsiElement): Int {
        val searchScope = GlobalSearchScope.projectScope(project)
        val search = ReferencesSearch.search(method, searchScope, true)
        return search.findAll().size
    }
}