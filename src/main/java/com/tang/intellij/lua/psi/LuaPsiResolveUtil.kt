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

package com.tang.intellij.lua.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.psi.impl.LuaNameExprMixin
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassMemberIndex
import com.tang.intellij.lua.ty.Ty

fun resolveLocal(ref: LuaNameExpr, context: SearchContext? = null) = resolveLocal(ref.name, ref, context)

fun resolveLocal(refName:String, ref: PsiElement, context: SearchContext? = null): PsiElement? {
    val element = resolveInFile(refName, ref, context)
    return if (element is LuaNameExpr) null else element
}

fun resolveInFile(refName:String, pin: PsiElement, context: SearchContext?): PsiElement? {
    var ret: PsiElement? = null
    LuaDeclarationTree.get(pin.containingFile).walkUp(pin) { decl ->
        if (decl.name == refName)
            ret = decl.firstDeclaration.psi
        ret == null
    }

    if (ret == null && refName == Constants.WORD_SELF) {
        val methodDef = PsiTreeUtil.getStubOrPsiParentOfType(pin, LuaClassMethodDef::class.java)
        if (methodDef != null && !methodDef.isStatic) {
            val methodName = methodDef.classMethodName
            val expr = methodName.expr
            ret = if (expr is LuaNameExpr && context != null && expr.name != Constants.WORD_SELF)
                resolve(expr, context)
            else
                expr
        }
    }
    return ret
}

fun isUpValue(ref: LuaNameExpr, context: SearchContext): Boolean {
    val funcBody = PsiTreeUtil.getParentOfType(ref, LuaFuncBody::class.java) ?: return false

    val refName = ref.name
    if (refName == Constants.WORD_SELF) {
        val classMethodFuncDef = PsiTreeUtil.getParentOfType(ref, LuaClassMethodDef::class.java)
        if (classMethodFuncDef != null && !classMethodFuncDef.isStatic) {
            val methodFuncBody = classMethodFuncDef.funcBody
            if (methodFuncBody != null)
                return methodFuncBody.textOffset < funcBody.textOffset
        }
    }

    val resolve = resolveLocal(ref, context)
    if (resolve != null) {
        if (!funcBody.textRange.contains(resolve.textRange))
            return true
    }

    return false
}

/**
 * 查找这个引用
 * @param nameExpr 要查找的ref
 * *
 * @param context context
 * *
 * @return PsiElement
 */
fun resolve(nameExpr: LuaNameExpr, context: SearchContext): PsiElement? {
    //search local
    var resolveResult = resolveInFile(nameExpr.name, nameExpr, context)

    //global
    if (resolveResult == null || resolveResult is LuaNameExpr) {
        val target = (resolveResult as? LuaNameExpr) ?: nameExpr
        val refName = target.name
        val moduleName = target.moduleName ?: Constants.WORD_G
        LuaShortNamesManager.getInstance(context.project).processMembers(moduleName, refName, context, {
            resolveResult = it
            false
        })
    }

    return resolveResult
}

fun multiResolve(ref: LuaNameExpr, context: SearchContext): Array<PsiElement> {
    val list = mutableSetOf<PsiElement>()
    var firstPis:PsiElement = ref
    val assignStat = PsiTreeUtil.getParentOfType(ref, LuaAssignStat::class.java)
    //避免a=a or {}这种死循环
    if (assignStat != null) {
        firstPis = assignStat.firstChild
    }
    //search local
    val resolveResult = resolveInFile(ref.name, firstPis, context)
    if (resolveResult != null) {
        //避免a=a or {}这种死循环
        if(resolveResult.text != ref.text)
            list.add(resolveResult)
    } else {
        val refName = ref.name
        val module = ref.moduleName ?: Constants.WORD_G
        LuaShortNamesManager.getInstance(context.project).processMembers(module, refName, context, {
            list.add(it)
            true
        })
    }
    return list.toTypedArray()
}

fun multiResolve(indexExpr: LuaIndexExpr, context: SearchContext): List<PsiElement> {
    val list = mutableListOf<PsiElement>()
    val name = indexExpr.name ?: return list
    val type = indexExpr.guessParentType(context)
    type.eachTopClass(Processor { ty ->
        val m = ty.findMember(name, context)
        if (m != null)
            list.add(m)
        true
    })
    if (list.isEmpty()) {
        val tree = LuaDeclarationTree.get(indexExpr.containingFile)
        val declaration = tree.find(indexExpr)
        if (declaration != null) {
            list.add(declaration.psi)
        }
    }
    return list
}

fun resolve(indexExpr: LuaIndexExpr, context: SearchContext): PsiElement? {
    val name = indexExpr.name ?: return null
    return resolve(indexExpr, name, context)
}

//renderdoc使用的这里的数据
fun resolve(indexExpr: LuaIndexExpr, idString: String, context: SearchContext): PsiElement? {
    val type = indexExpr.guessParentType(context)
    var ret: PsiElement? = null
    type.eachTopClass(Processor { ty ->
        ret = ty.findMember(idString, context)
        if (ret != null)
            return@Processor false
        true
    })

    if(ret is LuaIndexExpr && ret != indexExpr){
        val assignStat = (ret as LuaIndexExpr).assignStat
        if(assignStat != null){
            val referComment = assignStat.comment?.tagRefer
            if(referComment != null){
                val psi = referComment.referPsi
                return psi
            }
        }
    }

    if (ret == null) {
        val tree = LuaDeclarationTree.get(indexExpr.containingFile)
        val declaration = tree.find(indexExpr)
        if (declaration != null) {
            return declaration.psi
        }
    }

//    if(ret == null){
//        val nameExpr = GetPureFirstChild(indexExpr, context)
//        if(nameExpr != null && isGlobal(nameExpr)){
//            val className = indexExpr.prefixExpr.text
//            if(className.contains(".")){
//                val members = LuaClassMemberIndex.instance.get(className.hashCode(), context.project, context.scope)
//                for (member in members){
//                    if(member.name == indexExpr.name){
//                        return member
//                    }
//                }
//            }
//        }
//    }
    return ret
}

/**
 * 找到 require 的文件路径
 * @param pathString 参数字符串 require "aa.bb.cc"
 * *
 * @param project MyProject
 * *
 * @return PsiFile
 */
fun resolveRequireFile(pathString: String?, project: Project): LuaPsiFile? {
    if (pathString == null)
        return null
    val fileName = pathString.replace('.', '/')
    var f = LuaFileUtil.findFile(project, fileName)

    // issue #415, support init.lua
    if (f == null || f.isDirectory) {
        f = LuaFileUtil.findFile(project, "$fileName/init")
    }

    if (f != null) {
        val psiFile = PsiManager.getInstance(project).findFile(f)
        if (psiFile is LuaPsiFile)
            return psiFile
    }
    return null
}

/**
 * a.b.c => true
 * a.b().c => false
 * a().b.c => false
 */
fun isPure(indexExpr: LuaIndexExpr): Boolean {
    var prev = indexExpr.prefixExpr
    while (true) {
        when (prev) {
            is LuaNameExpr -> return true
            is LuaIndexExpr -> prev = prev.prefixExpr
            else -> return false
        }
    }
}

fun GetPureFirstChild(indexExpr: LuaIndexExpr, context: SearchContext): LuaNameExpr? {
    var prev = indexExpr.prefixExpr
    while (true) {
        when (prev) {
            is LuaNameExpr -> return prev
            is LuaIndexExpr -> prev = prev.prefixExpr
            else -> return null
        }
    }
}

fun isGlobal(nameExpr: LuaNameExpr):Boolean {
    val minx = nameExpr as LuaNameExprMixin
    val gs = minx.greenStub
    return gs?.isGlobal ?: (resolveLocal(nameExpr, null) == null)
}