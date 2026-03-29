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

package com.tang.intellij.lua.codeInsight.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.openapi.util.io.FileUtil
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.project.LuaSourceRootManager
import com.tang.intellij.lua.psi.*

class LuaRequirePathInspection : StrictInspection() {
    override fun isAvailableForFile(file: PsiFile): Boolean {
        return !LuaFileUtil.isStdLibFile(file.virtualFile, file.project)
    }

    override fun buildVisitor(
        myHolder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor =
        object : LuaVisitor() {
            fun isRequireLikeFunc(o: LuaCallExpr): Boolean {
                val nameRef = o.expr
                if (nameRef is LuaNameExpr || nameRef is LuaIndexExpr) {
                    if (LuaSettings.isRequireLikeFunctionName(nameRef.text)) {
                        return true
                    }
                }
                return false
            }

            override fun visitCallExpr(o: LuaCallExpr) {
                if (!isRequireLikeFunc(o)) return
                if (o.argList.isEmpty()) return

                val firstArg = o.argList[0]
                if (firstArg !is LuaLiteralExpr || firstArg.kind != LuaLiteralKind.String) return

                val pathString = firstArg.stringValue
                val file = resolveRequireFile(pathString, o.project)
                if (file == null) {
                    myHolder.registerProblem(firstArg, "Path '%s' not in SourceRoot.".format(pathString))
                    return
                }

                val path = file.virtualFile.toString()
                for (sourceRoot in LuaSourceRootManager.getInstance(o.project).getSourceRootUrls()) {
                    if (!path.startsWith(sourceRoot)) {
                        continue
                    }

                    val extension = FileUtil.getExtension(path)
                    val fileAbsolutePath = path.substring(sourceRoot.length + 1, path.length - extension.length - 1)
                    val filePathString = LuaSettings.instance.normalizeRequirePath(fileAbsolutePath)
                    if (!filePathString.equals(pathString, ignoreCase = false)) {
                        myHolder.registerProblem(
                            firstArg,
                            "Path '%s' Case not Match '%s'.".format(pathString, filePathString),
                            object : LocalQuickFix {
                                override fun getFamilyName(): String {
                                    return "Rename to '${filePathString}'"
                                }

                                override fun applyFix(p0: Project, p1: ProblemDescriptor) {
                                    val newLiteral = LuaElementFactory.createLiteral(
                                        o.project,
                                        "\"$filePathString\""
                                    )
                                    firstArg.replace(newLiteral)
                                }
                            }
                        )
                    }
                    return
                }
            }
        }
}