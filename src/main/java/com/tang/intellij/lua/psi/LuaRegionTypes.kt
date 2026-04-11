package com.tang.intellij.lua.psi

import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.lang.LuaLanguage

class LuaRegionTypes private constructor() {
    companion object {
        @JvmField
        val REGION_START: IElementType = IElementType("REGION_START", LuaLanguage.INSTANCE)

        @JvmField
        val REGION_DESC: IElementType = IElementType("REGION_DESC", LuaLanguage.INSTANCE)

        @JvmField
        val REGION_END: IElementType = IElementType("REGION_END", LuaLanguage.INSTANCE)
    }
}
