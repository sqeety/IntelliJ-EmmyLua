package com.tang.intellij.lua.debugger.remote

import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import com.tang.intellij.lua.lang.LuaIcons

class LuaMobStackFrame(
    private val functionName: String?,
    private val position: XSourcePosition?,
    val stackLevel: Int,
    private val process: LuaMobDebugProcess
) : XStackFrame() {
    private val values = XValueChildrenList()

    override fun getEvaluator(): XDebuggerEvaluator = LuaMobDebuggerEvaluator(process, this)

    override fun getSourcePosition(): XSourcePosition? = position

    fun addValue(namedValue: XNamedValue) {
        values.add(namedValue)
    }

    override fun computeChildren(node: XCompositeNode) {
        node.addChildren(values, true)
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        val sourcePosition = sourcePosition
        val positionInfo = if (sourcePosition != null) {
            "${sourcePosition.file.name}:${sourcePosition.line + 1}"
        } else {
            "unknown"
        }
        val info = functionName?.let { "$it ($positionInfo)" } ?: functionName.orEmpty()
        component.append(info, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        component.setIcon(LuaIcons.Debugger.StackFrame)
    }
}
