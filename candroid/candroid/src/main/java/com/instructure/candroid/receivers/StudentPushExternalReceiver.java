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

package com.instructure.candroid.receivers;

import android.app.Activity;
import android.content.Context;

import com.instructure.candroid.R;
import com.instructure.candroid.activity.NavigationActivity;
import com.instructure.pandautils.receivers.PushExternalReceiver;

import org.jetbrains.annotations.NotNull;

public class StudentPushExternalReceiver extends PushExternalReceiver {

    @NotNull
    @Override
    public String getAppName(Context context) {
        return context.getString(R.string.app_name);
    }

    @NotNull
    @Override
    public Class<? extends Activity> getStartingActivityClass() {
        return NavigationActivity.Companion.getStartActivityClass();
    }
}
