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
package com.instructure.teacher.tasks;

import android.content.Intent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.instructure.canvasapi2.CanvasRestAdapter;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.managers.OAuthManager;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ContextKeeper;
import com.instructure.canvasapi2.utils.FileUtils;
import com.instructure.canvasapi2.utils.MasqueradeHelper;
import com.instructure.loginapi.login.tasks.LogoutTask;
import com.instructure.pandautils.utils.FilePrefs;
import com.instructure.pandautils.utils.ThemePrefs;
import com.instructure.pandautils.utils.Utils;
import com.instructure.teacher.R;
import com.instructure.teacher.activities.InitLoginActivity;
import com.instructure.teacher.utils.TeacherPrefs;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;

public class LogoutAsyncTask extends LogoutTask {

    @SuppressWarnings("deprecation")
    @Override
    protected void clearCookies() {
        CookieSyncManager.createInstance(ContextKeeper.appContext);
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

        File exCacheDir = Utils.getAttachmentsDirectory(ContextKeeper.getAppContext());
        FileUtils.deleteAllFilesInDirectory(exCacheDir);
        RestBuilder.clearCacheDirectory();
        ApiPrefs.clearAllData();
        TeacherPrefs.INSTANCE.clearPrefs();
        FilePrefs.INSTANCE.clearPrefs();
    }

    @Override
    protected void cleanupMasquerading() {
        MasqueradeHelper.stopMasquerading();
    }

    @Override
    protected boolean logout() {
        if(APIHelper.hasNetworkConnection()) {
            if (!ApiPrefs.getToken().isEmpty()) {
                //Delete token from server. Fire and forget.
                OAuthManager.deleteToken();
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void refreshWidgets() {
        //Do nothing. No widgets in Canvas Teacher.
    }

    @Override
    protected void clearTheme() {
        ThemePrefs.INSTANCE.clearPrefs();
    }

    @Override
    protected void onLogoutFailed() {
        Toast.makeText(ContextKeeper.appContext, R.string.no_data_connection, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void startLoginFlow() {
        Intent intent = InitLoginActivity.Companion.createIntent(ContextKeeper.appContext);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ContextKeeper.appContext.startActivity(intent);
    }
}
