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

package com.tang.intellij.lua.debugger.emmyAttach

import com.intellij.execution.process.ProcessInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.xdebugger.attach.XAttachPresentationGroup
import com.tang.intellij.lua.debugger.utils.getDisplayName
import com.tang.intellij.lua.lang.LuaIcons
import java.io.File
import java.util.*
import javax.swing.Icon
import javax.swing.filechooser.FileSystemView

class EmmyAttachGroup : XAttachPresentationGroup<ProcessInfo> {
    override fun getItemDisplayText(project: Project, processInfo: ProcessInfo, userDataHolder: UserDataHolder): String {
        val map = userDataHolder.getUserData(EmmyAttachDebuggerProvider.DETAIL_KEY)
        if (map != null) {
            val detail = map[processInfo.pid]
            if (detail != null)
                return getDisplayName(processInfo, detail)
        }
        return processInfo.executableName
    }

    override fun getItemIcon(project: Project, processInfo: ProcessInfo, userDataHolder: UserDataHolder): Icon {
        val map = userDataHolder.getUserData(EmmyAttachDebuggerProvider.DETAIL_KEY)
        if (map != null) {
            val detail = map[processInfo.pid]
            if (detail != null) {
                try {
                    val file = File(detail.path)
                    if (file.exists()) {
                        val sf = FileSystemView.getFileSystemView()
                        val icon = sf.getSystemIcon(file)
                        return icon
                    }
                }catch (e:Exception){

                }
            }
        }
        return LuaIcons.FILE
    }

    override fun getGroupName() = "EmmyLua Attach Debugger"

    override fun compare(a: ProcessInfo, b: ProcessInfo): Int =
            a.executableName.lowercase(Locale.getDefault()).compareTo(b.executableName.lowercase(Locale.getDefault()))

    override fun getOrder(): Int {
        return 0
    }
}