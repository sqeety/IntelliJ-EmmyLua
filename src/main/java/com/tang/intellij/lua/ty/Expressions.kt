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

package com.tang.intellij.lua.ty

import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.containers.toArray
import com.tang.intellij.lua.Constants
import com.tang.intellij.lua.comment.psi.LuaDocTableField
import com.tang.intellij.lua.comment.psi.LuaDocTagField
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.psi.impl.LuaNameExprMixin
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.GuardType
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.lua.stubs.index.LuaClassIndex

fun inferExpr(expr: LuaExpr?, context: SearchContext): ITy {
    if (expr == null)
        return Ty.UNKNOWN
    if (expr is LuaNameExpr || expr is LuaIndexExpr) {
        val tree = LuaDeclarationTree.get(expr.containingFile)
        val declaration = tree.find(expr)?.firstDeclaration?.psi
        if (declaration != null && declaration != expr) {
            when (declaration) {
                is LuaNameDef -> {
                    return declaration.guessType(context)
                }

                is LuaIndexExpr -> {
                    return declaration.infer(context)
                }

                is LuaTypeGuessable ->{
                    return declaration.guessType(context)
                }
            }
        }
    }
    return inferExprInner(expr, context)
}

private fun inferExprInner(expr: LuaPsiElement, context: SearchContext): ITy {
    return when (expr) {
        is LuaUnaryExpr -> expr.infer(context)
        is LuaBinaryExpr -> expr.infer(context)
        is LuaCallExpr -> expr.infer(context)
        is LuaClosureExpr -> infer(expr, context)
        is LuaTableExpr -> expr.infer()
        is LuaParenExpr -> infer(expr.expr, context)
        is LuaNameExpr -> expr.infer(context)
        is LuaLiteralExpr -> expr.infer()
        is LuaIndexExpr -> expr.infer(context)
        else -> Ty.UNKNOWN
    }
}

private fun LuaUnaryExpr.infer(context: SearchContext): ITy {
    val stub = stub
    val operator = if (stub != null) stub.opType else unaryOp.node.firstChildNode.elementType

    return when (operator) {
        LuaTypes.MINUS -> infer(expr, context) // Negative something
        LuaTypes.GETN -> Ty.NUMBER // Table length is a number
        LuaTypes.NOT -> Ty.BOOLEAN //not
        else -> Ty.UNKNOWN
    }
}

private fun LuaBinaryExpr.infer(context: SearchContext): ITy {
    val stub = stub
    val operator = if (stub != null) stub.opType else {
        val firstChild = firstChild
        val nextVisibleLeaf = PsiTreeUtil.nextVisibleLeaf(firstChild)
        nextVisibleLeaf?.node?.elementType
    }
    return operator.let {
        when (it) {
        //..
            LuaTypes.CONCAT -> Ty.STRING
        //<=, ==, <, ~=, >=, >
            LuaTypes.LE, LuaTypes.EQ, LuaTypes.LT, LuaTypes.NE, LuaTypes.GE, LuaTypes.GT -> Ty.BOOLEAN
        //and, or
            LuaTypes.AND, LuaTypes.OR -> guessAndOrType(this, operator, context)
        //&, <<, |, >>, ~, ^,    +, -, *, /, //, %
            LuaTypes.BIT_AND, LuaTypes.BIT_LTLT, LuaTypes.BIT_OR, LuaTypes.BIT_RTRT, LuaTypes.BIT_TILDE, LuaTypes.EXP,
            LuaTypes.PLUS, LuaTypes.MINUS, LuaTypes.MULT, LuaTypes.DIV, LuaTypes.DOUBLE_DIV, LuaTypes.MOD -> Ty.NUMBER
            else -> Ty.UNKNOWN
        }
    }
}

private fun guessAndOrType(binaryExpr: LuaBinaryExpr, operator: IElementType?, context:SearchContext): ITy {
    val rhs = binaryExpr.right
    val assignStat = PsiTreeUtil.getParentOfType(binaryExpr, LuaAssignStat::class.java)
    //and
    if (operator == LuaTypes.AND)
    {
        if(rhs != null && assignStat != null) {
            for (expr in assignStat.varExprList.exprList){
                if(expr.text == rhs.text){
                    return Ty.UNKNOWN
                }
            }
        }
        return infer(rhs, context)
    }

    //or
    val lhs = binaryExpr.left
    if(lhs != null && assignStat != null) {
        for (expr in assignStat.varExprList.exprList){
            if(expr.text == lhs.text){
                return infer(rhs, context)
            }
        }
    }
    val lty = infer(lhs, context)
    return if (rhs != null) lty.union(infer(rhs, context)) else lty
}

private fun guessBinaryOpType(binaryExpr : LuaBinaryExpr, context:SearchContext): ITy {
    val lhs = binaryExpr.left
    // TODO: Search for operator overrides
    return infer(lhs, context)
}

fun LuaCallExpr.createSubstitutor(sig: IFunSignature, context: SearchContext): ITySubstitutor? {
    if (sig.isGeneric()) {
        val list = mutableListOf<ITy>()
        // self type
        if (this.isMethodColonCall) {
            this.prefixExpr?.let { prefix ->
                list.add(prefix.guessType(context))
            }
        }
        this.argList.map { list.add(it.guessType(context)) }
        val map = mutableMapOf<String, ITy>()
        var processedIndex = -1
        sig.tyParameters.forEach { map[it.name] = Ty.UNKNOWN }
        sig.processArgs { index, param ->
            val arg = list.getOrNull(index)
            if (arg != null) {
                GenericAnalyzer(arg, param.ty).analyze(map)
            }
            processedIndex = index
            true
        }
        // vararg
        val varargTy = sig.varargTy
        if (varargTy != null && processedIndex < list.lastIndex) {
            val argTy = list[processedIndex + 1]
            GenericAnalyzer(argTy, varargTy).analyze(map)
        }
        sig.tyParameters.forEach { it ->
            val superCls = it.superClassNames
            if(superCls.isNotEmpty()){
                superCls.forEach {
                    if (Ty.isInvalid(map[it])) map[it] = Ty.create(it)
                }
            }

        }
        return object : TySubstitutor() {
            override fun substitute(clazz: ITyClass, context: SearchContext): ITy {
                return map.getOrElse(clazz.className) { clazz }
            }
        }
    }
    return null
}

private fun LuaCallExpr.getReturnTy(sig: IFunSignature, context: SearchContext): ITy? {
    val substitutor = createSubstitutor(sig, context)
    val returnGuessTy = sig.guessReturnType(context)
    var returnTy = if (substitutor != null) returnGuessTy.substitute(substitutor, context) else returnGuessTy
    returnTy = returnTy.substitute(TySelfSubstitutor(project, this), context)
    return if (returnTy is TyTuple) {
        if (context.guessTuple())
            returnTy
        else {
            var returnIndexTy = returnTy.list.getOrNull(context.index)
            if(returnIndexTy == null){
                returnIndexTy = returnTy.list.getOrNull(0)
            }
            returnIndexTy
        }
    } else {
        if (context.guessTuple() || context.index == 0)
            returnTy
        else returnTy
    }
}

private fun LuaCallExpr.infer(context: SearchContext): ITy {
    val luaCallExpr = this
    // xxx()
    val expr = luaCallExpr.expr
    // 从 require 'xxx' 中获取返回类型
    if (expr is LuaNameExpr && LuaSettings.isRequireLikeFunctionName(expr.name)) {
        var filePath: String? = null
        val string = luaCallExpr.firstStringArg
        if (string is LuaLiteralExpr) {
            filePath = string.stringValue
        }
        var file: LuaPsiFile? = null
        if (filePath != null)
            file = resolveRequireFile(filePath, luaCallExpr.project)
        if (file != null)
            return file.guessType(context)

        return Ty.UNKNOWN
    }

    var ret: ITy = Ty.UNKNOWN
    val ty = infer(expr, context)//expr.guessType(context)
    val paramCount = luaCallExpr.argList.size
    TyUnion.each(ty) {
        when (it) {
            is ITyFunction -> {
                var matchFunc = false
                it.process(Processor { sig ->
                    val targetTy = getReturnTy(sig, context)
                    if (targetTy != null && sig.params.size == paramCount) {
                        ret = ret.union(targetTy)
                        matchFunc = true
                    }
                    !matchFunc
                })
                if (!matchFunc) {
                    val targetTy = getReturnTy(it.mainSignature, context)
                    if (targetTy != null)
                        ret = ret.union(targetTy)
                }
            }
            //constructor : Class table __call
            is ITyClass -> ret = ret.union(it)
        }
    }

    // xxx.new()
    if (Ty.isInvalid(ret) && expr is LuaIndexExpr) {
        val fnName = expr.name
        if (fnName != null && LuaSettings.isConstructorName(fnName)) {
            ret = ret.union(expr.guessParentType(context))
        }
    }

    return ret
}

private fun LuaNameExpr.infer(context: SearchContext): ITy {
    var type:ITy = Ty.UNKNOWN

    /**
     * fixme : optimize it.
     * function xx:method()
     *     self.name = '123'
     * end
     *
     * https://github.com/EmmyLua/IntelliJ-EmmyLua/issues/93
     * the type of 'self' should be same of 'xx'
     */
    if (name == Constants.WORD_SELF) {
        val methodDef = PsiTreeUtil.getStubOrPsiParentOfType(this, LuaClassMethodDef::class.java)
        if (methodDef != null && !methodDef.isStatic) {
            val methodName = methodDef.classMethodName
            val expr = methodName.expr
            type = expr.guessType(context)
            return type
        }
    }

    val multiResolve = multiResolve(this, context)
    for (element in multiResolve) {
        val set = getType(context, element)
        type = type.union(set)
    }

    if (Ty.isInvalid(type)) {
        type = getType(context, this)
    }
    return type
}

private fun getType(context: SearchContext, def: PsiElement): ITy {
    when (def) {
        is LuaNameExpr -> {
            //todo stub.module -> ty
//            val stub = def.stub
//            stub?.module?.let {
//                val memberType = createSerializedClass(it).findMemberType(def.name, context)
//                if (memberType != null && !Ty.isInvalid(memberType))
//                    return memberType
//            }

            var type: ITy = def.docTy ?: Ty.UNKNOWN
            //guess from value expr
            val stat = def.assignStat
            if (stat != null) {
                val exprList = stat.valueExprList
                if (exprList != null) {
                    val defIndex = stat.getIndexFor(def)
                    // foo = { ... }
                    if (Ty.isInvalid(type) || exprList.at(defIndex) is LuaTableExpr) {
                        val expr = exprList.at(defIndex)
                        if(expr != null)
                        {
                            //不推断_G的全局别名。比如A=_G
                            if(exprList.text != Constants.WORD_G){
                                if (!context.guessTextSet.contains(exprList.text)) {
                                    context.guessTextSet.add(exprList.text)
                                    val ret = expr.guessType(context)
                                    if (!Ty.isInvalid(ret)) {
                                        context.guessTextSet.remove(exprList.text)
                                        type = type.union(ret)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //Global
            if (isGlobal(def) && type !is ITyPrimitive) {
                //use globalClassTy to store class members, that's very important
                type = type.union(TyClass.createGlobalType(def, context.forStub))
            }
            return type
        }
        is LuaTypeGuessable -> return def.inferFast(context)
        else -> return Ty.UNKNOWN
    }
}

private fun isGlobal(nameExpr: LuaNameExpr):Boolean {
    val minx = nameExpr as LuaNameExprMixin
    val gs = minx.greenStub
    return gs?.isGlobal ?: (resolveLocal(nameExpr, null) == null)
}

fun LuaLiteralExpr.infer(): ITy {
    return when (this.kind) {
        LuaLiteralKind.Bool -> Ty.BOOLEAN
        LuaLiteralKind.String -> Ty.STRING
        LuaLiteralKind.Number -> Ty.NUMBER
        LuaLiteralKind.Varargs -> {
            val o = PsiTreeUtil.getParentOfType(this, LuaFuncBodyOwner::class.java)
            o?.varargType ?: Ty.UNKNOWN
        }
        //LuaLiteralKind.Nil -> Ty.NIL
        else -> Ty.UNKNOWN
    }
}

enum class SelectType {
    OnlyMethod,
    OnlyField,
    Both
}

private fun processClassMember(clazz:ITyClass, propName:String, context:SearchContext, selectType:SelectType):ITy{
    val key = "${clazz.className}**${propName}"
    if(context.guessTextSet.contains(key)){
        return Ty.UNKNOWN
    }
    context.guessTextSet.add(key)
    val result = guessFieldType(propName, clazz, context, selectType)
    if (!Ty.isInvalid(result)) {
        context.guessTextSet.remove(key)
        return result
    }
    return Ty.UNKNOWN
}

private fun IFunSignature.inferGeneric(genericNames:Array<String>, ty:ITyGeneric):IFunSignature{
    var signature = this
    var returnTy = signature.returnTy
    val params = signature.params
    val multiParams = mutableListOf<LuaParamInfo>()
    multiParams.addAll(params)
    for ((index, genericName) in genericNames.withIndex())
    {
        if(returnTy is TySerializedFunction){
            returnTy = returnTy.inferGeneric(genericNames, ty)
        }else{
            if (returnTy.displayName == genericName) {
                returnTy = ty.getParamTy(index)
            }
        }
        for ((paramIndex, param) in multiParams.withIndex()){
            val paramTy = param.ty
            if(paramTy is TySerializedFunction){
                multiParams[paramIndex] = LuaParamInfo(param.name, paramTy.inferGeneric(genericNames, ty))
            }else if(paramTy is TySerializedGeneric){
                for ((genericParamIndex, genericParamTy) in paramTy.params.withIndex()){
                    if(genericParamTy.displayName == genericName){
                        val temp = mutableListOf<ITy>()
                        temp.addAll(paramTy.params)
                        temp[genericParamIndex] = ty.getParamTy(index)
                        multiParams[paramIndex] = LuaParamInfo(param.name, TySerializedGeneric(temp.toTypedArray(), paramTy.base))
                        break
                    }
                }
            }
            else{
                if (param.name == genericName) {
                    multiParams[paramIndex] = LuaParamInfo(param.name, ty.getParamTy(index))
                }
            }
        }
    }
    signature = FunSignature(signature.colonCall, returnTy, signature.varargTy, multiParams.toTypedArray())
    return signature
}

private fun TySerializedFunction.inferGeneric(genericNames:Array<String>, ty:ITyGeneric):TySerializedFunction{
    if(signatures.isEmpty()){
        return TySerializedFunction(mainSignature.inferGeneric(genericNames, ty),signatures)
    }
    val multiSignatures = mutableListOf<IFunSignature>()
    for (signature in signatures) {
        multiSignatures.add(signature.inferGeneric(genericNames, ty))
    }
    return TySerializedFunction(mainSignature.inferGeneric(genericNames, ty), multiSignatures.toTypedArray())
}

private fun LuaIndexExpr.infer(context: SearchContext): ITy {
    val indexExpr = this
    var parentTy: ITy? = null
    // xxx[yyy] as an array element?
    if (indexExpr.brack) {
        val tySet = indexExpr.guessParentType(context)
        var ty: ITy = Ty.UNKNOWN

        // Type[]
        TyUnion.each(tySet) {
            if (it is ITyArray) ty = ty.union(it.base)
        }
        if (ty !is TyUnknown) return ty

        // table<number, Type>
        TyUnion.each(tySet) {
            if (it is ITyGeneric) ty = ty.union(it.getParamTy(1))
        }
        if (ty !is TyUnknown) return ty

        parentTy = tySet
    }

    //from @type annotation
    val docTy = indexExpr.docTy
    if (docTy != null)
        return docTy

    // xxx.yyy = zzz
    //from value
    var result: ITy = Ty.UNKNOWN

    //from other class member
    val propName = indexExpr.name
    if (propName != null) {
        val prefixType = parentTy ?: indexExpr.guessParentType(context)
        if(Ty.isInvalid(prefixType)){
            return result
        }
        var selectType = SelectType.Both
        val nextSibling = indexExpr.nextSibling
        if(nextSibling != null){
            if(nextSibling.text == "." || nextSibling.text == ":") {
                selectType = SelectType.OnlyField
            }else if(nextSibling.text.startsWith("(")) {
                selectType = SelectType.OnlyMethod
            }
        }
        prefixType.each { ty ->
            if (ty is ITyGeneric) {
                val base = ty.base
                if(base is TyLazyClass) {
                    base.lazyInit(context)
                }
            }
        }

        //这里容易死循环,优先取library
        val projectTys = mutableSetOf<ITyClass>()
        prefixType.eachTopClass { clazz ->
            val classDef = LuaClassIndex.find(clazz.className, context)
            if (classDef != null) {
                if (LuaFileUtil.isStdLibFile(classDef.containingFile.virtualFile, project)) {
                    result = result.union(processClassMember(clazz, propName, context, selectType))
                } else {
                    projectTys.add(clazz)
                }
            } else {
                projectTys.add(clazz)
            }
            Ty.isInvalid(result)
        }

        if (Ty.isInvalid(result)) {
            for (clazz in projectTys) {
                result = result.union(processClassMember(clazz, propName, context, selectType))
            }
        }
        //泛型临时处理
        prefixType.each { ty ->
            if (ty is ITyGeneric)
            {
                val base = ty.base
                if(base is TySerializedClass){
                    base.lazyInit(context)
                    if(base.genericNames.isNotEmpty()){
                        val temp = result
                        temp.each { rt ->
                            if (rt is TySerializedFunction) {
                                    result = result.replace(rt, rt.inferGeneric(base.genericNames, ty))
                                } else {
                                    for ((index, genericName) in base.genericNames.withIndex()) {
                                        if (rt.displayName == genericName) {
                                            result = result.replace(rt, ty.getParamTy(index))
                                        }
                                    }
                                }

                            }
                    }
                }
            }
        }
        // table<string, K> -> member type is K
        prefixType.each { ty ->
            if (ty is ITyGeneric && ty.getParamTy(0) == Ty.STRING)
                result = result.union(ty.getParamTy(1))
        }
    }

    val assignStat = indexExpr.assignStat
    if (assignStat != null && Ty.isInvalid(result)) {
        val index = assignStat.getIndexFor(indexExpr)
        if(context.guessTextSet.contains(indexExpr.text)) {
            return result
        }
        context.guessTextSet.add(indexExpr.text)
        result = context.withIndex(index) {
            assignStat.valueExprList?.guessTypeAt(context) ?: Ty.UNKNOWN
        }
        if(!Ty.isInvalid(result)){
            context.guessTextSet.remove(indexExpr.text)
            return result
        }
    }

    return result
}

private fun guessFieldType(fieldName: String, type: ITyClass, context: SearchContext, selectType:SelectType): ITy {
    // _G.var = {}  <==>  var = {}
    if (type.className == Constants.WORD_G)
        return TyClass.createGlobalType(fieldName)

    var set:ITy = Ty.UNKNOWN
    //这个地方容易死循环
    LuaShortNamesManager.getInstance(context.project).processMembers(type, fieldName, context) {
        when (it) {
            is LuaFuncBodyOwner -> {
                if (selectType != SelectType.OnlyField) set = set.union(it.guessType(context))
                if (selectType == SelectType.OnlyMethod) {
                    if (!Ty.isInvalid(set)) return@processMembers false
                }
            }
            is LuaDocTagField ->{
                if (selectType != SelectType.OnlyMethod)
                    set = set.union(it.guessType(context))
                else {
                    val ret = it.guessType(context)
                    if (ret is TySerializedFunction) {
                        set = set.union(ret)
                        return@processMembers false
                    }
                }
                if (selectType == SelectType.OnlyField) {
                    if (!Ty.isInvalid(set)) return@processMembers false
                }
            }
            is LuaDocTableField->{
                set = set.union(it.guessType(context))
                if(!Ty.isInvalid(set)) return@processMembers false
            }
            is LuaTableField->{
                set = set.union(it.guessType(context))
                if(!Ty.isInvalid(set)) return@processMembers false
            }
            is LuaIndexExpr ->{
                val stat = it.assignStat
                if (stat != null) {
                    val ty = stat.comment?.docTy
                    if (ty != null) {
                        set = set.union(ty)
                    }else{
                        val valueExprList = stat.valueExprList
                        if(valueExprList != null){
                            //跳过 = nil推导
                            if(valueExprList.text == Constants.WORD_NIL){
                                return@processMembers true
                            }
                            val index = stat.getIndexFor(it)
                            if(!context.forStub){
                                set = set.union(context.withIndex(index) {
                                    valueExprList.guessTypeAt(context) ?: Ty.UNKNOWN
                                })
                            }
                        }
                    }
                }
            }
            else -> {

            }
        }

        true
    }
    return set
}

fun LuaTableExpr.infer(): ITy {
    val list = this.tableFieldList
    if (list.size == 1) {
        val valueExpr = list.first().valueExpr
        if (valueExpr is LuaLiteralExpr && valueExpr.kind == LuaLiteralKind.Varargs) {
            val func = PsiTreeUtil.getStubOrPsiParentOfType(this, LuaFuncBodyOwner::class.java)
            val ty = func?.varargType
            if (ty != null)
                return TyArray(ty)
        }
    }
    return TyTable(this)
}