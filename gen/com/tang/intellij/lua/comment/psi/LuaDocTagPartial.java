// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.tang.intellij.lua.stubs.LuaDocTagPartialStub;
import com.tang.intellij.lua.ty.ITy;

public interface LuaDocTagPartial extends LuaDocTag, StubBasedPsiElement<LuaDocTagPartialStub> {

  @Nullable
  LuaDocTy getTy();

  @NotNull
  ITy getType();

}
