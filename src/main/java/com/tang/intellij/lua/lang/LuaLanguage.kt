package com.tang.intellij.lua.lang

import com.intellij.lang.Language

class LuaLanguage private constructor() : Language("Lua") {
    companion object {
        const val INDEX_VERSION = 40

        @JvmField
        val INSTANCE = LuaLanguage()
    }
}
