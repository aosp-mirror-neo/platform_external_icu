/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.icu4j.srcgen;

import com.google.currysrc.api.process.Context;
import com.google.currysrc.api.process.Processor;
import com.google.currysrc.api.process.ast.TypeLocator;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class RemoveCloneableModifier implements Processor {

  private final List<TypeLocator> classLocators;

  public RemoveCloneableModifier(List<String> classNames) {
    this.classLocators = TypeLocator.createLocatorsFromStrings(classNames.toArray(new String[0]));
  }

  @Override
  public void process(Context context, CompilationUnit cu) {
    final ASTRewrite rewrite = context.rewrite();
    ASTVisitor visitor = new ASTVisitor(false) {
      @Override
      public boolean visit(TypeDeclaration node) {
        for (TypeLocator locator : classLocators) {
          if (locator.matches(node)) {
            ListRewrite listRewrite = rewrite.getListRewrite(node, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY);
            for (Object superInterface : listRewrite.getOriginalList()) {
              if (superInterface.toString().equals("Cloneable") || superInterface.toString().equals("java.lang.Cloneable")) {
                listRewrite.remove((org.eclipse.jdt.core.dom.ASTNode) superInterface, null);
              }
            }
          }
        }
        return true;
      }
    };
    cu.accept(visitor);
  }
}
