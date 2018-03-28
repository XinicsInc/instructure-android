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

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import com.instructure.candroid.R
import com.instructure.pandautils.utils.ThemePrefs

class FatalErrorDialogStyled : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val activity = activity
        val args = arguments

        val builder = AlertDialog.Builder(activity)
                .setTitle(args.getInt(TITLE))
                .setPositiveButton(activity.getString(R.string.okay)) { dialog, _ ->
                    val shouldDismiss = args.getBoolean(SHOULD_DISMISS, false)
                    if (shouldDismiss) {
                        dialog.dismiss()
                    } else {
                        activity.finish()
                    }
                }
                .setMessage(args.getInt(MESSAGE))

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.brandColor);
        }

        return dialog
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance)
            dialog.setDismissMessage(null)
        super.onDestroyView()
    }

    companion object {
        const val TAG = "fatalErrorDialog"

        private const val TITLE = "title"
        private const val MESSAGE = "message"
        private const val SHOULD_DISMISS = "shouldDismiss"

        fun newInstance(title: Int, message: Int): FatalErrorDialogStyled {
            val frag = FatalErrorDialogStyled()
            val args = Bundle()
            args.putInt(TITLE, title)
            args.putInt(MESSAGE, message)
            args.putBoolean(SHOULD_DISMISS, false)
            frag.arguments = args
            return frag
        }

        /* @param shouldDismiss: if true dismiss the dialog, otherwise finish the activity */
        fun newInstance(title: Int, message: Int, shouldDismiss: Boolean): FatalErrorDialogStyled {
            val frag = newInstance(title, message)
            val args = frag.arguments
            args.putBoolean(SHOULD_DISMISS, shouldDismiss)
            frag.arguments = args
            return frag
        }
    }
}