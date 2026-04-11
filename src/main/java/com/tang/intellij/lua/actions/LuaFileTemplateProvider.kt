package com.tang.intellij.lua.actions

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory
import com.tang.intellij.lua.lang.LuaIcons

class LuaFileTemplateProvider : FileTemplateGroupDescriptorFactory {
    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        return FileTemplateGroupDescriptor("Lua", LuaIcons.FILE).apply {
            addTemplate(FileTemplateDescriptor("NewLua.lua", LuaIcons.FILE))
        }
    }
}
