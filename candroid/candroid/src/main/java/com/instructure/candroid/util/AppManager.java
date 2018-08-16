/*
 * Copyright (C) 2016 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.candroid.util;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.webkit.WebView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.instructure.candroid.BuildConfig;
import com.instructure.candroid.R;
import com.instructure.candroid.tasks.LogoutAsyncTask;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.UserManager;
import com.instructure.canvasapi2.models.CanvasErrorCode;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.Logger;
import com.instructure.pandautils.utils.ColorKeeper;
import com.pspdfkit.PSPDFKit;
import com.pspdfkit.exceptions.InvalidPSPDFKitLicenseException;
import com.pspdfkit.exceptions.PSPDFKitInitializationFailedException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;
import retrofit2.Call;
import retrofit2.Response;

public class AppManager extends com.instructure.canvasapi2.AppManager implements AnalyticsEventHandling {

    public final static String PREF_NAME = "candroidSP";

    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        initPSPDFKit();

        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        Fabric.with(this, crashlyticsKit);

        ColorKeeper.setDefaultColor(ContextCompat.getColor(this, R.color.defaultPrimary));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // there appears to be a bug when the user is installing/updating the android webview stuff.
            // http://code.google.com/p/android/issues/detail?id=175124
            try {
                WebView.setWebContentsDebuggingEnabled(true);
            } catch (Exception e) {
                Crashlytics.log("Exception trying to setWebContentsDebuggingEnabled");
            }
        }

        loadLanguage(getApplicationContext());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadLanguage(getApplicationContext());
    }

    @Deprecated
    public String getGlobalUserId(String domain, User user) {
        return domain + "-" + user.getId();
    }

    /**
     * @param context used to check the device version and DownloadManager information
     * @return true if the download manager is available
     */
    public static boolean isDownloadManagerAvailable(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    //region Analytics Event Handling

    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.analytics);
        }
        return mTracker;
    }

    @Override
    public void trackButtonPressed(String buttonName, Long buttonValue) {
        if(buttonName == null) return;

        if(buttonValue == null) {
            getDefaultTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("UI Actions")
                    .setAction("Button Pressed")
                    .setLabel(buttonName)
                    .build());
        } else {
            getDefaultTracker().send(new HitBuilders.EventBuilder()
                    .setCategory("UI Actions")
                    .setAction("Button Pressed")
                    .setLabel(buttonName)
                    .setValue(buttonValue)
                    .build());
        }
    }

    @Override
    public void trackScreen(String screenName) {
        if(screenName == null) return;

        Tracker tracker = getDefaultTracker();
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void trackEnrollment(String enrollmentType) {
        if(enrollmentType == null) return;

        getDefaultTracker().send(new HitBuilders.AppViewBuilder()
                .setCustomDimension(1, enrollmentType)
                .build());
    }

    @Override
    public void trackDomain(String domain) {
        if(domain == null) return;

        getDefaultTracker().send(new HitBuilders.AppViewBuilder()
                .setCustomDimension(2, domain)
                .build());
    }

    @Override
    public void trackEvent(String category, String action, String label, long value) {
        if(category == null || action == null || label == null) return;

        Tracker tracker = getDefaultTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
    }

    @Override
    public void trackUIEvent(String action, String label, long value) {
        if(action == null || label == null) return;

        getDefaultTracker().send(new HitBuilders.EventBuilder()
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
    }

    @Override
    public void trackTiming(String category, String name, String label, long duration) {
        if(category == null || name == null || label == null) return;

        Tracker tracker = getDefaultTracker();
        tracker.send(new HitBuilders.TimingBuilder()
                .setCategory(category)
                .setLabel(label)
                .setVariable(name)
                .setValue(duration)
                .build());
    }

    //endregion

    private void initPSPDFKit() {
        try {
            PSPDFKit.initialize(this, BuildConfig.PSPDFKIT_LICENSE_KEY);
        } catch (PSPDFKitInitializationFailedException e) {
            Logger.e("Current device is not compatible with PSPDFKIT!");
        } catch (InvalidPSPDFKitLicenseException e) {
            Logger.e("Invalid or Trial PSPDFKIT License!");
        }
    }

    /**
     * Pass the current context to load the stored language
     * @param context
     */
    private void loadLanguage(@NonNull Context context) {
        int language = StudentPrefs.getLanguageIndex();
        if (language >= context.getResources().getStringArray(R.array.supported_languages).length) {
            language = 0;
            StudentPrefs.setLanguageIndex(0);
        }

        Locale locale;
        String languageToLoad = generateLanguageString(language);
        if (languageToLoad.equals("zh")) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else if (languageToLoad.equals("zh_HK")) {
            locale = Locale.TRADITIONAL_CHINESE;
        } else if (languageToLoad.equals("pt_BR")) {
            locale = new Locale("pt", "BR");
        } else if (languageToLoad.equals("fr_CA")) {
            locale = new Locale("fr", "CA");
        } else if (languageToLoad.equals("root")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(languageToLoad);
        }

        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    /**
     * The position provided needs to correspond to the position in the supported languages
     * array found in the values folder.
     * @param position
     * @return
     */
    private static String generateLanguageString(int position) {
        switch (position) {
            case 0:
                return "root";
            case 1:
                return "ar";
            case 2:
                return "zh_HK";
            case 3:
                return "zh";
            case 4:
                return "da";
            case 5:
                return "nl";
            case 6:
                return "en_AU";
            case 7:
                return "en_GB";
            case 8:
                return "en_US";
            case 9:
                return "fr";
            case 10:
                return "fr_CA";
            case 11:
                return "de";
            case 12:
                return "ja";
            case 13:
                return "mi";
            case 14:
                return "nb";
            case 15:
                return "pl";
            case 16:
                return "pt";
            case 17:
                return "pt_BR";
            case 18:
                return "ru";
            case 19:
                return "es";
            case 20:
                return "sv";
             //aka system default
            default:
                return "root";
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    private void onUserNotAuthorized(CanvasErrorCode event) {
        if(event.getCode() == 401) {
            UserManager.getSelf(true, new StatusCallback<User>(){
                @Override
                public void onFail(@Nullable Call<User> call, @NonNull Throwable error, @Nullable Response response) {
                    new LogoutAsyncTask().execute();
                }
            });
        }
    }

}
