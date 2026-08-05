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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.lang.LuaLanguageLevel
import java.nio.charset.Charset

/**
 *
 * Created by tangzx on 2017/6/12.
 */
@State(name = "LuaSettings", storages = [(Storage("emmy.xml"))])
class LuaSettings : PersistentStateComponent<LuaSettings> {
    //自定义require函数，参考constructorNames
    var requireLikeFunctionNames: Array<String> = arrayOf("require")

    var requirePathSeparator: String = REQUIRE_PATH_SEPARATOR_SLASH

    var constructorNames: Array<String> = arrayOf()

    //Doc文档严格模式，对不合法的注解报错
    var isStrictDoc: Boolean = false

    //在未匹配end的statement后回车会自动补全
    var isSmartCloseEnd: Boolean = true

    //在代码完成时使用参数完成模板
    var autoInsertParameters: Boolean = false

    var isShowWordsInFile: Boolean = true

    // Throw errors if specified and found types do not match
    var isEnforceTypeSafety: Boolean = false

    var isNilStrict: Boolean = false

    var isRecognizeGlobalNameAsType = true

    var additionalSourcesRoot = arrayOf<String>()

    /**
     * 使用泛型
     */
    var enableGeneric: Boolean = false

    /**
     * (KB)
     */
    var tooLargerFileThreshold = 10240

    var attachDebugDefaultCharsetName = "UTF-8"

    var attachDebugCaptureStd = true

    var attachDebugCaptureOutput = true

    /**
     * Lua language level
     */
    var languageLevel = LuaLanguageLevel.LUA53

    var strictGlobalNames: Array<String> = arrayOf<String>()

    override fun getState(): LuaSettings {
        return this
    }

    override fun loadState(luaSettings: LuaSettings) {
        XmlSerializerUtil.copyBean(luaSettings, this)
    }

    var constructorNamesString: String
        get() {
            return constructorNames.mapNotNull(::normalizeConstructorConfig).joinToString(";")
        }
        set(value) {
            constructorNames = value.split(";").mapNotNull(::normalizeConstructorConfig).toTypedArray()
        }

    private fun normalizeConstructorConfig(value: String): String? {
        val parts = value.split("=", limit = 2)
        val name = parts[0].trim()
        if (name.isEmpty())
            return null
        val initializerName = parts.getOrNull(1)?.trim()
        return if (initializerName.isNullOrEmpty()) name else "$name=$initializerName"
    }

    private fun getConstructorInitializerName(name: String): String? {
        constructorNames.forEach { config ->
            val parts = config.split("=", limit = 2)
            if (parts[0].trim() == name)
                return parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getConstructorNamesForInitializer(initializerName: String): List<String> {
        return constructorNames.mapNotNull { config ->
            val parts = config.split("=", limit = 2)
            val name = parts[0].trim()
            val configuredInitializer = parts.getOrNull(1)?.trim()
            name.takeIf { it.isNotEmpty() && configuredInitializer == initializerName }
        }.distinct()
    }

    val attachDebugDefaultCharset: Charset
        get() {
            return Charset.forName(attachDebugDefaultCharsetName) ?: Charset.forName("UTF-8")
        }

    var requireLikeFunctionNamesString: String
        get() {
            return requireLikeFunctionNames.joinToString(";")
        }
        set(value) {
            requireLikeFunctionNames = value.split(";").map { it.trim() }.toTypedArray()
        }

    var strictGlobalNamesString: String
        get() {
            return strictGlobalNames.joinToString(";")
        }
        set(value) {
            strictGlobalNames = value.split(";").map { it.trim() }.toTypedArray()
        }

    val requirePathSeparatorChar: Char
        get() = if (requirePathSeparator == REQUIRE_PATH_SEPARATOR_DOT) '.' else '/'

    fun isRequirePathSeparatorDot(): Boolean {
        return requirePathSeparator == REQUIRE_PATH_SEPARATOR_DOT
    }

    fun toFileRequirePath(path: String): String {
        return path.replace('\\', '/').replace('.', '/')
    }

    fun normalizeRequirePath(path: String): String {
        val filePath = toFileRequirePath(path)
        return if (isRequirePathSeparatorDot()) filePath.replace('/', '.') else filePath
    }

    companion object {
        const val REQUIRE_PATH_SEPARATOR_SLASH = "/"
        const val REQUIRE_PATH_SEPARATOR_DOT = "."

        val instance: LuaSettings
            get() = ApplicationManager.getApplication().getService(LuaSettings::class.java)

        fun isConstructorName(name: String): Boolean {
            return instance.constructorNames.any { it.substringBefore('=').trim() == name }
        }

        fun getConstructorInitializerName(name: String): String? {
            return instance.getConstructorInitializerName(name)
        }

        fun getConstructorNamesForInitializer(initializerName: String): List<String> {
            return instance.getConstructorNamesForInitializer(initializerName)
        }

        fun isRequireLikeFunctionName(name: String): Boolean {
            return instance.requireLikeFunctionNames.contains(name) || name == Constants.WORD_REQUIRE
        }
    }
}
