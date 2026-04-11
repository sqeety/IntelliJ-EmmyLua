package com.tang.intellij.lua.codeInsight

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.navigation.PsiTargetNavigator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.util.Query
import java.awt.event.MouseEvent

abstract class LuaLineMarkerNavigator<T : PsiElement, S : PsiElement> : GutterIconNavigationHandler<T> {
    override fun navigate(mouseEvent: MouseEvent?, elt: T) {
        if (mouseEvent == null) {
            return
        }
        val navElements = mutableListOf<NavigatablePsiElement>()
        val query = search(elt)
        if (query != null) {
            query.forEach {
                navElements.add(it as NavigatablePsiElement)
                true
            }
            val methods = navElements.toTypedArray()
            if (ApplicationManager.getApplication().isUnitTestMode) {
                elt.putUserData(MARKERS, methods)
            } else {
                PsiTargetNavigator(navElements).navigate(mouseEvent, getTitle(elt), elt.project)
            }
        }
    }

    protected abstract fun getTitle(elt: T): String

    protected abstract fun search(elt: T): Query<S>?

    companion object {
        private val MARKERS = Key.create<Array<NavigatablePsiElement>>("LuaLineMarkerNavigator")
    }
}
