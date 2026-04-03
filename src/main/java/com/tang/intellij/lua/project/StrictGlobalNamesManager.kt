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

package com.tang.intellij.lua.project

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.LinkedHashSet

object StrictGlobalNamesManager {
    private const val DIRECTORY_NAME = ".idea"
    private const val FILE_NAME = "strict-global-names.txt"
    private val DEFAULT_FILE_LINES = listOf(
        "# One global name per line.",
        "# Blank lines and lines starting with # are ignored.",
        ""
    )

    @JvmStatic
    val projectRelativePath: String
        get() = "$DIRECTORY_NAME/$FILE_NAME"

    @JvmStatic
    fun getDisplayPath(project: Project?): String {
        val path = getProjectFilePath(project)
        return path?.toString() ?: "PROJECT_ROOT/$projectRelativePath"
    }

    @JvmStatic
    fun getStrictGlobalNames(project: Project?): Set<String> {
        val names = LinkedHashSet<String>()
        appendNames(names, LuaSettings.instance.strictGlobalNames.asIterable())

        val path = getProjectFilePath(project)
        if (path != null && Files.exists(path)) {
            runCatching {
                Files.readAllLines(path, StandardCharsets.UTF_8)
            }.getOrNull()?.let {
                appendNames(names, it)
            }
        }

        return names
    }

    @JvmStatic
    fun add(project: Project?, name: String): Boolean {
        val normalizedName = normalizeName(name) ?: return false
        val path = getProjectFilePath(project)

        if (path == null) {
            return addToLegacySettings(normalizedName)
        }

        return runCatching {
            val lines = if (Files.exists(path)) {
                Files.readAllLines(path, StandardCharsets.UTF_8).toMutableList()
            } else {
                Files.createDirectories(path.parent)
                DEFAULT_FILE_LINES.toMutableList()
            }

            if (lines.any { normalizeName(it) == normalizedName }) {
                false
            } else {
                lines.add(normalizedName)
                Files.write(path, lines.joinToString(System.lineSeparator()).toByteArray(StandardCharsets.UTF_8))
                refreshProject(project, path)
                true
            }
        }.getOrElse {
            addToLegacySettings(normalizedName)
        }
    }

    @JvmStatic
    fun openFile(project: Project): Boolean {
        val path = getProjectFilePath(project) ?: return false
        return runCatching {
            if (!Files.exists(path)) {
                Files.createDirectories(path.parent)
                Files.write(path, DEFAULT_FILE_LINES.joinToString(System.lineSeparator()).toByteArray(StandardCharsets.UTF_8))
            }

            val virtualFile = LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(FileUtil.toSystemIndependentName(path.toString()))
                ?: return false
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
            true
        }.getOrDefault(false)
    }

    private fun appendNames(target: MutableSet<String>, source: Iterable<String>) {
        source.mapNotNullTo(target) { normalizeName(it) }
    }

    private fun addToLegacySettings(name: String): Boolean {
        val settings = LuaSettings.instance
        if (settings.strictGlobalNames.any { normalizeName(it) == name }) {
            return false
        }

        settings.strictGlobalNames = (settings.strictGlobalNames + name).distinct().toTypedArray()
        ProjectManager.getInstance().openProjects.forEach {
            DaemonCodeAnalyzer.getInstance(it).restart("Global restart")
        }
        return true
    }

    private fun refreshProject(project: Project?, path: Path) {
        if (project != null) {
            LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(path.toString()))
            DaemonCodeAnalyzer.getInstance(project).restart("Global restart")
        }
    }

    private fun getProjectFilePath(project: Project?): Path? {
        val basePath = project?.basePath ?: return null
        return Paths.get(basePath, DIRECTORY_NAME, FILE_NAME)
    }

    private fun normalizeName(value: String): String? {
        val trimmed = value.trim()
        return if (trimmed.isEmpty() || trimmed.startsWith("#")) null else trimmed
    }
}
