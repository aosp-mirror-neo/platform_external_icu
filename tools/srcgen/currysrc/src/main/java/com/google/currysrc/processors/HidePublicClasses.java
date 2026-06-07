/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.currysrc.processors;

import com.google.currysrc.api.process.Context;
import com.google.currysrc.api.process.ast.TypeLocator;
import com.google.currysrc.processors.AnnotationInfo.AnnotationClass;
import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * Hides any public class that is not found in the allowlist.
 */
public final class HidePublicClasses extends BaseAddAnnotation {

  private final List<TypeLocator> allowlist;
  private final String reason;

  public HidePublicClasses(List<TypeLocator> allowlist, String reason) {
    this.allowlist = allowlist;
    this.reason = reason;
  }

  public static final AnnotationInfo HIDE =
      new AnnotationInfo(
          new AnnotationClass("android.annotation.Hide"),
        Collections.emptyMap());

  @Override public void process(Context context, CompilationUnit cu) {
    ASTRewrite rewrite = context.rewrite();
    cu.accept(new ASTVisitor() {
      @Override
      public boolean visit(AnnotationTypeDeclaration node) {
        return visitAbstract(node);
      }

      @Override
      public boolean visit(TypeDeclaration node) {
        return visitAbstract(node);
      }

      @Override
      public boolean visit(EnumDeclaration node) {
        return visitAbstract(node);
      }

      private boolean visitAbstract(AbstractTypeDeclaration node) {
        boolean mustHide = false;
        if ((node.getModifiers() & Modifier.PUBLIC) > 0) {
          mustHide = true;
          for (TypeLocator allowlistedType : allowlist) {
            if (allowlistedType.matches(node)) {
              mustHide = false;
              break;
            }
          }
        }
        if (mustHide) {
          addAnnotationToBodyDeclaration(rewrite, node, HIDE, reason);
        }
        return true;
      }
    });
  }

  @Override
  public String toString() {
    return "HidePublicClasses{" +
        "allowlist=" + allowlist +
        '}';
  }
}
