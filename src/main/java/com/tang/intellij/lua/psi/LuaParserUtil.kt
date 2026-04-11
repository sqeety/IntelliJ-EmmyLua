@file:Suppress("unused")

package com.tang.intellij.lua.psi

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.lang.WhitespacesBinders
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.tang.intellij.lua.psi.parser.LuaExpressionParser
import com.tang.intellij.lua.psi.parser.LuaStatementParser

class LuaParserUtil : GeneratedParserUtilBase() {
    companion object {
        @JvmField
        val MY_LEFT_COMMENT_BINDER = WhitespacesAndCommentsBinder { list, _, tokenTextGetter ->
            var lines = 0
            for (i in list.size - 1 downTo 0) {
                val type = list[i]
                if (type == LuaTypes.DOC_COMMENT) {
                    return@WhitespacesAndCommentsBinder i
                }
                val sequence = tokenTextGetter[i]
                lines += StringUtil.getLineBreakCount(sequence)
                if (lines > 1) {
                    break
                }
            }
            list.size
        }

        @JvmField
        val MY_RIGHT_COMMENT_BINDER = WhitespacesAndCommentsBinder { list, _, tokenTextGetter ->
            for (i in 0 until list.size) {
                val type = list[i]
                if (type == LuaTypes.DOC_COMMENT) {
                    return@WhitespacesAndCommentsBinder i + 1
                }
                val sequence = tokenTextGetter[i]
                if (StringUtil.contains(sequence, "\n")) {
                    break
                }
            }
            0
        }

        private val END_SET = TokenSet.create(LuaTypes.END)
        private val IF_SKIPS = TokenSet.create(LuaTypes.THEN, LuaTypes.ELSE, LuaTypes.ELSEIF)
        private val REPEAT_TYPES = TokenSet.create(LuaTypes.UNTIL)
        private val THEN_TYPES1 = TokenSet.create(LuaTypes.ELSE, LuaTypes.ELSEIF, LuaTypes.END)
        private val THEN_SKIPS2 = TokenSet.create(LuaTypes.ELSE, LuaTypes.ELSEIF)
        private val BRACE_L_SET = TokenSet.create(LuaTypes.LCURLY, LuaTypes.LBRACK, LuaTypes.LPAREN)

        @JvmStatic
        fun repeat(builder: PsiBuilder, level: Int, parser: Parser, times: Int): Boolean {
            val marker = builder.mark()
            var result = true
            for (i in 0 until times) {
                if (!result) {
                    break
                }
                result = parser.parse(builder, level)
            }
            marker.rollbackTo()
            return result
        }

        @JvmStatic
        fun checkType(builder: PsiBuilder, level: Int, type: IElementType): Boolean {
            val marker: LighterASTNode? = builder.latestDoneMarker
            return marker != null && marker.tokenType == type
        }

        @JvmStatic
        fun lazyBlock(builder: PsiBuilder, level: Int): Boolean {
            var index = -1
            var begin = builder.rawLookup(index)
            while (begin == TokenType.WHITE_SPACE) {
                begin = builder.rawLookup(--index)
            }

            if (begin != null) {
                val marker = builder.mark()
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_LEFT_BINDER, null)
                if (begin == LuaTypes.RPAREN) {
                    begin = LuaTypes.FUNCTION
                }

                matchStart(true, builder, 0, begin)
                marker.collapse(LuaTypes.BLOCK)
                marker.setCustomEdgeTokenBinders(null, WhitespacesBinders.GREEDY_RIGHT_BINDER)
            }
            return true
        }

        private fun matchStart(advanced: Boolean, builder: PsiBuilder, level: Int, begin: IElementType): Boolean {
            return when (begin) {
                LuaTypes.DO, LuaTypes.ELSE, LuaTypes.FUNCTION -> matchEnd(advanced, builder, level, TokenSet.EMPTY, END_SET)
                LuaTypes.REPEAT -> matchEnd(advanced, builder, level, TokenSet.EMPTY, REPEAT_TYPES)
                LuaTypes.IF -> matchEnd(advanced, builder, level, IF_SKIPS, END_SET)
                LuaTypes.THEN -> {
                    if (level == 0) {
                        matchEnd(advanced, builder, level, TokenSet.EMPTY, THEN_TYPES1)
                    } else {
                        matchEnd(advanced, builder, level, THEN_SKIPS2, END_SET)
                    }
                }
                else -> false
            }
        }

        private fun getRBrace(type: IElementType): IElementType? {
            return when (type) {
                LuaTypes.LCURLY -> LuaTypes.RCURLY
                LuaTypes.LPAREN -> LuaTypes.RPAREN
                LuaTypes.LBRACK -> LuaTypes.RBRACK
                else -> null
            }
        }

        private fun matchBrace(advanced: Boolean, builder: PsiBuilder, level: Int, end: IElementType?): Boolean {
            if (!advanced) {
                builder.advanceLexer()
            }
            var type = builder.tokenType
            while (true) {
                if (type == null || builder.eof()) {
                    return false
                }

                while (true) {
                    val currentType = type ?: return false
                    if (currentType == end) {
                        if (level != 0) {
                            builder.advanceLexer()
                        }
                        return true
                    }
                    var matchBrace = false
                    if (BRACE_L_SET.contains(currentType)) {
                        matchBrace = matchBrace(false, builder, level + 1, getRBrace(currentType) ?: return false)
                    }
                    if (!matchBrace) {
                        break
                    }
                    type = builder.tokenType
                }

                builder.advanceLexer()
                type = builder.tokenType
            }
        }

        private fun matchEnd(advanced: Boolean, builder: PsiBuilder, level: Int, skips: TokenSet, types: TokenSet): Boolean {
            if (!advanced) {
                builder.advanceLexer()
            }
            var type = builder.tokenType

            while (true) {
                if (type == null || builder.eof()) {
                    return false
                }

                while (!skips.contains(type)) {
                    val currentType = type ?: return false
                    if (types.contains(currentType)) {
                        if (level != 0) {
                            builder.advanceLexer()
                        }
                        return true
                    }
                    if (currentType == LuaTypes.UNTIL) {
                        return true
                    }
                    val matched = if (BRACE_L_SET.contains(currentType)) {
                        matchBrace(false, builder, level + 1, getRBrace(currentType) ?: return false)
                    } else {
                        matchStart(false, builder, level + 1, currentType)
                    }
                    if (!matched) {
                        break
                    }
                    type = builder.tokenType
                }

                builder.advanceLexer()
                type = builder.tokenType
            }
        }

        @JvmStatic
        fun parseExpr(builder: PsiBuilder, level: Int): Boolean {
            return LuaExpressionParser.parseExpr(builder, level) != null
        }

        @JvmStatic
        fun parseStatement(builder: PsiBuilder, level: Int): Boolean {
            return LuaStatementParser.parseStatement(builder, level) != null
        }
    }
}
