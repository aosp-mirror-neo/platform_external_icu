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
package com.android.icu.test.sdk36;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertNotNull;

import android.icu.text.BreakIterator;
import android.icu.text.Collator;
import android.icu.text.CurrencyPluralInfo;
import android.icu.text.DateFormat;
import android.icu.text.DateFormatSymbols;
import android.icu.text.DateIntervalFormat;
import android.icu.text.DateIntervalInfo;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.DecimalFormat;
import android.icu.text.DecimalFormatSymbols;
import android.icu.text.MessageFormat;
import android.icu.text.MessagePattern;
import android.icu.text.NumberFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.text.TimeZoneFormat;
import android.icu.text.UCharacterIterator;
import android.icu.text.UnicodeSet;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

// Unit test for the covariant return type. http://b/493857949
@RunWith(JUnit4.class)
public class CovariantAllTest {
    @Test
    public void testAllCovariantMethods() throws Exception {
        {
            Collator obj = Collator.getInstance();
            Collator cloned = (Collator) obj.clone();
            assertNotNull(cloned);
        }
        {
            BreakIterator obj = BreakIterator.getWordInstance();
            BreakIterator cloned = (BreakIterator) obj.clone();
            assertNotNull(cloned);
        }
        {
            CurrencyPluralInfo obj = CurrencyPluralInfo.getInstance();
            CurrencyPluralInfo cloned = (CurrencyPluralInfo) obj.clone();
            assertNotNull(cloned);
        }
        {
            DateFormat obj = DateFormat.getInstance();
            DateFormat cloned = (DateFormat) obj.clone();
            assertNotNull(cloned);
        }
        {
            DateFormatSymbols obj = DateFormatSymbols.getInstance();
            DateFormatSymbols cloned = (DateFormatSymbols) obj.clone();
            assertNotNull(cloned);
        }
        {
            DateIntervalFormat obj = DateIntervalFormat.getInstance(DateFormat.YEAR_MONTH_DAY);
            DateIntervalFormat cloned = (DateIntervalFormat) obj.clone();
            assertNotNull(cloned);
        }
        {
            android.icu.text.DateIntervalInfo obj = new DateIntervalInfo(ULocale.getDefault());
            android.icu.text.DateIntervalInfo cloned = (DateIntervalInfo) obj.clone();
            assertNotNull(cloned);
        }
        {
            DateTimePatternGenerator obj = DateTimePatternGenerator.getInstance();
            DateTimePatternGenerator cloned = (DateTimePatternGenerator) obj.clone();
            assertNotNull(cloned);
        }
        {
            DecimalFormat obj = (DecimalFormat) DecimalFormat.getInstance();
            DecimalFormat cloned = (DecimalFormat) obj.clone();
            assertNotNull(cloned);
        }
        {
            DecimalFormatSymbols obj = DecimalFormatSymbols.getInstance();
            DecimalFormatSymbols cloned = (DecimalFormatSymbols) obj.clone();
            assertNotNull(cloned);
        }
        {
            MessageFormat obj = new MessageFormat("message format");
            MessageFormat cloned = (MessageFormat) obj.clone();
            assertNotNull(cloned);
        }
        {
            android.icu.text.MessagePattern obj = new MessagePattern();
            android.icu.text.MessagePattern cloned = (MessagePattern) obj.clone();
            assertNotNull(cloned);
        }
        {
            NumberFormat obj = NumberFormat.getInstance();
            NumberFormat cloned = (NumberFormat) obj.clone();
            assertNotNull(cloned);
        }
        {
            SimpleDateFormat obj = (SimpleDateFormat) DateFormat.getInstance();
            SimpleDateFormat cloned = (SimpleDateFormat) obj.clone();
            assertNotNull(cloned);
        }
        {
            TimeZoneFormat obj = TimeZoneFormat.getInstance(ULocale.getDefault());
            TimeZoneFormat cloned = (TimeZoneFormat) obj.clone();
            assertNotNull(cloned);
        }
        {
            UCharacterIterator obj = UCharacterIterator.getInstance("abc".toCharArray());
            UCharacterIterator cloned = (UCharacterIterator) obj.clone();
            assertNotNull(cloned);
        }
        {
            UnicodeSet obj = UnicodeSet.EMPTY;
            UnicodeSet cloned = (UnicodeSet) obj.clone();
            assertNotNull(cloned);
        }
        {
            Calendar obj = Calendar.getInstance();
            Calendar cloned = (Calendar) obj.clone();
            assertNotNull(cloned);
        }
        {
            GregorianCalendar obj = new GregorianCalendar();
            GregorianCalendar cloned = (GregorianCalendar) obj.clone();
            assertNotNull(cloned);
        }
        {
            TimeZone obj = TimeZone.getDefault();
            TimeZone cloned = (TimeZone) obj.clone();
            assertNotNull(cloned);
        }
        {
            ULocale obj = ULocale.US;
            ULocale cloned = (ULocale) obj.clone();
            assertNotNull(cloned);
        }
    }
}
