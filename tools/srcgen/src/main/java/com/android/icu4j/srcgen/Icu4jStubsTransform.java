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

import com.google.common.collect.Lists;
import com.google.currysrc.Main;
import com.google.currysrc.api.RuleSet;
import com.google.currysrc.api.input.InputFileGenerator;
import com.google.currysrc.api.output.OutputSourceFileGenerator;
import com.google.currysrc.api.process.Rule;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static com.google.currysrc.api.process.Rules.createOptionalRule;

public class Icu4jStubsTransform {

  private static final boolean DEBUG = false;

  private Icu4jStubsTransform() {
  }

  public static void main(String[] args) throws Exception {
    new Main(DEBUG).execute(new Icu4jStubsRules(args));
  }

  private static class Icu4jStubsRules implements RuleSet {

    private final InputFileGenerator inputFileGenerator;
    private final List<Rule> rules;
    private final OutputSourceFileGenerator outputSourceFileGenerator;

    public Icu4jStubsRules(String[] args) throws Exception {
      if (args.length < 3) {
        throw new IllegalArgumentException("Usage: Icu4jStubsTransform <input-dir> <json-file> <output-dir>");
      }

      String inputDir = args[0];
      String jsonFile = args[1];
      String outputDir = args[2];

      inputFileGenerator = Icu4jTransformRules.createInputFileGenerator(new String[]{inputDir});
      rules = createTransformRules(jsonFile);
      outputSourceFileGenerator = Icu4jTransformRules.createOutputFileGenerator(outputDir);
    }

    @Override
    public List<Rule> getRuleList(File ignored) {
      return rules;
    }

    @Override
    public InputFileGenerator getInputFileGenerator() {
      return inputFileGenerator;
    }

    @Override
    public OutputSourceFileGenerator getOutputSourceFileGenerator() {
      return outputSourceFileGenerator;
    }

    private static List<Rule> createTransformRules(String jsonFile) throws Exception {
      List<Rule> rules = Lists.newArrayList();
      rules.add(createOptionalRule(CovariantReturnTypeModifier.fromJsonFile(Paths.get(jsonFile))));
      rules.add(createOptionalRule(new RemoveCloneableModifier(Lists.newArrayList(
          "android.icu.text.DateIntervalFormat",
          "android.icu.text.DecimalFormat",
          "android.icu.text.MessageFormat",
          "android.icu.text.NumberFormat",
          "android.icu.text.RuleBasedCollator",
          "android.icu.text.SimpleDateFormat",
          "android.icu.text.TimeZoneFormat",
          "android.icu.text.UnicodeSet",
          "android.icu.text.RuleBasedBreakIterator",
          "android.icu.text.RuleBasedNumberFormat",
          "android.icu.text.TimeUnitFormat",
          "android.icu.util.GregorianCalendar",
          "android.icu.util.RuleBasedTimeZone",
          "android.icu.util.SimpleTimeZone",
          "android.icu.util.ULocale",
          "android.icu.util.VTimeZone"
      ))));
      return rules;
    }
  }
}
