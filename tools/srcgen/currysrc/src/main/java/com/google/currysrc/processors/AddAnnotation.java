/*
 * Copyright (C) 2018 The Android Open Source Project
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
import com.google.currysrc.api.process.Processor;
import com.google.currysrc.api.process.ast.BodyDeclarationLocator;
import com.google.currysrc.api.process.ast.BodyDeclarationLocatorStore;
import com.google.currysrc.api.process.ast.BodyDeclarationLocatorStore.Mapping;
import com.google.currysrc.api.process.ast.BodyDeclarationLocators;
import com.google.currysrc.processors.AnnotationInfo.AnnotationClass;
import com.google.currysrc.processors.AnnotationInfo.Placeholder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * Add annotations to an allowlist of classes and class members.
 */
public class AddAnnotation extends BaseAddAnnotation {

  private final BodyDeclarationLocatorStore<AnnotationInfo> locator2AnnotationInfo;

  private final String reason;

  /**
   * Create a {@link Processor} that will add annotations of the supplied class to classes and class
   * members specified in the supplied file.
   *
   * <p>The supplied JSON file must consist of an outermost array containing objects with the
   * following structure:
   *
   * <pre>{@code
   * {
   *  "@location": "<body declaration location>",
   *  [<property>[, <property>]*]?
   * }
   * }</pre>
   *
   * <p>Where:
   * <ul>
   * <li>{@code <body declaration location>} is in the format expected by
   * {@link BodyDeclarationLocators#fromStringForm(String)}. This is the only required field.</li>
   * <li>{@code <property>} is a property of the annotation and is of the format
   * {@code "<name>": <value>} where {@code <name>} is the name of the annotations property which
   * must correspond to the name of a property in the supplied {@link AnnotationClass} and
   * {@code <value>} is the value that will be supplied for the property. A {@code <value>} must
   * match the type of the property in the supplied {@link AnnotationClass}.
   * </ul>
   *
   * <p>A {@code <value>} can be one of the following types:
   * <ul>
   * <li>{@code <int>} and {@code <long>} which are literal JSON numbers that are inserted into the
   * source as literal primitive values. The corresponding property type in the supplied
   * {@link AnnotationClass} must be {@code int.class} or {@code long.class} respectively.</li>
   * <li>{@code <string>} is a quoted JSON string that is inserted into the source as a literal
   * string.The corresponding property type in the supplied {@link AnnotationClass} must be
   * {@code String.class}.</li>
   * <li>{@code <placeholder>} is a quoted JSON string that is inserted into the source as if it
   * was a constant expression. It is used to reference constants in annotation values, e.g. {@code
   * android.compat.annotation.UnsupportedAppUsage.VERSION_CODES.P}. It can be used for any property
   * type and will be type checked when the generated code is compiled.</li>
   * </ul>
   *
   * <p>See external/icu/tools/srcgen/unsupported-app-usage.json for an example.
   *
   * @param annotationClass the type of the annotation to add, includes class name, property names
   * and types.
   * @param file the JSON file.
   */
  public static AddAnnotation fromJsonFile(AnnotationClass annotationClass, Path file)
      throws IOException {
    Gson gson = new GsonBuilder().create();
    BodyDeclarationLocatorStore<AnnotationInfo> annotationStore =
        new BodyDeclarationLocatorStore<>();
    String jsonStringWithoutComments =
        Files.lines(file, StandardCharsets.UTF_8)
            .filter(l -> !l.trim().startsWith("//"))
            .collect(Collectors.joining("\n"));
    try (JsonReader reader = gson.newJsonReader(new StringReader(jsonStringWithoutComments))) {
      try {
        reader.beginArray();

        while (reader.hasNext()) {
          reader.beginObject();

          BodyDeclarationLocator locator = null;

          String annotationClassName = annotationClass.getName();
          Map<String, Object> annotationProperties = new LinkedHashMap<>();

          while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
              case "@location":
                locator = BodyDeclarationLocators.fromStringForm(reader.nextString());
                break;
              default:
                Class<?> propertyType = annotationClass.getPropertyType(name);
                Object value;
                JsonToken token = reader.peek();
                if (token == JsonToken.STRING) {
                  String text = reader.nextString();
                  if (propertyType != String.class) {
                    value = new Placeholder(text);
                  } else {
                    // Literal string.
                    value = text;
                  }
                } else {
                  if (propertyType == boolean.class) {
                    value = reader.nextBoolean();
                  } else if (propertyType == int.class) {
                    value = reader.nextInt();
                  } else if (propertyType == double.class) {
                    value = reader.nextDouble();
                  } else if (propertyType == long.class) {
                    value = reader.nextLong();
                  } else {
                    throw new IllegalStateException(
                        "Unknown property type: " + propertyType + " for " + annotationClassName);
                  }
                }

                annotationProperties.put(name, value);
            }
          }

          if (locator == null) {
            throw new IllegalStateException("Missing location");
          }
          AnnotationInfo annotationInfo = new AnnotationInfo(annotationClass, annotationProperties);
          annotationStore.add(locator, annotationInfo);

          reader.endObject();
        }

        reader.endArray();
      } catch (RuntimeException e) {
        throw new MalformedJsonException("Error parsing JSON at " + reader.getPath(), e);
      }
    }

    return new AddAnnotation(annotationStore, null);
  }

  /**
   * Create a {@link Processor} that will add annotations of the supplied class to classes and class
   * members specified in the supplied file.
   *
   * <p>Each line in the supplied file can be empty, start with a {@code #} or be in the format
   * expected by {@link BodyDeclarationLocators#fromStringForm(String)}. Lines that are empty or
   * start with a {@code #} are ignored.
   *
   * @param annotationClassName the fully qualified class name of the annotation to add.
   * @param file the flat file.
   */
  public static AddAnnotation markerAnnotationFromFlatFile(String annotationClassName, Path file) {
    List<BodyDeclarationLocator> locators =
        BodyDeclarationLocators.readBodyDeclarationLocators(file);
    return markerAnnotationFromLocators(annotationClassName, locators);
  }

  /**
   * Create a {@link Processor} that will add annotations of the supplied class to classes and class
   * members specified in the locators.
   *
   * @param annotationClassName the fully qualified class name of the annotation to add.
   * @param locators list of BodyDeclarationLocator
   */
  public static AddAnnotation markerAnnotationFromLocators(String annotationClassName,
      List<BodyDeclarationLocator> locators) {
    return markerAnnotationFromLocators(annotationClassName, locators, null);
  }

  /**
   * Create a {@link Processor} that will add annotations of the supplied class to classes and class
   * members specified in the locators.
   *
   * @param annotationClassName the fully qualified class name of the annotation to add.
   * @param locators list of BodyDeclarationLocator
   * @param reason the reason to use.
   */
  public static AddAnnotation markerAnnotationFromLocators(
      String annotationClassName,
      List<BodyDeclarationLocator> locators,
      String reason) {
    AnnotationClass annotationClass = new AnnotationClass(annotationClassName);
    AnnotationInfo markerAnnotation = new AnnotationInfo(annotationClass, Collections.emptyMap());
    BodyDeclarationLocatorStore<AnnotationInfo> locator2AnnotationInfo =
        new BodyDeclarationLocatorStore<>();
    locators.forEach(l -> locator2AnnotationInfo.add(l, markerAnnotation));
    return new AddAnnotation(locator2AnnotationInfo, reason);
  }

    /**
     * Create a {@link Processor} that will add annotations of the supplied class to classes and class
     * members specified in the supplied file. The annotations will have a single property.
     *
     * <p>Each line in the supplied file can be empty, start with a {@code #} or be in the format
     * expected by {@link BodyDeclarationLocators#fromStringForm(String)}. Lines that are empty or
     * start with a {@code #} are ignored.
     *
     * @param annotationClassName the fully qualified class name of the annotation to add.
     * @param propertyName the name of the property to add
     * @param propertyClass the class of the property to add (use {@link Enum} for enums)
     * @param propertyValue the value of the property to add
     * @param file the flat file.
     */
  public static AddAnnotation markerAnnotationWithPropertyFromFlatFile(
      String annotationClassName,
      String propertyName,
      Class<?> propertyClass,
      Object propertyValue,
      Path file) {
    List<BodyDeclarationLocator> locators =
        BodyDeclarationLocators.readBodyDeclarationLocators(file);
    return markerAnnotationWithPropertyFromLocators(
        annotationClassName, propertyName, propertyClass, propertyValue, locators);
  }

  /**
   * Create a {@link Processor} that will add annotations of the supplied class to classes and class
   * members specified in the locators. The annotations will have a single property.
   *
   * @param annotationClassName the fully qualified class name of the annotation to add.
   * @param propertyName the name of the property to add
   * @param propertyClass the class of the property to add (use {@link Enum} for enums)
   * @param propertyValue the value of the property to add
   * @param locators list of BodyDeclarationLocator
   */
  public static AddAnnotation markerAnnotationWithPropertyFromLocators(
      String annotationClassName,
      String propertyName,
      Class<?> propertyClass,
      Object propertyValue,
      List<BodyDeclarationLocator> locators) {
    AnnotationClass annotationClass =
        new AnnotationClass(annotationClassName).addProperty(propertyName, propertyClass);
    AnnotationInfo markerAnnotation =
        new AnnotationInfo(annotationClass, Collections.singletonMap(propertyName, propertyValue));
    BodyDeclarationLocatorStore<AnnotationInfo> locator2AnnotationInfo =
        new BodyDeclarationLocatorStore<>();
    locators.forEach(l -> locator2AnnotationInfo.add(l, markerAnnotation));
    return new AddAnnotation(locator2AnnotationInfo, null);
  }

  private AddAnnotation(
      BodyDeclarationLocatorStore<AnnotationInfo> locator2AnnotationInfo,
      String reason) {
    this.locator2AnnotationInfo = locator2AnnotationInfo;
    this.reason = reason;
  }

  @Override
  public void process(Context context, CompilationUnit cu) {
    final ASTRewrite rewrite = context.rewrite();
    ASTVisitor visitor = new ASTVisitor(false /* visitDocTags */) {
      @Override
      public boolean visit(AnnotationTypeDeclaration node) {
        return handleBodyDeclaration(rewrite, node);
      }

      @Override
      public boolean visit(AnnotationTypeMemberDeclaration node) {
        return handleBodyDeclaration(rewrite, node);
      }

      @Override
      public boolean visit(EnumConstantDeclaration node) {
        return handleBodyDeclaration(rewrite, node);
      }

      @Override
      public boolean visit(EnumDeclaration node) {
        return handleBodyDeclaration(rewrite, node);
      }

      @Override
      public boolean visit(FieldDeclaration node) {
        return handleBodyDeclaration(rewrite, node);
      }

      @Override
      public boolean visit(MethodDeclaration node) {
        return handleBodyDeclaration(rewrite, node);
      }

      @Override
      public boolean visit(TypeDeclaration node) {
        return handleBodyDeclaration(rewrite, node);
      }
    };
    cu.accept(visitor);
  }

  private boolean handleBodyDeclaration(ASTRewrite rewrite, BodyDeclaration node) {
    Mapping<AnnotationInfo> mapping = locator2AnnotationInfo.findMapping(node);
    if (mapping != null) {
      AnnotationInfo annotationInfo = mapping.getValue();
      addAnnotationToBodyDeclaration(rewrite, node, annotationInfo, reason);
    }
    return true;
  }
}
