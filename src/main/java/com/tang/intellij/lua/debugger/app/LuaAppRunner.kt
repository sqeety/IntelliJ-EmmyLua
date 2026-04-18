package com.tang.intellij.lua.debugger.app

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.tang.intellij.lua.debugger.LuaRunner
import com.tang.intellij.lua.debugger.getRunContentDescriptorCompat

class LuaAppRunner : LuaRunner() {
    override fun getRunnerId(): String = ID

    override fun canRun(executorId: String, runProfile: RunProfile): Boolean {
        return runProfile is LuaAppRunConfiguration && super.canRun(executorId, runProfile)
    }

    @Throws(ExecutionException::class)
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        FileDocumentManager.getInstance().saveAllDocuments()

        if (environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
            return getRunContentDescriptorCompat(createSession(environment))
        }

        val result: ExecutionResult? = state.execute(environment.executor, environment.runner)
        return result?.let { RunContentBuilder(it, environment).showRunContent(environment.contentToReuse) }
    }

    @Throws(ExecutionException::class)
    private fun createSession(environment: ExecutionEnvironment): XDebugSession {
        val manager = XDebuggerManager.getInstance(environment.project)
        return manager.startSession(environment, object : XDebugProcessStarter() {
            @Throws(ExecutionException::class)
            override fun start(session: XDebugSession): XDebugProcess {
                return LuaAppMobProcess(session)
            }
        })
    }

    companion object {
        private const val ID = "lua.app.runner"
    }
}
