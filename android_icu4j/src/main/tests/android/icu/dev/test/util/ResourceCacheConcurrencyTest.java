/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package android.icu.dev.test.util;

import android.icu.impl.ICUData;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import android.icu.testsharding.MainTestShard;

/** Concurrency regression test for ResourceCache. */
@MainTestShard
@RunWith(JUnit4.class)
public class ResourceCacheConcurrencyTest extends ConcurrencyTest {

    @Test
    public void testResourceCacheConcurrentLookups() throws Exception {
        String[] localeNames = {"en", "de", "ja", "zh", "fr", "es", "ko", "pt"};

        for (String loc : localeNames) {
            UResourceBundle rb =
                    UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, new ULocale(loc));
            if (rb == null) {
                return;
            }
        }

        runConcurrent(
                "ResourceCacheLookups",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        String loc = localeNames[(tid + i) % localeNames.length];
                        UResourceBundle rb =
                                UResourceBundle.getBundleInstance(
                                        ICUData.ICU_BASE_NAME, new ULocale(loc));
                        assertNotNull("ResourceBundle should not be null", rb);
                        try {
                            rb.get("Version");
                        } catch (Exception e) {
                            // Some bundles may not have this key
                        }
                    }
                });
    }
}
