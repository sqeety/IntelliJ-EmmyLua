package com.tang.intellij.lua.debugger

enum class DebuggerType(private val code: Int, private val desc: String) {
    Attach(1, "Attach Debugger(Not available)"),
    Mob(2, "Remote Debugger(Mobdebug)");

    fun value(): Int = code

    override fun toString(): String = desc

    companion object {
        @JvmStatic
        fun valueOf(v: Int): DebuggerType? {
            return when (v) {
                1 -> Attach
                2 -> Mob
                else -> null
            }
        }
    }
}
