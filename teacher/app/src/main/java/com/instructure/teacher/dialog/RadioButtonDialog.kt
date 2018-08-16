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
import android.support.v4.content.ContextCompat
import android.support.v4.widget.CompoundButtonCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.widget.AppCompatRadioButton
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R

typealias OnRadioButtonSelected = ((selectedIdx: Int) -> Unit)?

class RadioButtonDialog : AppCompatDialogFragment() {

    init {
        retainInstance = true
    }

    private var mSelectedIdx by IntArg(-1)
    private var mOptions by SerializableListArg<String>(emptyList())
    private var mCallback by BlindSerializableArg<OnRadioButtonSelected>()
    private var mTitle by StringArg()

    private var currentSelectionIdx: Int = -1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        mSelectedIdx = arguments.getInt(Const.SELECTED_ITEM)
        mOptions = arguments.getStringArrayList(Const.OPTIONS)
        mTitle = arguments.getString(Const.TITLE)

        if (currentSelectionIdx == -1) {
            currentSelectionIdx = mSelectedIdx
        }

        val view = View.inflate(ContextThemeWrapper(activity, 0), R.layout.dialog_radio_button, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentSelectionIdx = checkedId - 1

            // Dynamically created RadioButtons keep losing tint after selection - here's a workaround
            // NOTE: We are not using the ViewStyler.themeRadioButton for these due to issues with the buttons not unchecking,
            //       probably an issue with how they are setup in the RadioGroup
            val unselectedColor = ContextCompat.getColor(activity, R.color.unselected_radio_color)
            (radioGroup.getChildAt(currentSelectionIdx) as AppCompatRadioButton).let {
                val colorStateList = ViewStyler.makeColorStateListForRadioGroup(ThemePrefs.brandColor, ThemePrefs.brandColor)
                CompoundButtonCompat.setButtonTintList(it, colorStateList)
            }

            radioGroup.children<AppCompatRadioButton>().filter { !it.isChecked }
                    .forEach {
                        val colorStateList = ViewStyler.makeColorStateListForRadioGroup(unselectedColor, unselectedColor)
                        CompoundButtonCompat.setButtonTintList(it, colorStateList)
                    }
        }

        for ((index, option) in mOptions.withIndex()) {
            val radioButton = AppCompatRadioButton(activity)
            radioButton.text = option
            radioButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.dialogRadioButtonTextSize))
            radioButton.id = index + 1

            radioGroup.addView(radioButton)

            // The way this view has to be inflated and added means that layout measurements are skipped initially,
            // add a height to the radio button
            val params = radioButton.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            val typedValues = TypedValue()
            activity.theme.resolveAttribute(android.R.attr.listPreferredItemHeightSmall, typedValues, true)
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            params.height = typedValues.getDimension(metrics).toInt()
            radioButton.layoutParams = params
        }

        // Setting it afterwards so the onCheckChangedListener get triggered after all the radio buttons are added
        radioGroup.check(currentSelectionIdx + 1)

        val dialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle(mTitle)
                .setView(view)
                .setPositiveButton(activity.getString(android.R.string.ok).toUpperCase(), { _, _ ->
                    if (currentSelectionIdx != mSelectedIdx) mCallback?.invoke(currentSelectionIdx)
                })
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AppCompatDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
            dialog.getButton(AppCompatDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
        }

        return dialog
    }

    override fun onDestroyView() {
        // Fix for rotation bug
        dialog?.let { if (retainInstance) it.setDismissMessage(null) }
        super.onDestroyView()
    }

    companion object {

        @JvmStatic
        fun getInstance(manager: FragmentManager, title: String, options: ArrayList<String>,
                        selectedIdx: Int,
                        callback: OnRadioButtonSelected): RadioButtonDialog {
            manager.dismissExisting<RadioButtonDialog>()
            val dialog = RadioButtonDialog()
            val args = Bundle()
            args.putString(Const.TITLE, title)
            args.putStringArrayList(Const.OPTIONS, options)
            args.putInt(Const.SELECTED_ITEM, selectedIdx)
            dialog.arguments = args
            dialog.mCallback = callback
            return dialog
        }
    }

}