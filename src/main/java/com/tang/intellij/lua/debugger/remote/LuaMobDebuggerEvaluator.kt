package com.tang.intellij.lua.debugger.remote

import com.intellij.xdebugger.XSourcePosition
import com.tang.intellij.lua.debugger.LuaDebuggerEvaluator
import com.tang.intellij.lua.debugger.remote.commands.EvaluatorCommand
import com.tang.intellij.lua.debugger.remote.value.LuaRValue
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform

class LuaMobDebuggerEvaluator(
    private val process: LuaMobDebugProcess,
    private val stackFrame: LuaMobStackFrame
) : LuaDebuggerEvaluator() {
    override fun eval(expression: String, callback: XEvaluationCallback, sourcePosition: XSourcePosition?) {
        val evaluatorCommand = EvaluatorCommand("return $expression", stackFrame.stackLevel, object : EvaluatorCommand.Callback {
            override fun onResult(data: String) {
                val standardGlobals = JsePlatform.standardGlobals()
                val code: LuaValue = standardGlobals.load(data).call()
                val code2Str = code.get(1).toString()
                val code2 = standardGlobals.load(String.format("local _=%s return _", code2Str))
                val value = LuaRValue.create(expression, code2.call(), expression, process.session)
                callback.evaluated(value)
            }
        })
        process.runCommand(evaluatorCommand)
    }
}
