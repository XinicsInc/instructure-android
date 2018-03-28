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

import android.support.v4.app.Fragment
import android.widget.Toast
import com.instructure.teacher.R

/**
 * Show the provided message in a Toast, prefixed with 'Coming Soon:'.
 * This is for development only and input strings do not need i18n.
 */
fun Fragment.soonToast(message: String) {
    Toast.makeText(context, "Coming soon: $message", Toast.LENGTH_SHORT).show()
}

fun Fragment.withRequireNetwork(block: () -> Unit) = activity.withRequireNetwork(block)

/** The status bar color */
var Fragment.statusBarColor: Int
    get() = activity.statusBarColor
    set(value) { activity.statusBarColor = value }

