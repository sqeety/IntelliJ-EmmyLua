package com.tang.intellij.lua.psi

import com.tang.intellij.lua.comment.psi.api.LuaComment

interface LuaCommentOwner : LuaPsiElement {
    val comment: LuaComment?
}
