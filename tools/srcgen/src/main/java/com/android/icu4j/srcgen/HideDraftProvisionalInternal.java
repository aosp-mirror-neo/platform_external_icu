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
package com.android.icu4j.srcgen;

import com.google.common.collect.ImmutableSet;
import com.google.currysrc.api.process.Context;
import com.google.currysrc.api.process.Reporter;
import com.google.currysrc.processors.BaseAddAnnotation;
import com.google.currysrc.processors.BaseJavadocNodeScanner;
import com.google.currysrc.processors.HidePublicClasses;
import com.google.currysrc.processors.JavadocVisitor;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * Adds {@literal @}hide to all JavaDoc comments that contain any of {@literal @}draft,
 * {@literal @}provisional, {@literal @}internal}.
 */
public class HideDraftProvisionalInternal extends BaseAddAnnotation implements JavadocVisitor {
  private static final Set<String> toMatch = ImmutableSet.of("@draft", "@provisional", "@internal");
  private static final String HIDE_HIDDEN_ON_ANDROID =
      "draft / provisional / internal are hidden on Android";

  private boolean mustHide(Javadoc javadoc) {
    for (TagElement tagElement : (List<TagElement>) javadoc.tags()) {
      if (tagElement.getTagName() != null
          && HideDraftProvisionalInternal.toMatch.contains(tagElement.getTagName().toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void process(Context context, CompilationUnit cu) {
    BaseJavadocNodeScanner.scanJavadoc(context, cu, this);
  }

  @Override
  public void visit(Reporter reporter, Javadoc javadoc, ASTRewrite rewrite) {
    if (mustHide(javadoc)) {
      ASTNode parent = javadoc.getParent();
      if (parent instanceof BodyDeclaration) {
        BodyDeclaration body = (BodyDeclaration) parent;
        BaseAddAnnotation.insertAnnotationBefore(rewrite, body, HidePublicClasses.HIDE,
            HIDE_HIDDEN_ON_ANDROID);
      }
    }
  }

  @Override public String toString() {
    return "HideDraftProvisionalInternal{}";
  }
}
