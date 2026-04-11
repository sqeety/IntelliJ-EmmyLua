package com.tang.intellij.lua.psi

interface LuaParametersOwner : LuaPsiElement {
    val paramNameDefList: List<LuaParamNameDef>?
}
