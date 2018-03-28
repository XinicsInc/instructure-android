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

package com.instructure.pandautils.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils

object PermissionUtils {

    const val PERMISSION_REQUEST_CODE = 78
    const val WRITE_FILE_PERMISSION_REQUEST_CODE = 98
    const val READ_FILE_PERMISSION_REQUEST_CODE = 108

    const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    const val WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
    const val READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE

    const val CAMERA = Manifest.permission.CAMERA
    const val RECORD_AUDIO = Manifest.permission.RECORD_AUDIO

    /**
     * Extension method on [Context] to check for permissions
     */
    @JvmStatic
    fun Context.hasPermissions(vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return permissions
                    .map { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
                    .all { it }
        }
        return true
    }

    /**
     * Checks to see if we have the necessary permissions.
     * @param activity A context in the form of an activity
     * @param permissions A string of permissions (we have hard coded values in [PermissionUtils])
     * @return a boolean telling if the user has the necessary permissions
     */
    @JvmStatic
    fun hasPermissions(activity: Activity, vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return permissions
                    .map { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
                    .all { it }
        }

        return true
    }

    // Helper to make an array
    @JvmStatic
    fun makeArray(vararg items: String): Array<String> {
        return items as Array<String>
    }

    /**
     * Whether or not all of the permissions were granted.
     * @param grantResults the array returned from [Activity.onRequestPermissionsResult]
     * @return the result telling if all permissions were granted
     */
    @JvmStatic
    fun allPermissionsGrantedResultSummary(grantResults: IntArray): Boolean = grantResults.indices.none { grantResults[it] == PackageManager.PERMISSION_DENIED }

    @JvmStatic
    fun permissionGranted(permissions: Array<String>, grantResults: IntArray, permission: String): Boolean {
        var permissionsGranted = false

        if (permissions.size == grantResults.size && !TextUtils.isEmpty(permission)) {
            permissions.indices
                    .filter { permissions[it] == permission && grantResults[it] == PackageManager.PERMISSION_GRANTED }
                    .forEach { permissionsGranted = true }
        }

        return permissionsGranted
    }
}
