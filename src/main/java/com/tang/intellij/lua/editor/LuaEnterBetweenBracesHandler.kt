package com.tang.intellij.lua.editor

import com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesDelegate

class LuaEnterBetweenBracesHandler : EnterBetweenBracesDelegate() {
    override fun isBracePair(c1: Char, c2: Char): Boolean {
        return (c1 == '{' && c2 == '}') || (c1 == '[' && c2 == ']') || (c1 == '(' && c2 == ')')
    }
}
