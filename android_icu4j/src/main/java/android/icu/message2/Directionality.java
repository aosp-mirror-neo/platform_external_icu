/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package android.icu.message2;

import android.icu.util.ULocale;

/**
 * Encodes info about the direction of the message.
 *
 * <p>It is used to implement the @code u:dir} functionality.</p>
 *
 * @deprecated This API is for technology preview only.
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
@Deprecated
public enum Directionality {
    /**
     * Not initialized or unknown.
     *
     * <p>No special processing will be used.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    UNKNOWN,
    /**
     * Left-to-right directionality.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    LTR,
    /**
     * Right-to-left directionality.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    RTL,
    /**
     * Directionality determined from <i>expression</i> contents.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    AUTO,
    /**
     * Directionality inherited from the <i>message</i> or from the <i>resolved value</i>
     * of the <i>operand</i> without requiring isolation of the <i>expression</i> value.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    INHERIT;

    /**
     * Determines the directionality appropriate for a given locale.
     *
     * @param ulocale the locale to determine the directionality from.
     * @return the appropriate directionality for the locale given.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static Directionality of(ULocale ulocale) {
        if (ulocale == null ) {
            return Directionality.INHERIT;
        }
        return ulocale.isRightToLeft() ? Directionality.RTL : Directionality.LTR;
    }
}
