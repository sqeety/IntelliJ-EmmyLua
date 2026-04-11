package com.tang.intellij.lua.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.LayeredIcon
import com.intellij.ui.RowIcon
import javax.swing.Icon

private fun loadIcon(path: String): Icon = IconLoader.getIcon(path, LuaIcons::class.java)

class LuaIcons private constructor() {
    companion object {
        @JvmField
        val FILE: Icon = loadIcon("/icons/lua.png")

        @JvmField
        val CSHARP: Icon = loadIcon("/icons/csharp.png")

        @JvmField
        val CPP: Icon = loadIcon("/icons/cpp.png")

        @JvmField
        val CLASS: Icon = AllIcons.Nodes.Class

        @JvmField
        val Alias: Icon = AllIcons.Nodes.AbstractClass

        @JvmField
        val CLASS_FIELD: Icon = AllIcons.Nodes.Field

        @JvmField
        val CLASS_METHOD: Icon = AllIcons.Nodes.Method

        @JvmField
        val CLASS_METHOD_OVERRIDING: Icon = RowIcon(AllIcons.Nodes.Method, AllIcons.Gutter.OverridingMethod)

        @JvmField
        val GLOBAL_FUNCTION: Icon = LayeredIcon(AllIcons.Nodes.Function, AllIcons.Nodes.StaticMark)

        @JvmField
        val GLOBAL_VAR: Icon = LayeredIcon(AllIcons.Nodes.Variable, AllIcons.Nodes.StaticMark)

        @JvmField
        val LOCAL_VAR: Icon = AllIcons.Nodes.Variable

        @JvmField
        val LOCAL_FUNCTION: Icon = AllIcons.Nodes.Function

        @JvmField
        val PARAMETER: Icon = AllIcons.Nodes.Parameter

        @JvmField
        val WORD: Icon = AllIcons.Actions.Edit

        @JvmField
        val ANNOTATION: Icon = loadIcon("/icons/annotation.png")

        @JvmField
        val META_METHOD: Icon = loadIcon("/icons/meta.png")

        @JvmField
        val PUBLIC: Icon = AllIcons.Nodes.C_public

        @JvmField
        val PROTECTED: Icon = AllIcons.Nodes.C_protected

        @JvmField
        val PRIVATE: Icon = AllIcons.Nodes.C_private

        @JvmField
        val MODULE: Icon = loadIcon("/icons/module.png")

        @JvmField
        val STRING_ARG_HISTORY: Icon = AllIcons.Vcs.History

        @JvmField
        val LAYER: Icon = loadIcon("/icons/lua_layer.svg")

        @JvmField
        val ROOT: Icon = loadIcon("/icons/lua_root.svg")

        @JvmField
        val PROJECT: Icon = loadIcon("/icons/lua_project.svg")

        @JvmField
        val STRING_LITERAL: Icon = AllIcons.Nodes.Aspect
    }

    class LineMarker private constructor() {
        companion object {
            @JvmField
            val TailCall: Icon = loadIcon("/icons/tail.png")
        }
    }

    class Debugger private constructor() {
        companion object {
            @JvmField
            val Console: Icon = loadIcon("/icons/console.svg")

            @JvmField
            val StackFrame: Icon = loadIcon("/icons/frame.svg")
        }

        class Actions private constructor() {
            companion object {
                @JvmField
                val PROFILER: Icon = AllIcons.Debugger.Db_primitive
            }
        }
    }
}
