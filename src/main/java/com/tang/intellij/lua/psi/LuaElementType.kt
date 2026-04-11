package com.tang.intellij.lua.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.tree.CustomParsingType
import com.intellij.psi.tree.ILazyParseableElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IReparseableElementType
import com.intellij.util.CharTable
import com.tang.intellij.lua.comment.lexer.LuaDocLexerAdapter
import com.tang.intellij.lua.comment.parser.LuaDocParser
import com.tang.intellij.lua.lang.LuaLanguage
import com.tang.intellij.lua.lang.LuaParserDefinition
import com.tang.intellij.lua.lexer.LuaLexerAdapter
import com.tang.intellij.lua.parser.LuaParser
import com.tang.intellij.lua.stubs.LuaClassMethodType
import com.tang.intellij.lua.stubs.LuaDocTableDefType
import com.tang.intellij.lua.stubs.LuaDocTableFieldType
import com.tang.intellij.lua.stubs.LuaDocTagAliasType
import com.tang.intellij.lua.stubs.LuaDocTagClassType
import com.tang.intellij.lua.stubs.LuaDocTagFieldType
import com.tang.intellij.lua.stubs.LuaDocTagPartialType
import com.tang.intellij.lua.stubs.LuaDocTagTypeType
import com.tang.intellij.lua.stubs.LuaFuncType
import com.tang.intellij.lua.stubs.LuaIndexExprType
import com.tang.intellij.lua.stubs.LuaLiteralElementType
import com.tang.intellij.lua.stubs.LuaNameDefElementType
import com.tang.intellij.lua.stubs.LuaNameExprType
import com.tang.intellij.lua.stubs.LuaStubElementType
import com.tang.intellij.lua.stubs.LuaTableExprType
import com.tang.intellij.lua.stubs.LuaTableFieldType
import com.tang.intellij.lua.stubs.ParamNameDefElementType

private class LuaBlockElementType : IReparseableElementType("LuaBlock", LuaLanguage.INSTANCE) {
    override fun parseContents(chameleon: ASTNode): ASTNode? {
        val project: Project = chameleon.psi.project
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            project,
            chameleon,
            LuaLexerAdapter(),
            LuaLanguage.INSTANCE,
            chameleon.text
        )
        val luaParser: PsiParser = LuaParser()
        return luaParser.parse(this, builder).firstChildNode
    }

    override fun createNode(text: CharSequence?): ASTNode? = null
}

class LuaElementType(debugName: String) : IElementType(debugName, LuaLanguage.INSTANCE) {
    companion object {
        @JvmField
        val DOC_COMMENT: CustomParsingType = object : CustomParsingType("DOC_COMMENT", LuaLanguage.INSTANCE) {
            override fun parse(charSequence: CharSequence, charTable: CharTable): ASTNode {
                val parser: PsiParser = LuaDocParser()
                val builder = PsiBuilderFactory.getInstance().createBuilder(
                    LuaParserDefinition(),
                    LuaDocLexerAdapter(),
                    charSequence
                )
                return parser.parse(this, builder)
            }
        }

        @JvmField
        val FUNC_DEF: IStubElementType<*, *> = LuaFuncType()

        @JvmField
        val CLASS_METHOD_DEF: IStubElementType<*, *> = LuaClassMethodType()

        @JvmField
        val CLASS_FIELD_DEF: LuaStubElementType<*, *> = LuaDocTagFieldType()

        @JvmField
        val TYPE_DEF: LuaStubElementType<*, *> = LuaDocTagTypeType()

        @JvmField
        val CLASS_DEF: LuaStubElementType<*, *> = LuaDocTagClassType()

        @JvmField
        val DOC_TABLE_DEF: LuaStubElementType<*, *> = LuaDocTableDefType()

        @JvmField
        val DOC_TABLE_FIELD_DEF: LuaStubElementType<*, *> = LuaDocTableFieldType()

        @JvmField
        val DOC_ALIAS: LuaStubElementType<*, *> = LuaDocTagAliasType()

        @JvmField
        val DOC_PARTIAL: LuaStubElementType<*, *> = LuaDocTagPartialType()

        @JvmField
        val TABLE: IStubElementType<*, *> = LuaTableExprType()

        @JvmField
        val TABLE_FIELD: IStubElementType<*, *> = LuaTableFieldType()

        @JvmField
        val INDEX: IStubElementType<*, *> = LuaIndexExprType()

        @JvmField
        val NAME_EXPR: IStubElementType<*, *> = LuaNameExprType()

        @JvmField
        val BLOCK: ILazyParseableElementType = LuaBlockElementType()

        @JvmField
        val NAME_DEF = LuaNameDefElementType()

        @JvmField
        val PARAM_NAME_DEF = ParamNameDefElementType()

        @JvmField
        val LITERAL_EXPR = LuaLiteralElementType()
    }
}
