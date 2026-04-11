package com.tang.intellij.lua.editor.completion

import com.intellij.codeInsight.completion.BasicInsertHandler
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.tang.intellij.lua.lang.LuaIcons
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import javax.swing.Icon

open class LuaLookupElement(
    lookupString: String,
    private val bold: Boolean,
    open var icon: Icon?
) : LookupElement(), Comparable<LookupElement> {
    private var myLookupString = lookupString
    private var itemTextUnderlined = false
    protected var myItemString: String? = null
    private var myTailText: String? = null
    protected open var typeText: String? = null
    var handler: InsertHandler<LookupElement> = BasicInsertHandler()

    open val itemText: String
        get() = myItemString ?: myLookupString

    override fun getLookupString(): String = myLookupString

    fun setLookupString(value: String) {
        myLookupString = value
    }

    fun setItemText(value: String?) {
        myItemString = value
    }

    fun setItemTextUnderlined(value: Boolean) {
        itemTextUnderlined = value
    }

    fun setTailText(text: String?) {
        myTailText = text
    }

    override fun handleInsert(context: InsertionContext) {
        handler.handleInsert(context, this)
    }

    override fun renderElement(presentation: LookupElementPresentation) {
        presentation.itemText = itemText
        presentation.isItemTextUnderlined = itemTextUnderlined
        presentation.isItemTextBold = bold
        presentation.setTailText(myTailText.takeUnless { StringUtil.isEmpty(it) }, true)
        presentation.setTypeText(typeText.takeUnless { StringUtil.isEmpty(it) }, null)
        presentation.icon = icon
    }

    override fun compareTo(other: LookupElement): Int {
        return myLookupString.compareTo(other.lookupString)
    }

    override fun equals(other: Any?): Boolean {
        return other is LuaLookupElement && other.hashCode() == hashCode()
    }

    override fun hashCode(): Int = itemText.hashCode()

    override fun isValid(): Boolean = true

    companion object {
        @JvmStatic
        fun fillTypes(project: Project, results: MutableCollection<LookupElement>) {
            LuaShortNamesManager.getInstance(project).processClassNames(project) { key ->
                results.add(LookupElementBuilder.create(key).withIcon(LuaIcons.CLASS))
                true
            }
            LuaShortNamesManager.getInstance(project).processAllAlias(project) { key ->
                results.add(LookupElementBuilder.create(key).withIcon(LuaIcons.Alias))
                true
            }
        }
    }
}
