package com.tang.intellij.lua.debugger.remote

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.tang.intellij.lua.debugger.LuaRunner
import com.tang.intellij.lua.debugger.getRunContentDescriptorCompat

class LuaMobDebuggerRunner : LuaRunner() {
    override fun getRunnerId(): String = ID

    override fun canRun(executorId: String, runProfile: RunProfile): Boolean {
        return DefaultDebugExecutor.EXECUTOR_ID == executorId && runProfile is LuaMobConfiguration
    }

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        return getRunContentDescriptorCompat(createSession(environment))
    }

    @Throws(ExecutionException::class)
    private fun createSession(environment: ExecutionEnvironment): XDebugSession {
        val manager = XDebuggerManager.getInstance(environment.project)
        return manager.startSession(environment, object : XDebugProcessStarter() {
            @Throws(ExecutionException::class)
            override fun start(session: XDebugSession): XDebugProcess {
                return LuaMobDebugProcess(session)
            }
        })
    }

    companion object {
        private const val ID = "lua.mob.runner"
    }
}
