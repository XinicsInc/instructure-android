/*
 * Copyright (C) 2017 - present Instructure, Inc.
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
package com.instructure.teacher.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.teacher.R
import com.instructure.teacher.dialog.NoInternetConnectionDialog

/** Show a toast with a default length of Toast.LENGTH_SHORT */
fun Context.toast(@StringRes messageResId: Int, length: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, messageResId, length).show()

fun Context.getColorCompat(@ColorRes colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun FragmentActivity.withRequireNetwork(block: () -> Unit) {
    if (APIHelper.hasNetworkConnection()) block() else NoInternetConnectionDialog.show(supportFragmentManager)
}

/** The status bar color */
var Activity.statusBarColor: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) window.statusBarColor else 0
    set(value) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) window.statusBarColor = value }

val Activity.isTablet: Boolean
    get() = resources.getBoolean(R.bool.is_device_tablet)

