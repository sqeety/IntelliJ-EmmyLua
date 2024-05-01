package com.tang.intellij.lua.debugger.utils

import com.tang.intellij.lua.psi.LuaFileUtil.getPluginVirtualFile

object FileUtils {
    val archExeFile: String?
        get() = getPluginVirtualFile("debugger/emmy/windows/x86/emmy_tool.exe")

}