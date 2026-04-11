package com.tang.intellij.lua.editor.formatter

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.tang.intellij.lua.lang.LuaLanguage

class LuaCodeStyleSettings(container: CodeStyleSettings) : CustomCodeStyleSettings(LuaLanguage.INSTANCE.id, container) {
    @JvmField
    var SPACE_AFTER_TABLE_FIELD_SEP = true

    @JvmField
    var SPACE_AROUND_BINARY_OPERATOR = true

    @JvmField
    var SPACE_INSIDE_INLINE_TABLE = true

    @JvmField
    var ALIGN_TABLE_FIELD_ASSIGN = false
}
