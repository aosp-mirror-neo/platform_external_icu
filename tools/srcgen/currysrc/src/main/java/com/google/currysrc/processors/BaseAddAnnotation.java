/*
 * Copyright (C) 2026 The Android Open Source Project
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

import com.google.currysrc.api.process.Processor;
import com.google.currysrc.api.process.ast.BodyDeclarationLocator;
import com.google.currysrc.api.process.ast.BodyDeclarationLocators;
import com.google.currysrc.processors.AnnotationInfo.Placeholder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.text.edits.TextEditGroup;

/**
 * Provides support for adding annotations to {@link BodyDeclaration}s.
 */
public abstract class BaseAddAnnotation implements Processor {

  private Listener listener;

  public interface Listener {
    /**
     * Called when an annotation is added to a class or one of its members.
     *
     * @param annotationInfo the information about the annotation that was added.
     * @param locator the locator of the element to which the annotation was added.
     * @param bodyDeclaration the modified class or class member.
     */
    void onAddAnnotation(AnnotationInfo annotationInfo, BodyDeclarationLocator locator,
        BodyDeclaration bodyDeclaration);
  }

  @SuppressWarnings("unused")
  protected BaseAddAnnotation() {
    this.listener = (c, l, b) -> {};
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  protected void addAnnotationToBodyDeclaration(ASTRewrite rewrite, BodyDeclaration node, AnnotationInfo annotationInfo) {
    insertAnnotationBefore(rewrite, node, annotationInfo);

    // Notify any listeners that an annotation has been added.
    BodyDeclarationLocator locator = BodyDeclarationLocators.createLocators(node).getFirst();
    listener.onAddAnnotation(annotationInfo, locator, node);
  }

  /**
   * Add an annotation to a {@link BodyDeclaration} node.
   */
  private static void insertAnnotationBefore(
      ASTRewrite rewrite, BodyDeclaration node,
      AnnotationInfo annotationInfo) {
    final TextEditGroup editGroup = null;
    AST ast = node.getAST();
    Map<String, Object> elements = annotationInfo.getProperties();
    Annotation annotation;
    if (elements.isEmpty()) {
      annotation = ast.newMarkerAnnotation();
    } else if (elements.size() == 1 && elements.containsKey("value")) {
      SingleMemberAnnotation singleMemberAnnotation = ast.newSingleMemberAnnotation();
      singleMemberAnnotation.setValue(createAnnotationValue(rewrite, elements.get("value")));
      annotation = singleMemberAnnotation;
    } else {
      NormalAnnotation normalAnnotation = ast.newNormalAnnotation();
      @SuppressWarnings("unchecked")
      List<MemberValuePair> values = normalAnnotation.values();
      for (Entry<String, Object> entry : elements.entrySet()) {
        MemberValuePair pair = ast.newMemberValuePair();
        pair.setName(ast.newSimpleName(entry.getKey()));
        pair.setValue(createAnnotationValue(rewrite, entry.getValue()));
        values.add(pair);
      }
      annotation = normalAnnotation;
    }

    annotation.setTypeName(ast.newName(annotationInfo.getQualifiedName()));
    ListRewrite listRewrite = rewrite.getListRewrite(node, node.getModifiersProperty());
    listRewrite.insertFirst(annotation, editGroup);
  }

  private static Expression createAnnotationValue(ASTRewrite rewrite, Object value) {
    if (value instanceof String) {
      StringLiteral stringLiteral = rewrite.getAST().newStringLiteral();
      stringLiteral.setLiteralValue((String) value);
      return stringLiteral;
    }
    if ((value instanceof Integer) || (value instanceof Long)) {
      NumberLiteral numberLiteral = rewrite.getAST().newNumberLiteral();
      numberLiteral.setToken(value.toString());
      return numberLiteral;
    }
    if (value instanceof Placeholder placeholder) {
      // The cast is safe because createStringPlaceholder returns an instance of type NumberLiteral
      // which is an Expression.
      return (Expression)
          rewrite.createStringPlaceholder(placeholder.getText(), ASTNode.NUMBER_LITERAL);
    }
    throw new IllegalStateException("Unknown value '" + value + "' of class " +
        (value == null ? "NULL" : value.getClass()));
  }
}
