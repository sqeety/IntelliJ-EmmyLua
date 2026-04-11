package com.tang.intellij.lua.psi

import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy

interface LuaFuncBodyOwner : LuaParametersOwner, LuaTypeGuessable {
    val funcBody: LuaFuncBody?

    fun guessReturnType(searchContext: SearchContext): ITy

    val varargType: ITy?
        get() = getVarargTy(this)

    val params: Array<LuaParamInfo>

    val paramSignature: String
        get() = com.tang.intellij.lua.psi.getParamSignature(this)
}
