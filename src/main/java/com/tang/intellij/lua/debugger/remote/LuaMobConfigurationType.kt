package com.tang.intellij.lua.debugger.remote

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.tang.intellij.lua.lang.LuaIcons
import javax.swing.Icon

class LuaMobConfigurationType : ConfigurationType {
    private val factory = LuaMobConfigurationFactory(this)

    override fun getDisplayName(): String = "Lua Remote(Mobdebug)"

    override fun getConfigurationTypeDescription(): String = "Lua Remote Debugger"

    override fun getIcon(): Icon = LuaIcons.FILE

    override fun getId(): String = "lua.mobdebug"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)

    companion object {
        @JvmStatic
        fun getInstance(): LuaMobConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(LuaMobConfigurationType::class.java)
        }
    }
}
