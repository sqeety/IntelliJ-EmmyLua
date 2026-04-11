package com.tang.intellij.lua.psi

import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.lang.LuaLanguage

class LuaStringTypes private constructor() {
    companion object {
        @JvmField
        val NEXT_LINE: IElementType = IElementType("NEXT_LINE", LuaLanguage.INSTANCE)

        @JvmField
        val INVALID_NEXT_LINE: IElementType = IElementType("INVALID_NEXT_LINE", LuaLanguage.INSTANCE)

        @JvmField
        val BLOCK_START: IElementType = IElementType("BLOCK_START", LuaLanguage.INSTANCE)

        @JvmField
        val BLOCK_END: IElementType = IElementType("BLOCK_END", LuaLanguage.INSTANCE)
    }
}
