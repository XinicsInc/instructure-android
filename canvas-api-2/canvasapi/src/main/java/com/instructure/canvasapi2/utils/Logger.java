/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.canvasapi2.utils;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class Logger {

    private static final String LOG_TAG = "canvasLog";

    public static void d(@Nullable String s) {
        if(s == null){ s = "Value was null."; }
        Log.d(LOG_TAG, s);
    }

    public static void i(@Nullable String s) {
        if(s == null){ s = "Value was null."; }
        Log.i(LOG_TAG, s);
    }

    public static void e(@Nullable String s) {
        if(s == null){ s = "Value was null."; }
        Log.e(LOG_TAG, s);
    }

    public static void v(@Nullable String s) {
        if(s == null){ s = "Value was null."; }
        Log.v(LOG_TAG, s);
    }

    public static void w(@Nullable String s) {
        if (s == null) s = "Value was null.";
        Log.w(LOG_TAG, s);
    }

    public static void date(@Nullable String s, GregorianCalendar date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss zzz", Locale.US);
        Log.d(LOG_TAG, s + ": " + sdf.format(new Date(date.getTimeInMillis())));
    }

    public static void logBundle(@Nullable final Bundle extras) {
        if(extras != null) {
            d("---====---LOGGING BUNDLE---====---");
            if(extras.size() == 0) {
                d("- Bundle was empty.");
            }
            for (String key: extras.keySet()){
                d("- Bundle: " + key);

                if("bundledExtras".equals(key)) {
                    Bundle innerExtras = extras.getBundle("bundledExtras");
                    if(innerExtras != null) {
                        for (String innerKey: innerExtras.keySet()) {
                            d("   -> Inner Bundle: " + innerKey);
                        }
                    }
                }
            }
        } else {
            d("Bundle was null.");
        }
    }

    public static <F extends android.support.v4.app.Fragment> String getFragmentName(F fragment) {
        if(fragment != null) {
            return fragment.getClass().getName();
        }
        return "UNKNOWN";
    }

    /**
     * List of ISO 3166-1 alpha-2 codes of countries whose laws restrict us from logging user details
     */
    private static String[] LOGGING_DISALLOWED_COUNTRY_CODES = new String[]{"CA"};

    /**
     * Whether user detail logging is allowed for the current country. This checks the network country,
     * the SIM country, and the current locale country. If <i>any</i> of these match a disallowed
     * country then this method will return false.
     * @return True if user detail logging is allowed, false otherwise.
     */
    public static boolean canLogUserDetails() {
        TelephonyManager telephonyManager = (TelephonyManager) ContextKeeper.getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
        String networkCountryCode = telephonyManager.getNetworkCountryIso().toUpperCase(Locale.US);
        String simCountryCode = telephonyManager.getSimCountryIso().toUpperCase(Locale.US);
        String localeCountryCode = Locale.getDefault().getCountry().toUpperCase(Locale.US);

        for (String disallowedCountryCode : LOGGING_DISALLOWED_COUNTRY_CODES) {
            if (disallowedCountryCode.equals(networkCountryCode)
                    || disallowedCountryCode.equals(simCountryCode)
                    || disallowedCountryCode.equals(localeCountryCode)) {
                return false;
            }
        }

        return true;
    }
}
