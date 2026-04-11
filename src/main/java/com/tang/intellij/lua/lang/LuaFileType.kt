package com.tang.intellij.lua.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class LuaFileType private constructor() : LanguageFileType(LuaLanguage.INSTANCE) {
    override fun getName(): String = "lua"

    override fun getDescription(): String = "Lua language file"

    override fun getDefaultExtension(): String = "lua"

    override fun getIcon(): Icon = LuaIcons.FILE

    companion object {
        @JvmField
        val INSTANCE = LuaFileType()
    }
}
