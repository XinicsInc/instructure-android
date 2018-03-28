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

package com.instructure.teacher.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.widget.AppCompatEditText
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import com.instructure.canvasapi2.models.Course
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.teacher.R
import com.instructure.pandautils.utils.dismissExisting
import com.instructure.canvasapi2.utils.globalName
import kotlin.properties.Delegates

class EditCourseNameDialog : AppCompatDialogFragment() {

    init {
        retainInstance = true
    }

    private var mEditNameCallback: (String) -> Unit by Delegates.notNull()

    @Suppress("unused")
    private fun EditCourseNameDialog() { }

    companion object {
        @JvmStatic
        fun getInstance(manager: FragmentManager, course: Course, callback: (String) -> Unit) : EditCourseNameDialog {
            manager.dismissExisting<EditCourseNameDialog>()
            val dialog = EditCourseNameDialog()
            val args = Bundle()
            args.putParcelable(Const.COURSE, course)
            dialog.arguments = args
            dialog.mEditNameCallback = callback
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val course : Course = arguments.get(Const.COURSE) as Course
        val view = View.inflate(activity, R.layout.dialog_rename_course, null)
        val editCourseNameEditText = view.findViewById<AppCompatEditText>(R.id.newCourseName)
        editCourseNameEditText.setText(course.globalName)
        ViewStyler.themeEditText(context, editCourseNameEditText, ThemePrefs.brandColor)
        editCourseNameEditText.inputType = EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS
        editCourseNameEditText.selectAll()

        val nameDialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle(activity.getString(R.string.course_name))
                .setView(view)
                .setPositiveButton(activity.getString(android.R.string.ok), { _, _ ->
                    mEditNameCallback(editCourseNameEditText.text.toString())
                })
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .create()

        //Adjust the dialog to the top so keyboard does not cover it up, issue happens on tablets in landscape
        val params = nameDialog.window.attributes
        params.gravity = Gravity.CENTER or Gravity.TOP
        params.y = 120
        nameDialog.window.attributes = params
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
