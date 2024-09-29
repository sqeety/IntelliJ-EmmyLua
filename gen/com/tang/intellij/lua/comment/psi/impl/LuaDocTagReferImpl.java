// This is a generated file. Not intended for manual editing.
package com.tang.intellij.lua.comment.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.tang.intellij.lua.comment.psi.LuaDocTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.tang.intellij.lua.comment.psi.*;
import com.intellij.psi.PsiReference;
import com.tang.intellij.lua.psi.LuaTypeGuessable;
import com.tang.intellij.lua.ty.ITy;

public class LuaDocTagReferImpl extends ASTWrapperPsiElement implements LuaDocTagRefer {

  public LuaDocTagReferImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull LuaDocVisitor visitor) {
    visitor.visitTagRefer(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof LuaDocVisitor) accept((LuaDocVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public LuaDocClassOrGlobalNameRef getClassOrGlobalNameRef() {
    return PsiTreeUtil.getChildOfType(this, LuaDocClassOrGlobalNameRef.class);
  }

  @Override
  @Nullable
  public PsiElement getId() {
    return findChildByType(ID);
  }

  @Override
  @Nullable
  public PsiReference getReference() {
    return LuaDocPsiImplUtilKt.getReference(this);
  }

  @Override
  @NotNull
  public ITy getType() {
    return LuaDocPsiImplUtilKt.getType(this);
  }

  @Override
  @Nullable
  public LuaTypeGuessable getReferPsi() {
    return LuaDocPsiImplUtilKt.getReferPsi(this);
  }

}
