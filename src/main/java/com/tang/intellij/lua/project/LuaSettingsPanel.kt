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

package com.tang.intellij.lua.project

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.FileContentUtil
import com.tang.intellij.lua.LuaBundle
import com.tang.intellij.lua.lang.LuaLanguageLevel
import java.awt.BorderLayout
import java.nio.charset.Charset
import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.JTextField
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.PlainDocument

class LuaSettingsPanel : SearchableConfigurable {
    private val settings = LuaSettings.instance
    private lateinit var myPanel: JPanel
    private lateinit var constructorNames: JTextField
    private lateinit var strictDoc: JCheckBox
    private lateinit var smartCloseEnd: JCheckBox
    private lateinit var showWordsInFile: JCheckBox
    private lateinit var enforceTypeSafety: JCheckBox
    private lateinit var nilStrict: JCheckBox
    private lateinit var recognizeGlobalNameAsCheckBox: JCheckBox
    private lateinit var additionalRoots: LuaAdditionalSourcesRootPanel
    private lateinit var enableGenericCheckBox: JCheckBox
    private lateinit var captureOutputDebugString: JCheckBox
    private lateinit var captureStd: JCheckBox
    private lateinit var defaultCharset: JComboBox<String>
    private lateinit var languageLevel: JComboBox<LuaLanguageLevel>
    private lateinit var requireFunctionNames: JTextField
    private lateinit var requirePathSeparator: JComboBox<String>
    private lateinit var tooLargerFileThreshold: JTextField
    private lateinit var strictGlobalNames: JTextField
    private lateinit var strictGlobalNamesFilePath: JTextField
    private lateinit var openStrictGlobalNamesFileButton: JButton

    init {
        constructorNames.text = settings.constructorNamesString
        constructorNames.toolTipText = LuaBundle.message("ui.settings.constructor_names_hint")
        tooLargerFileThreshold.document = IntegerDocument()
        tooLargerFileThreshold.text = settings.tooLargerFileThreshold.toString()
        strictDoc.isSelected = settings.isStrictDoc
        smartCloseEnd.isSelected = settings.isSmartCloseEnd
        showWordsInFile.isSelected = settings.isShowWordsInFile
        enforceTypeSafety.isSelected = settings.isEnforceTypeSafety
        nilStrict.isSelected = settings.isNilStrict
        recognizeGlobalNameAsCheckBox.isSelected = settings.isRecognizeGlobalNameAsType
        additionalRoots.roots = settings.additionalSourcesRoot
        enableGenericCheckBox.isSelected = settings.enableGeneric
        requireFunctionNames.text = settings.requireLikeFunctionNamesString
        requirePathSeparator.model = DefaultComboBoxModel(
            arrayOf(LuaSettings.REQUIRE_PATH_SEPARATOR_SLASH, LuaSettings.REQUIRE_PATH_SEPARATOR_DOT)
        )
        requirePathSeparator.selectedItem = settings.requirePathSeparator
        strictGlobalNames.text = settings.strictGlobalNamesString
        strictGlobalNamesFilePath.isEditable = false
        updateStrictGlobalNamesPath(getSingleOpenProject())
        openStrictGlobalNamesFileButton.addActionListener {
            val project = chooseProjectForStrictGlobalNames()
            if (project != null && StrictGlobalNamesManager.openFile(project)) {
                updateStrictGlobalNamesPath(project)
            }
        }

        captureStd.isSelected = settings.attachDebugCaptureStd
        captureOutputDebugString.isSelected = settings.attachDebugCaptureOutput

        val charsetModel: ComboBoxModel<String> = DefaultComboBoxModel(Charset.availableCharsets().keys.toTypedArray())
        defaultCharset.model = charsetModel
        defaultCharset.selectedItem = settings.attachDebugDefaultCharsetName

        val languageLevelModel: ComboBoxModel<LuaLanguageLevel> = DefaultComboBoxModel(LuaLanguageLevel.values())
        languageLevel.model = languageLevelModel
        languageLevelModel.selectedItem = settings.languageLevel
    }

    override fun getId(): String = "Lua"

    override fun getDisplayName(): String = "Lua"

    override fun createComponent(): JComponent {
        val contentWrapper = JPanel(BorderLayout())
        contentWrapper.add(myPanel, BorderLayout.NORTH)

        return JScrollPane(
            contentWrapper,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        ).apply {
            border = null
            verticalScrollBar.unitIncrement = 16
        }
    }

    override fun isModified(): Boolean {
        return !StringUtil.equals(settings.constructorNamesString, constructorNames.text) ||
            !StringUtil.equals(settings.requireLikeFunctionNamesString, requireFunctionNames.text) ||
            !StringUtil.equals(settings.requirePathSeparator, requirePathSeparator.selectedItem as? String) ||
            settings.tooLargerFileThreshold != getTooLargerFileThreshold() ||
            settings.isSmartCloseEnd != smartCloseEnd.isSelected ||
            settings.isShowWordsInFile != showWordsInFile.isSelected ||
            settings.isEnforceTypeSafety != enforceTypeSafety.isSelected ||
            settings.isNilStrict != nilStrict.isSelected ||
            settings.isRecognizeGlobalNameAsType != recognizeGlobalNameAsCheckBox.isSelected ||
            settings.enableGeneric != enableGenericCheckBox.isSelected ||
            settings.attachDebugCaptureOutput != captureOutputDebugString.isSelected ||
            settings.attachDebugCaptureStd != captureStd.isSelected ||
            settings.attachDebugDefaultCharsetName != defaultCharset.selectedItem as? String ||
            settings.languageLevel != languageLevel.selectedItem as? LuaLanguageLevel ||
            !settings.additionalSourcesRoot.contentEquals(additionalRoots.roots) ||
            !StringUtil.equals(settings.strictGlobalNamesString, strictGlobalNames.text)
    }

    override fun apply() {
        settings.constructorNamesString = constructorNames.text
        constructorNames.text = settings.constructorNamesString
        settings.requireLikeFunctionNamesString = requireFunctionNames.text
        requireFunctionNames.text = settings.requireLikeFunctionNamesString
        settings.requirePathSeparator = requireNotNull(requirePathSeparator.selectedItem as? String)
        settings.tooLargerFileThreshold = getTooLargerFileThreshold()
        settings.isSmartCloseEnd = smartCloseEnd.isSelected
        settings.isShowWordsInFile = showWordsInFile.isSelected
        settings.isEnforceTypeSafety = enforceTypeSafety.isSelected
        settings.isNilStrict = nilStrict.isSelected
        settings.isRecognizeGlobalNameAsType = recognizeGlobalNameAsCheckBox.isSelected
        settings.additionalSourcesRoot = additionalRoots.roots
        settings.enableGeneric = enableGenericCheckBox.isSelected
        settings.attachDebugCaptureOutput = captureOutputDebugString.isSelected
        settings.attachDebugCaptureStd = captureStd.isSelected
        settings.attachDebugDefaultCharsetName = requireNotNull(defaultCharset.selectedItem as? String)
        settings.strictGlobalNamesString = strictGlobalNames.text
        strictGlobalNames.text = settings.strictGlobalNamesString

        val selectedLevel = requireNotNull(languageLevel.selectedItem as? LuaLanguageLevel)
        if (selectedLevel != settings.languageLevel) {
            settings.languageLevel = selectedLevel
            StdLibraryProvider.reload()
            FileContentUtil.reparseOpenedFiles()
        } else {
            for (project in ProjectManager.getInstance().openProjects) {
                DaemonCodeAnalyzer.getInstance(project).restart()
            }
        }
    }

    private fun updateStrictGlobalNamesPath(project: Project?) {
        strictGlobalNamesFilePath.text = StrictGlobalNamesManager.getDisplayPath(project)
        strictGlobalNamesFilePath.caretPosition = 0
    }

    private fun getSingleOpenProject(): Project? {
        val projects = ProjectManager.getInstance().openProjects
        return if (projects.size == 1) projects[0] else null
    }

    private fun chooseProjectForStrictGlobalNames(): Project? {
        val projects = ProjectManager.getInstance().openProjects
        if (projects.isEmpty()) {
            Messages.showInfoMessage(
                LuaBundle.message("ui.settings.strict_global_names_no_project"),
                displayName
            )
            return null
        }
        if (projects.size == 1) {
            return projects[0]
        }

        val projectNames = projects.map(Project::getName).toTypedArray()
        val index = Messages.showDialog(
            myPanel,
            LuaBundle.message("ui.settings.strict_global_names_choose_project"),
            LuaBundle.message("ui.settings.strict_global_names_choose_title"),
            projectNames,
            0,
            null
        )
        return if (index >= 0) projects[index] else null
    }

    private fun getTooLargerFileThreshold(): Int {
        return tooLargerFileThreshold.text.toIntOrNull() ?: settings.tooLargerFileThreshold
    }

    private class IntegerDocument : PlainDocument() {
        @Throws(BadLocationException::class)
        override fun insertString(offset: Int, str: String?, attr: AttributeSet?) {
            if (str == null) {
                return
            }
            try {
                str.toInt()
            } catch (_: NumberFormatException) {
                return
            }
            super.insertString(offset, str, attr)
        }
    }
}
