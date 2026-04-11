package com.tang.intellij.lua

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

class LuaBundle private constructor() : DynamicBundle(BUNDLE) {
    companion object {
        @NonNls
        private const val BUNDLE = "LuaBundle"

        private val instance = LuaBundle()

        @JvmStatic
        fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): String {
            return instance.getMessage(key, *params)
        }
    }
}
