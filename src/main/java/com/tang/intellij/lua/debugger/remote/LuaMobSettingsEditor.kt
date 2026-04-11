/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.debugger.remote

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.HyperlinkAdapter
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.HyperlinkEvent

class LuaMobSettingsEditor : SettingsEditor<LuaMobConfiguration>() {
    private lateinit var port: JTextField
    private lateinit var myPanel: JPanel
    private lateinit var mobdebugLink: HoverHyperlinkLabel

    override fun resetEditorFrom(luaMobConfiguration: LuaMobConfiguration) {
        port.text = luaMobConfiguration.port.toString()
    }

    @Throws(ConfigurationException::class)
    override fun applyEditorTo(luaMobConfiguration: LuaMobConfiguration) {
        port.text.toIntOrNull()?.let {
            luaMobConfiguration.port = it
        }
    }

    override fun createEditor(): JComponent {
        port.addActionListener { fireEditorStateChanged() }
        return myPanel
    }

    private fun createUIComponents() {
        mobdebugLink = HoverHyperlinkLabel("Get mobdebug.lua 0.7+")
        mobdebugLink.addHyperlinkListener(object : HyperlinkAdapter() {
            override fun hyperlinkActivated(hyperlinkEvent: HyperlinkEvent) {
                BrowserUtil.browse("https://github.com/pkulchenko/MobDebug/releases")
            }
        })
    }
}
