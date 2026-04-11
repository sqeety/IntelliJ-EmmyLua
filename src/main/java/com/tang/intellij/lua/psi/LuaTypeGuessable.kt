package com.tang.intellij.lua.psi

import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.ty.ITy
import com.tang.intellij.lua.ty.TyAliasSubstitutor

interface LuaTypeGuessable : LuaPsiElement {
    fun guessType(context: SearchContext): ITy {
        return TyAliasSubstitutor.substitute(SearchContext.infer(this, context), context)
    }
}
