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
package com.android.icu.charset;

/**
 * On Android, CharsetDecoderICU and CharsetEncoderICU skips the replaceWith call in its
 * constructor. We use the workarounds here to achieve similar effect.
 */
public class CharsetICU_ravenwood {

    static void implReplaceWith(CharsetDecoderICU decoder, String newReplacement) {
        if (isCalledFromConstructor(decoder)) {
            return;
        }
        decoder.updateCallback();
    }

    static void implReplaceWith(CharsetEncoderICU encoder, byte[] newReplacement) {
        if (isCalledFromConstructor(encoder)) {
            return;
        }
        encoder.updateCallback();
    }

    private static boolean isCalledFromConstructor(Object obj) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().equals(obj.getClass().getName()) &&
                    element.getMethodName().equals("<init>")) {
                return true;
            }
        }
        return false;
    }
}
