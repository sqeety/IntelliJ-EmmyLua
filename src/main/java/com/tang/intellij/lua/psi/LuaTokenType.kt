package com.tang.intellij.lua.psi

import com.intellij.psi.tree.IElementType
import com.tang.intellij.lua.lang.LuaLanguage

class LuaTokenType(debugName: String) : IElementType(debugName, LuaLanguage.INSTANCE)
