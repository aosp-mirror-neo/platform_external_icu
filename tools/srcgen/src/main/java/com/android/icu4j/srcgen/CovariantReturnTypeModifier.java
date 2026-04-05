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
package com.android.icu4j.srcgen;

import com.google.currysrc.api.process.Context;
import com.google.currysrc.api.process.Processor;
import com.google.currysrc.api.process.ast.BodyDeclarationLocator;
import com.google.currysrc.api.process.ast.BodyDeclarationLocatorStore;
import com.google.currysrc.api.process.ast.BodyDeclarationLocatorStore.Mapping;
import com.google.currysrc.api.process.ast.BodyDeclarationLocators;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class CovariantReturnTypeModifier implements Processor {

  private final BodyDeclarationLocatorStore<String> locator2ReturnType;

  public static CovariantReturnTypeModifier fromJsonFile(Path file) throws IOException {
    Gson gson = new GsonBuilder().create();
    BodyDeclarationLocatorStore<String> store = new BodyDeclarationLocatorStore<>();
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
          String returnType = null;
          while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("@location")) {
              locator = BodyDeclarationLocators.fromStringForm(reader.nextString());
            } else if (name.equals("returnType")) {
              returnType = reader.nextString();
            } else {
              reader.skipValue();
            }
          }
          if (locator == null || returnType == null) {
            throw new IllegalStateException("Missing location or returnType");
          }
          store.add(locator, returnType);
          reader.endObject();
        }
        reader.endArray();
      } catch (RuntimeException e) {
        throw new MalformedJsonException("Error parsing JSON at " + reader.getPath(), e);
      }
    }
    return new CovariantReturnTypeModifier(store);
  }

  private CovariantReturnTypeModifier(BodyDeclarationLocatorStore<String> locator2ReturnType) {
    this.locator2ReturnType = locator2ReturnType;
  }

  @Override
  public void process(Context context, CompilationUnit cu) {
    final ASTRewrite rewrite = context.rewrite();
    ASTVisitor visitor = new ASTVisitor(false) {
      @Override
      public boolean visit(MethodDeclaration node) {
        Mapping<String> mapping = locator2ReturnType.findMapping(node);
        if (mapping != null) {
          String returnTypeStr = mapping.getValue();
          Type newReturnType = (Type) rewrite.createStringPlaceholder(returnTypeStr, org.eclipse.jdt.core.dom.ASTNode.SIMPLE_TYPE);
          rewrite.set(node, MethodDeclaration.RETURN_TYPE2_PROPERTY, newReturnType, null);
        }
        return true;
      }
    };
    cu.accept(visitor);
  }
}