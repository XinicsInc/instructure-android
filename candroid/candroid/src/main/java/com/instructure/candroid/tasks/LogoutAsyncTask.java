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
package com.instructure.candroid.tasks;

import android.content.Intent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.activity.LoginActivity;
import com.instructure.candroid.util.StudentPrefs;
import com.instructure.candroid.view.CanvasRecipientManager;
import com.instructure.candroid.widget.WidgetUpdater;
import com.instructure.canvasapi2.CanvasRestAdapter;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.managers.OAuthManager;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ContextKeeper;
import com.instructure.canvasapi2.utils.FileUtils;
import com.instructure.loginapi.login.tasks.LogoutTask;
import com.instructure.pandautils.utils.ThemePrefs;
import com.instructure.pandautils.utils.Utils;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;


public class LogoutAsyncTask extends LogoutTask {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        CanvasRecipientManager.getInstance(ContextKeeper.getAppContext()).clearCache();
    }

    @Override
    protected void onLogoutFailed() {
        Toast.makeText(ContextKeeper.getAppContext(), R.string.no_data_connection, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void clearCookies() {
        CookieSyncManager.createInstance(ContextKeeper.getAppContext());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    @Override
    protected void clearCache() {
        OkHttpClient client = CanvasRestAdapter.getClient();
        if(client != null) {
            try {
                client.cache().evictAll();
            } catch (IOException e) {/* Do Nothing */}
        }

        RestBuilder.clearCacheDirectory();

        // Clear shared preferences
        StudentPrefs.INSTANCE.safeClearPrefs();
        ApiPrefs.clearAllData();

        File cacheDir = new File(ContextKeeper.getAppContext().getFilesDir(), "cache");
        File exCacheDir = Utils.getAttachmentsDirectory(ContextKeeper.getAppContext());
        FileUtils.deleteAllFilesInDirectory(cacheDir);
        FileUtils.deleteAllFilesInDirectory(exCacheDir);
    }

    @Override
    protected void cleanupMasquerading() {
        //remove the cached stuff for masqueraded user
        File masqueradeCacheDir = new File(ContextKeeper.getAppContext().getFilesDir(), "cache_masquerade");
        //need to delete the contents of the internal cache folder so previous user's results don't show up on incorrect user
        FileUtils.deleteAllFilesInDirectory(masqueradeCacheDir);
    }

    @Override
    protected boolean logout() {
        //It is possible for multiple APIs to come back 'simultaneously' as HTTP401s causing a logout
        //if this has already ran, data is already cleared causing null pointer exceptions
        if(APIHelper.hasNetworkConnection()) {
            String token = ApiPrefs.getToken();
            if (!token.equals("")) {
                OAuthManager.deleteToken();
                return true;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void refreshWidgets() {
        WidgetUpdater.updateWidgets();
    }

    @Override
    protected void clearTheme() {
        ThemePrefs.INSTANCE.clearPrefs();
    }

    @Override
    protected void startLoginFlow() {
        Intent intent = LoginActivity.Companion.createIntent(ContextKeeper.appContext);
        ContextKeeper.appContext.startActivity(intent);
    }
}
