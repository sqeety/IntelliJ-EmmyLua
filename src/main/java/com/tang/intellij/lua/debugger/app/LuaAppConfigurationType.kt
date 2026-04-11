package com.tang.intellij.lua.debugger.app

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.tang.intellij.lua.lang.LuaIcons
import javax.swing.Icon

class LuaAppConfigurationType : ConfigurationType {
    val factory = LuaAppConfigurationFactory(this)

    override fun getDisplayName(): String = "Lua Application"

    override fun getConfigurationTypeDescription(): String = "Lua application runner"

    override fun getIcon(): Icon = LuaIcons.FILE

    override fun getId(): String = "lua.app"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)

    companion object {
        @JvmStatic
        fun getInstance(): LuaAppConfigurationType {
            return ConfigurationTypeUtil.findConfigurationType(LuaAppConfigurationType::class.java)
        }
    }
}
