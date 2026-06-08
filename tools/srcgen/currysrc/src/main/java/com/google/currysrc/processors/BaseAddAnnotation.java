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
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

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

  protected void addAnnotationToBodyDeclaration(
      ASTRewrite rewrite,
      BodyDeclaration node,
      AnnotationInfo annotationInfo,
      String reason) {

    insertAnnotationBefore(rewrite, node, annotationInfo, reason);

    // Notify any listeners that an annotation has been added.
    BodyDeclarationLocator locator = BodyDeclarationLocators.createLocators(node).getFirst();
    listener.onAddAnnotation(annotationInfo, locator, node);
  }

  /**
   * Get the simpler name from the qualifiedName,
   */
  private static String getSimpleClassName(String qualifiedName) {
    int lastDot = qualifiedName.lastIndexOf('.');
    return lastDot == -1 ? qualifiedName : qualifiedName.substring(lastDot + 1);
  }

  /**
   * Check to see if node already has an annotation.
   */
  public static boolean hasAnnotation(BodyDeclaration node, AnnotationInfo annotationInfo) {
    String qualifiedName = annotationInfo.getQualifiedName();
    String simpleName = getSimpleClassName(qualifiedName);
    // node.modifiers() returns a list of IExtendedModifier (Modifier or Annotation)
    for (Object modifier : node.modifiers()) {
      if (modifier instanceof Annotation) {
        Annotation annotation = (Annotation) modifier;
        String name = annotation.getTypeName().getFullyQualifiedName();

        if (name.equals(qualifiedName) || name.equals(simpleName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Add an annotation to a {@link BodyDeclaration} node.
   */
  protected static void insertAnnotationBefore(
      ASTRewrite rewrite, BodyDeclaration node,
      AnnotationInfo annotationInfo, String reason) {
    String qualifiedName = annotationInfo.getQualifiedName();

    // Do not add an annotation if it already exists.
    if (hasAnnotation(node, annotationInfo)) {
      return;
    }

    Map<String, Object> elements = annotationInfo.getProperties();

    // Construct a string representation of the annotation.
    StringBuilder builder = new StringBuilder();
    builder.append("@");
    builder.append(qualifiedName);
    if (!elements.isEmpty()) {
      builder.append("(");
      if (elements.size() == 1 && elements.containsKey("value")) {
        builder.append(createAnnotationValue(rewrite, elements.get("value")));
      } else {
        String separator = "";
        for (Entry<String, Object> entry : elements.entrySet()) {
          builder.append(separator);
          separator = ", ";

          builder.append(entry.getKey());
          builder.append(" = ");
          builder.append(createAnnotationValue(rewrite, entry.getValue()));
        }
      }
      builder.append(")");
    }

    // Append a line comment after the annotation with the reason for it, if provided.
    if (reason != null) {
      builder.append(" // ");
      builder.append(reason);
    }

    // Create a placeholder containing the annotation.
    ASTNode placeholderNode = rewrite.createStringPlaceholder(builder.toString(), ASTNode.NORMAL_ANNOTATION);
    ListRewrite listRewrite = rewrite.getListRewrite(node, node.getModifiersProperty());
    listRewrite.insertFirst(placeholderNode, null);
  }

  private static String createAnnotationValue(ASTRewrite rewrite, Object value) {
    if (value instanceof String) {
      StringLiteral stringLiteral = rewrite.getAST().newStringLiteral();
      stringLiteral.setLiteralValue((String) value);
      return stringLiteral.getEscapedValue();
    }
    if ((value instanceof Integer) || (value instanceof Long)) {
      NumberLiteral numberLiteral = rewrite.getAST().newNumberLiteral();
      numberLiteral.setToken(value.toString());
      return numberLiteral.getToken();
    }
    if (value instanceof Placeholder placeholder) {
      return placeholder.getText();
    }
    throw new IllegalStateException("Unknown value '" + value + "' of class " +
        (value == null ? "NULL" : value.getClass()));
  }
}
