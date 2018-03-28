package com.instructure.candroid.dialog

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
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.widget.AppCompatEditText
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import com.instructure.candroid.R
import com.instructure.canvasapi2.models.Course
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.dismissExisting
import kotlin.properties.Delegates

class EditCourseNicknameDialog : AppCompatDialogFragment() {

    private var mEditNicknameCallback: (String) -> Unit by Delegates.notNull()

    init {
        retainInstance = true
    }

    companion object {
        @JvmStatic
        fun getInstance(manager: FragmentManager, course: Course, callback: (String) -> Unit) : EditCourseNicknameDialog {
            manager.dismissExisting<EditCourseNicknameDialog>()
            val dialog = EditCourseNicknameDialog()
            val args = Bundle()
            args.putParcelable(Const.COURSE, course)
            dialog.arguments = args
            dialog.mEditNicknameCallback = callback
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val course : Course = arguments.get(Const.COURSE) as Course
        val view = View.inflate(activity, R.layout.dialog_course_nickname, null)
        val editCourseNicknameEditText = view.findViewById<AppCompatEditText>(R.id.newCourseNickname)
        editCourseNicknameEditText.setText(course.name)
        ViewStyler.themeEditText(context, editCourseNicknameEditText, ThemePrefs.brandColor)
        editCourseNicknameEditText.inputType = EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS
        editCourseNicknameEditText.selectAll()

        val nameDialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle(activity.getString(R.string.edit_course_nickname))
                .setView(view)
                .setPositiveButton(activity.getString(android.R.string.ok).toUpperCase(), { _, _ ->
                    mEditNicknameCallback(editCourseNicknameEditText.text.toString())
                })
                .setNegativeButton(activity.getString(android.R.string.cancel).toUpperCase(), null)
                .create()
        nameDialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        nameDialog.setOnShowListener {
            nameDialog.getButton(AppCompatDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
            nameDialog.getButton(AppCompatDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
        }
        return nameDialog
    }

    override fun onDestroyView() {
        // Fix for rotation bug
        dialog?.let { if (retainInstance) it.setDismissMessage(null) }
        super.onDestroyView()
    }
}
