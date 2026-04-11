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

package com.tang.intellij.test.refactoring

import com.intellij.openapi.application.ApplicationManager
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.tang.intellij.test.LuaTestBase

class MoveFileTest : LuaTestBase() {
    fun `test move file`() = checkByDirectoryWithProject("""
         --- A.lua
         require('B')
         --- B.lua
         require('to.C')
         --- to/C.lua
         print('c')
    """, """
         --- B.lua
         require('to.C')
         --- to/A.lua
         require('B')
         --- to/C.lua
         print('c')
    """) {
        val file = psiFile("A.lua")
        val targetDirectory = psiDirectory("to")

        ApplicationManager.getApplication().runWriteAction {
            MoveFilesOrDirectoriesUtil.doMoveFile(file, targetDirectory)
        }
    }
}
