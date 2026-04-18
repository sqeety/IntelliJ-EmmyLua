package com.tang.intellij.lua.debugger

import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugSession

internal fun getRunContentDescriptorCompat(session: XDebugSession): RunContentDescriptor? {
    val compatMethod = session.javaClass.methods.firstOrNull {
        it.name == "getRunContentDescriptorIfInitialized" && it.parameterCount == 0
    }
    if (compatMethod != null) {
        try {
            return compatMethod.invoke(session) as? RunContentDescriptor
        } catch (_: ReflectiveOperationException) {
            // Fall through to legacy API for older implementations.
        } catch (_: SecurityException) {
            // Fall through to legacy API when reflective access is denied.
        }
    }

    @Suppress("DEPRECATION")
    return session.runContentDescriptor
}
