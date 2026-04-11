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

package com.tang.intellij.lua.debugger.emmy

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfoRt
import com.tang.intellij.lua.lang.LuaFileType
import com.tang.intellij.lua.psi.LuaFileUtil
import java.awt.BorderLayout
import javax.swing.ButtonGroup
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.PlainDocument

class EmmyDebugSettingsPanel(project: Project) : SettingsEditor<EmmyDebugConfiguration>(), DocumentListener {
    private lateinit var typeCombox: JComboBox<EmmyDebugTransportType>
    private lateinit var type: JLabel
    private lateinit var tcpHostInput: JTextField
    private lateinit var tcpPortInput: JTextField
    private lateinit var tcpHostLabel: JLabel
    private lateinit var tcpPortLabel: JLabel
    private lateinit var pipelineInput: JTextField
    private lateinit var pipeNameLabel: JLabel
    private lateinit var panel: JPanel
    private lateinit var codePanel: JPanel
    private lateinit var waitIDECheckBox: JCheckBox
    private lateinit var breakWhenIDEConnectedCheckBox: JCheckBox
    private lateinit var x64RadioButton: JRadioButton
    private lateinit var x86RadioButton: JRadioButton
    private lateinit var winArchPanel: JPanel
    private lateinit var winArchGroup: ButtonGroup
    private val editorEx: EditorEx

    init {
        val model = javax.swing.DefaultComboBoxModel<EmmyDebugTransportType>()
        model.addElement(EmmyDebugTransportType.TCP_CLIENT)
        model.addElement(EmmyDebugTransportType.TCP_SERVER)
        typeCombox.addActionListener {
            setType(typeCombox.selectedItem as? EmmyDebugTransportType)
            onChanged()
        }
        typeCombox.model = model

        tcpHostInput.text = "localhost"
        tcpHostInput.document.addDocumentListener(this)
        tcpPortInput.text = "9966"
        tcpPortInput.document = IntegerDocument()
        tcpPortInput.document.addDocumentListener(this)

        pipelineInput.text = "emmylua"
        pipelineInput.document.addDocumentListener(this)

        waitIDECheckBox.addActionListener { onChanged() }
        breakWhenIDEConnectedCheckBox.addActionListener { onChanged() }

        winArchGroup = ButtonGroup()
        winArchPanel.isVisible = SystemInfoRt.isWindows
        winArchGroup.add(x64RadioButton)
        winArchGroup.add(x86RadioButton)
        x64RadioButton.addChangeListener { onChanged() }
        x86RadioButton.addChangeListener { onChanged() }

        editorEx = createEditorEx(project)
        codePanel.add(editorEx.component, BorderLayout.CENTER)

        updateCode()
    }

    private fun onChanged() {
        breakWhenIDEConnectedCheckBox.isEnabled = if (isClient()) {
            waitIDECheckBox.isSelected
        } else {
            true
        }
        fireEditorStateChanged()
        updateCode()
    }

    override fun resetEditorFrom(configuration: EmmyDebugConfiguration) {
        typeCombox.selectedItem = configuration.type
        setType(configuration.type)

        tcpHostInput.text = configuration.host
        tcpPortInput.text = configuration.port.toString()
        pipelineInput.text = configuration.pipeName

        if (SystemInfoRt.isWindows) {
            if (configuration.winArch == EmmyWinArch.X64) {
                x64RadioButton.isSelected = true
            } else {
                x86RadioButton.isSelected = true
            }
        }
    }

    override fun applyEditorTo(configuration: EmmyDebugConfiguration) {
        configuration.type = requireNotNull(typeCombox.selectedItem as? EmmyDebugTransportType)
        configuration.host = tcpHostInput.text
        configuration.port = tcpPortInput.text.toInt()
        configuration.pipeName = pipelineInput.text
        if (SystemInfoRt.isWindows) {
            configuration.winArch = if (x64RadioButton.isSelected) EmmyWinArch.X64 else EmmyWinArch.X86
        }
    }

    protected fun setType(type: EmmyDebugTransportType?) {
        val isTcp = type == EmmyDebugTransportType.TCP_CLIENT || type == EmmyDebugTransportType.TCP_SERVER
        tcpHostLabel.isVisible = isTcp
        tcpPortLabel.isVisible = isTcp
        tcpHostInput.isVisible = isTcp
        tcpPortInput.isVisible = isTcp

        pipeNameLabel.isVisible = !isTcp
        pipelineInput.isVisible = !isTcp

        waitIDECheckBox.isVisible = isClient()
    }

    private fun isClient(): Boolean {
        val type = getType()
        return type == EmmyDebugTransportType.TCP_CLIENT || type == EmmyDebugTransportType.PIPE_CLIENT
    }

    private fun getType(): EmmyDebugTransportType? = typeCombox.selectedItem as? EmmyDebugTransportType

    private fun getHost(): String = tcpHostInput.text

    private fun getPort(): Int = tcpPortInput.text.toIntOrNull() ?: 0

    private fun getPipeName(): String = pipelineInput.text

    override fun createEditor(): JComponent = panel

    private fun createEditorEx(project: Project): EditorEx {
        val editorFactory = EditorFactory.getInstance()
        val editorDocument: Document = editorFactory.createDocument("")
        return editorFactory.createEditor(editorDocument, project, LuaFileType.INSTANCE, false) as EditorEx
    }

    private fun updateCode() {
        ApplicationManager.getApplication().runWriteAction { updateCodeImpl() }
    }

    private fun getDebuggerFolder(): String? {
        return when {
            SystemInfoRt.isWindows -> LuaFileUtil.getPluginVirtualFile("debugger/emmy/windows")
            SystemInfoRt.isMac -> LuaFileUtil.getPluginVirtualFile("debugger/emmy/mac")
            else -> LuaFileUtil.getPluginVirtualFile("debugger/emmy/linux")
        }
    }

    private fun updateCodeImpl() {
        val sb = StringBuilder()
        when {
            SystemInfoRt.isWindows -> {
                val arch = if (x64RadioButton.isSelected) EmmyWinArch.X64 else EmmyWinArch.X86
                sb.append("package.cpath = package.cpath .. ';")
                    .append(getDebuggerFolder())
                    .append("/")
                    .append(arch.desc)
                    .append("/?.dll'\n")
            }

            SystemInfoRt.isMac -> {
                sb.append("package.cpath = package.cpath .. ';")
                    .append(getDebuggerFolder())
                    .append("/")
                    .append(if (System.getProperty("os.arch") == "arm64") "arm64" else "x64")
                    .append("/?.dylib'\n")
            }

            else -> {
                sb.append("package.cpath = package.cpath .. ';")
                    .append(getDebuggerFolder())
                    .append("/?.so'\n")
            }
        }

        sb.append("local dbg = require('emmy_core')\n")
        when (getType()) {
            EmmyDebugTransportType.PIPE_CLIENT -> {
                sb.append("dbg.pipeListen('").append(getPipeName()).append("')\n")
            }

            EmmyDebugTransportType.PIPE_SERVER -> {
                sb.append("dbg.pipeConnect('").append(getPipeName()).append("')\n")
            }

            EmmyDebugTransportType.TCP_CLIENT -> {
                sb.append("dbg.tcpListen('").append(getHost()).append("', ").append(getPort()).append(")\n")
            }

            EmmyDebugTransportType.TCP_SERVER -> {
                sb.append("dbg.tcpConnect('").append(getHost()).append("', ").append(getPort()).append(")\n")
            }

            null -> Unit
        }

        if (isClient()) {
            if (waitIDECheckBox.isSelected) {
                sb.append("dbg.waitIDE()\n")
                if (breakWhenIDEConnectedCheckBox.isSelected) {
                    sb.append("dbg.breakHere()\n")
                }
            }
        } else if (breakWhenIDEConnectedCheckBox.isSelected) {
            sb.append("dbg.breakHere()\n")
        }

        editorEx.document.setText(sb.toString())
    }

    override fun insertUpdate(e: DocumentEvent) {
        onChanged()
    }

    override fun removeUpdate(e: DocumentEvent) {
        onChanged()
    }

    override fun changedUpdate(e: DocumentEvent) {
        onChanged()
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
