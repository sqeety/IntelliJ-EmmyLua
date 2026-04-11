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

package com.tang.intellij.lua.debugger.app

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.RawCommandLineEditor
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.textCompletion.TextFieldWithCompletion
import com.tang.intellij.lua.debugger.DebuggerType
import com.tang.intellij.lua.lang.LuaFileType
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.psi.LuaFileUtil
import java.nio.charset.Charset
import javax.swing.DefaultComboBoxModel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class LuaAppSettingsEditor(private val project: Project) : SettingsEditor<LuaAppRunConfiguration>() {
    private lateinit var myProgram: TextFieldWithBrowseButton
    private lateinit var myPanel: JPanel
    private lateinit var myDebugger: ComboBox<DebuggerType>
    private lateinit var myFile: TextFieldWithCompletion
    private lateinit var myWorkingDir: TextFieldWithBrowseButton
    private lateinit var parameters: RawCommandLineEditor
    private lateinit var mobdebugLink: HoverHyperlinkLabel
    private lateinit var myEnvironments: EnvironmentVariablesTextFieldWithBrowseButton
    private lateinit var outputCharset: ComboBox<String>
    private lateinit var showConsoleWindowCheckBox: JCheckBox

    init {
        var descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        myProgram.addBrowseFolderListener(
            project,
            descriptor.withTitle("Choose Program").withDescription("Choose program file")
        )
        descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        myWorkingDir.addBrowseFolderListener(
            project,
            descriptor.withTitle("Choose Working Dir").withDescription("Choose working dir")
        )

        myDebugger.model = DefaultComboBoxModel(arrayOf(DebuggerType.Mob))
        myDebugger.addItemListener {
            onDebuggerTypeChanged()
            fireEditorStateChanged()
        }

        outputCharset.model = DefaultComboBoxModel(Charset.availableCharsets().keys.toTypedArray())
        outputCharset.addItemListener { fireEditorStateChanged() }

        onDebuggerTypeChanged()
    }

    private fun onDebuggerTypeChanged() {
        val debuggerType = myDebugger.selectedItem as? DebuggerType
        mobdebugLink.isVisible = debuggerType == DebuggerType.Mob
        showConsoleWindowCheckBox.isVisible = debuggerType == DebuggerType.Attach
    }

    override fun resetEditorFrom(luaAppRunConfiguration: LuaAppRunConfiguration) {
        myProgram.text = luaAppRunConfiguration.program
        myWorkingDir.text = luaAppRunConfiguration.workingDir.orEmpty()
        myFile.text = luaAppRunConfiguration.file.orEmpty()
        myDebugger.selectedItem = luaAppRunConfiguration.debuggerType
        parameters.text = luaAppRunConfiguration.parameters
        myEnvironments.envs = luaAppRunConfiguration.envs
        mobdebugLink.isVisible = luaAppRunConfiguration.debuggerType == DebuggerType.Mob
        outputCharset.selectedItem = luaAppRunConfiguration.charset
        showConsoleWindowCheckBox.isSelected = luaAppRunConfiguration.showConsole
    }

    override fun applyEditorTo(luaAppRunConfiguration: LuaAppRunConfiguration) {
        luaAppRunConfiguration.program = myProgram.text
        luaAppRunConfiguration.workingDir = myWorkingDir.text
        luaAppRunConfiguration.file = myFile.text
        luaAppRunConfiguration.debuggerType = requireNotNull(myDebugger.selectedItem as? DebuggerType)
        luaAppRunConfiguration.parameters = parameters.text
        luaAppRunConfiguration.envs = myEnvironments.envs
        luaAppRunConfiguration.charset = requireNotNull(outputCharset.selectedItem as? String)
        luaAppRunConfiguration.showConsole = showConsoleWindowCheckBox.isSelected
    }

    override fun createEditor(): JComponent = myPanel

    private inner class LuaFileCompletionProvider : TextFieldCompletionProvider() {
        override fun addCompletionVariants(
            text: String,
            offset: Int,
            prefix: String,
            result: CompletionResultSet
        ) {
            ProjectRootManager.getInstance(project).fileIndex.iterateContent { virtualFile ->
                if (!virtualFile.isDirectory && virtualFile.fileType == LuaFileType.INSTANCE) {
                    val url = LuaFileUtil.getShortPath(project, virtualFile)
                    result.addElement(LookupElementBuilder.create(url).withIcon(LuaIcons.FILE))
                }
                true
            }
        }
    }

    private fun createUIComponents() {
        myFile = TextFieldWithCompletion(
            project,
            LuaFileCompletionProvider(),
            "",
            true,
            true,
            true,
            true
        )

        mobdebugLink = HoverHyperlinkLabel("Get mobdebug.lua 0.7+")
        mobdebugLink.addHyperlinkListener(object : HyperlinkAdapter() {
            override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
                BrowserUtil.browse("https://github.com/pkulchenko/MobDebug/releases")
            }
        })
    }
}
