/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
 */
package com.instructure.pandautils.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.instructure.pandautils.R
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.BlindSerializableArg
import com.instructure.pandautils.utils.dismissExisting

class UnsavedChangesContinueDialog : AppCompatDialogFragment() {

    private var mListener: (() -> Unit)? by BlindSerializableArg()

    init { retainInstance = true }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.utils_continueWithoutSavingTitle)
                .setMessage(R.string.utils_continueWithoutSavingMessage)
                .setPositiveButton(R.string.utils_continueUnsaved, null)
                .setNegativeButton(R.string.utils_cancel) { _, _ -> mListener?.invoke() }
                .create()
        return dialog.apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
                getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
            }
        }
    }

    companion object {
        fun show(manager: FragmentManager, listener: () -> Unit) = UnsavedChangesContinueDialog().apply {
            manager.dismissExisting<UnsavedChangesContinueDialog>()
            mListener = listener
            show(manager, javaClass.simpleName)
        }
    }

    override fun onDestroyView() {
        mListener = null
        // Fix for rotation bug
        dialog?.let { if (retainInstance) it.setDismissMessage(null) }
        super.onDestroyView()
    }
}