package com.tang.intellij.lua.codeInsight.intention

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CreateFieldFromParameterDialog(project: Project?, defaultName: String) : DialogWrapper(project) {
    private val initialFieldName = defaultName
    private lateinit var nameField: JTextField
    private lateinit var docCheckbox: JCheckBox

    val fieldName: String
        get() = nameField.text

    val isCreateDoc: Boolean
        get() = docCheckbox.isSelected

    init {
        init()
        title = "Create Field"
    }

    override fun createNorthPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.EAST
            fill = GridBagConstraints.BOTH
            gridwidth = 1
            weightx = 1.0
            weighty = 1.0
        }

        constraints.weightx = 0.0
        constraints.gridx = 0
        constraints.gridy = 0
        panel.add(JLabel("Name:"), constraints)

        constraints.weightx = 1.0
        constraints.gridx = 1
        nameField = object : JTextField(initialFieldName) {
            override fun getPreferredSize(): Dimension {
                return super.getPreferredSize().apply { setSize(200.0, height.toDouble()) }
            }
        }
        panel.add(nameField, constraints)

        constraints.weightx = 1.0
        constraints.gridx = 0
        constraints.gridy = 1
        docCheckbox = JCheckBox("Type annotation")
        panel.add(docCheckbox, constraints)

        return panel
    }

    override fun createCenterPanel(): JComponent? = null

    override fun getPreferredFocusedComponent(): JComponent = nameField
}
