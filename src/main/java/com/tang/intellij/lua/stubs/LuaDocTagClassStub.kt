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

package com.tang.intellij.lua.stubs

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.tang.intellij.lua.comment.psi.LuaDocTagClass
import com.tang.intellij.lua.comment.psi.impl.LuaDocTagClassImpl
import com.tang.intellij.lua.psi.LuaElementType
import com.tang.intellij.lua.psi.aliasName
import com.tang.intellij.lua.stubs.index.StubKeys
import com.tang.intellij.lua.ty.TyClass
import com.tang.intellij.lua.ty.createSerializedClass

/**

 * Created by tangzx on 2016/11/28.
 */
class LuaDocTagClassType : LuaStubElementType<LuaDocTagClassStub, LuaDocTagClass>("DOC_CLASS") {

    override fun createPsi(luaDocClassStub: LuaDocTagClassStub): LuaDocTagClass {
        return LuaDocTagClassImpl(luaDocClassStub, this)
    }

    override fun createStub(luaDocTagClass: LuaDocTagClass, stubElement: StubElement<*>): LuaDocTagClassStub {
        val classNameRefList = luaDocTagClass.superClassNameRef?.classNameRefList;
        val superClassNames = mutableListOf<String>()
        if(classNameRefList != null){
            classNameRefList.forEach{
                val superClassName = it.name
                if(superClassName != null) {
                    superClassNames.add(superClassName)
                }
            }
        }
        val aliasName: String? = luaDocTagClass.aliasName
        val genericTypes = mutableListOf<String>()
        val genericParameters = luaDocTagClass.genericParameters
        if(genericParameters != null){
            if(genericParameters.genericParameterList.isNotEmpty()){
                genericParameters.genericParameterList.forEach {
                    val genericName = it.name
                    if(genericName != null) {
                        genericTypes.add(genericName)
                    }
                }
            }
        }
        return LuaDocTagClassStubImpl(luaDocTagClass.name, genericTypes.toTypedArray(), aliasName, superClassNames.toTypedArray(), luaDocTagClass.isDeprecated, stubElement)
    }

    override fun serialize(luaDocClassStub: LuaDocTagClassStub, stubOutputStream: StubOutputStream) {
        stubOutputStream.writeName(luaDocClassStub.className)
        stubOutputStream.writeNames(luaDocClassStub.genericNames)
        stubOutputStream.writeName(luaDocClassStub.aliasName)
        stubOutputStream.writeNames(luaDocClassStub.superClassName)
        stubOutputStream.writeBoolean(luaDocClassStub.isDeprecated)
    }


    override fun deserialize(stubInputStream: StubInputStream, stubElement: StubElement<*>): LuaDocTagClassStub {
        val className = stubInputStream.readName()
        val genericNames = stubInputStream.readNames()
        val aliasName = stubInputStream.readName()
        val superClassName = stubInputStream.readNames()
        val isDeprecated = stubInputStream.readBoolean()
        return LuaDocTagClassStubImpl(StringRef.toString(className)!!,
                genericNames,
                StringRef.toString(aliasName),
                superClassName,
                isDeprecated,
                stubElement)
    }

    override fun indexStub(luaDocClassStub: LuaDocTagClassStub, indexSink: IndexSink) {
        val classType = luaDocClassStub.classType
        indexSink.occurrence(StubKeys.CLASS, classType.className)
        indexSink.occurrence(StubKeys.SHORT_NAME, classType.className)

        val superClassName = classType.superClassNames
        if (superClassName.isNotEmpty()) {
            superClassName.forEach{indexSink.occurrence(StubKeys.SUPER_CLASS, it)}
        }
    }
}

interface LuaDocTagClassStub : StubElement<LuaDocTagClass> {
    val className: String
    val genericNames:Array<String>
    val aliasName: String?
    val superClassName: Array<String>
    val classType: TyClass
    val isDeprecated: Boolean
}

class LuaDocTagClassStubImpl(override val className: String,
                             override val genericNames:Array<String>,
                             override val aliasName: String?,
                             override val superClassName: Array<String>,
                             override val isDeprecated: Boolean,
                             parent: StubElement<*>)
    : LuaDocStubBase<LuaDocTagClass>(parent, LuaElementType.CLASS_DEF), LuaDocTagClassStub {

    override val classType: TyClass
        get() {
            val luaType = createSerializedClass(className, genericNames, className, superClassName)
            luaType.aliasName = aliasName
            return luaType
        }
}