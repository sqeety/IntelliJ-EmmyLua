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

package com.tang.intellij.lua.ext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

class LuaFileAbsoluteResolver : ILuaFileResolver {
    companion object {
        var basePath: String = ""
        var projectPath: String = ""
    }

    //处理Project可能在源码路径子文件夹中。只处理文件夹名字唯一的情况
    private fun getBasePath(project: Project, shortUrl: String): String {
        if (project.basePath == projectPath) {
            return basePath
        } else {
            val projectBasePath = project.basePath
            if(projectBasePath != null){
                projectPath = projectBasePath
                val names = projectBasePath.split('/')
                val lastDirectoryName = names.lastOrNull()
                if(lastDirectoryName != null){
                    val index = shortUrl.indexOf(lastDirectoryName)
                    val lastIndex = shortUrl.lastIndexOf(lastDirectoryName)
                    if(index == lastIndex && index != -1){
                        val sameDirectory = shortUrl.substring(0, index + lastDirectoryName.length)
                        if(projectBasePath.endsWith(sameDirectory)){
                            basePath = projectBasePath.substring(0, projectBasePath.length - sameDirectory.length)
                        }
                    }
                    return basePath
                }
            }
        }
        return ""
    }
    override fun find(project: Project, shortUrl: String, extNames: Array<String>): VirtualFile? {
        //绝对路径
        var formatUrl = shortUrl.replace("\\", "/")
        val basePath = getBasePath(project, formatUrl)
        if(formatUrl.startsWith(projectPath)){
            val index = formatUrl.lastIndexOf(".")
            if(index != -1){
                formatUrl = formatUrl.substring(0, index)
                for (ext in extNames) {
                    val newUrl = VfsUtil.pathToUrl(formatUrl + ext)
                    val newFile = VirtualFileManager.getInstance().findFileByUrl(newUrl)
                    if (newFile != null && !newFile.isDirectory) {
                        return newFile
                    }
                }
            }
            return null
        }
        for (ext in extNames) {
            val url = VfsUtil.pathToUrl(basePath + formatUrl + ext)
            val file = VirtualFileManager.getInstance().findFileByUrl(url)
            if (file != null && !file.isDirectory) {
                return file
            }
        }
        return null
    }
}
