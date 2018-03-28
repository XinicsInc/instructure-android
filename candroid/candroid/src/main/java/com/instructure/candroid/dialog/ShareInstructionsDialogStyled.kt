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

package com.instructure.candroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.instructure.candroid.R
import com.instructure.pandautils.utils.ThemePrefs

class ShareInstructionsDialogStyled : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity

        @SuppressLint("InflateParams") // Suppress lint warning about null parent when inflating layout
        val view = LayoutInflater.from(activity).inflate(R.layout.share_dialog_instructions, null)

        val builder = AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.fromOtherApplication))
                .setPositiveButton(activity.getString(R.string.okay)) { _, _ -> dismissAllowingStateLoss() }

        builder.setView(view)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.brandColor)
        }

        return dialog
    }

    override fun onDestroyView() {
        if (retainInstance)
            dialog?.setDismissMessage(null)

        super.onDestroyView()
    }

    companion object {
        const val TAG = "shareInstructorDialog"

        fun show(activity: FragmentActivity): ShareInstructionsDialogStyled =
                ShareInstructionsDialogStyled().apply {
                    show(activity.supportFragmentManager, TAG)
                }
    }
}

