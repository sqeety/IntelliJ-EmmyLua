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

package com.tang.intellij.lua.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.tang.intellij.lua.psi.LuaFileManager

class RefreshLuaFileIndexAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (virtualFile != null && project != null) {
            VfsUtil.markDirtyAndRefresh(false, true, true, virtualFile)
            ApplicationManager.getApplication().invokeLater {
                Notifications.Bus.notify(
                    Notification(
                        "RefreshLuaFileIndex",
                        "RefreshLuaFile",
                        "RefreshLuaFile Success: ${virtualFile.path}",
                        NotificationType.INFORMATION
                    )
                )
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (actionUpdateThread == ActionUpdateThread.BGT) {
            val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
            e.presentation.isVisible = shouldShowAction(virtualFile)
        } else {
            e.presentation.isVisible = true
        }
    }

    private fun shouldShowAction(virtualFile: VirtualFile?): Boolean {
        if (virtualFile == null) return false
        val extensions = LuaFileManager.extensions
        for (ext in extensions) {
            if (virtualFile.path.endsWith(ext)) {
                return true
            }
        }
        return false
    }
}