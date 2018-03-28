/*
 * Copyright (C) 2016 - present  Instructure, Inc.
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

package com.instructure.parentapp.dialogs

import android.app.Dialog
import android.app.Service
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.instructure.pandautils.utils.onClick
import com.instructure.parentapp.R
import kotlinx.android.synthetic.main.dialog_student_threshold.view.*
import java.util.*

class StudentThresholdDialog : DialogFragment() {

    private lateinit var mCallback: StudentThresholdChanged

    interface StudentThresholdChanged {
        fun handlePositiveThreshold(thresholdType: Int, value: String)
        fun handleNeutralThreshold(thresholdType: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is StudentThresholdChanged) {
            mCallback = context
        } else {
            throw IllegalStateException("Caller must implement StudentThresholdChange callback.")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = arguments.getString(TITLE, "")
        val threshold = arguments.getString(THRESHOLD, "")

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_student_threshold, null)

        val createdDialog = AlertDialog.Builder(activity).setView(dialogView).create()

        dialogView.title.text = title

        val et = dialogView.threshold

        val size = dialogView.inputSize
        size.text = String.format(Locale.getDefault(), "%d/%d", 0, 3)

        val saveButton = dialogView.save.apply {
            onClick {
                // Input will return here, when we eventually add input checking
                // we can filter input by calling alwaysCallInputCallback() and check the
                // input here
                val thresholdType = arguments.getInt(THRESHOLD_TYPE)
                mCallback.handlePositiveThreshold(thresholdType, et.text.toString())
                createdDialog.dismiss()
            }
        }

        dialogView.never.onClick {
            val thresholdType = arguments.getInt(THRESHOLD_TYPE)
            mCallback.handleNeutralThreshold(thresholdType)
            createdDialog.dismiss()
        }

        dialogView.cancel.onClick { dialog?.dismiss() }

        createdDialog.setOnShowListener {
            val imm = activity.getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(et, 0)

            et.addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                            val length = s.length

                            size.text = String.format(Locale.getDefault(), "%d/%d", length, 3)

                            val isDisabled = length == 0 || length > 3
                            val textColor = if (isDisabled) Color.RED else resolveColor(activity, android.R.attr.textColorSecondary)
                            val widgetColor = if (isDisabled) Color.RED else resolveColor(activity, R.attr.colorAccent)

                            size.setTextColor(textColor)
                            et.setTextColor(textColor)

                            val thresholdEditTextColor = createColorStateList(activity, widgetColor)
                            et.backgroundTintList = thresholdEditTextColor
                            setCursorTint(et, widgetColor)

                            saveButton.isEnabled = !isDisabled
                            saveButton.alpha = if (isDisabled) 0.4f else 1.0f
                        }

                        override fun afterTextChanged(s: Editable) {}
                    })

            et.setText(if (threshold == resources.getString(R.string.never)) "" else threshold)
            et.setSelection(et.text.toString().length)
        }

        return createdDialog
    }

    // Taken from https://github.com/afollestad/material-dialogs/blob/master/core/src/main/java/com/afollestad/materialdialogs/internal/MDTintHelper.java
    private fun createColorStateList(context: Context, @ColorInt color: Int): ColorStateList {
        val states = arrayOfNulls<IntArray>(3)
        val colors = IntArray(3)
        var i = 0
        states[i] = intArrayOf(-android.R.attr.state_enabled)
        colors[i] = resolveColor(context, R.attr.colorControlNormal)
        i++
        states[i] = intArrayOf(-android.R.attr.state_pressed, -android.R.attr.state_focused)
        colors[i] = resolveColor(context, R.attr.colorControlNormal)
        i++
        states[i] = intArrayOf()
        colors[i] = color
        return ColorStateList(states, colors)
    }

    companion object {
        private const val TITLE = "TITLE"
        private const val THRESHOLD = "THRESHOLD"
        private const val THRESHOLD_TYPE = "THRESHOLD_TYPE"

        fun newInstance(title: String, currentThreshold: String, thresholdType: Int): StudentThresholdDialog {
            val dialog = StudentThresholdDialog()
            val args = Bundle()
            args.putString(TITLE, title)
            args.putString(THRESHOLD, currentThreshold)
            args.putInt(THRESHOLD_TYPE, thresholdType)
            dialog.arguments = args
            return dialog
        }

        // Taken from https://github.com/afollestad/material-dialogs/blob/master/core/src/main/java/com/afollestad/materialdialogs/internal/MDTintHelper.java
        @ColorInt
        fun resolveColor(context: Context, @AttrRes attr: Int): Int {
            val a = context.theme.obtainStyledAttributes(intArrayOf(attr))
            try {
                return a.getColor(0, 0)
            } finally {
                a.recycle()
            }
        }

        // Taken from https://github.com/afollestad/material-dialogs/blob/master/core/src/main/java/com/afollestad/materialdialogs/internal/MDTintHelper.java
        private fun setCursorTint(editText: EditText, @ColorInt color: Int) {
            try {
                val fCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                fCursorDrawableRes.isAccessible = true
                val mCursorDrawableRes = fCursorDrawableRes.getInt(editText)
                val fEditor = TextView::class.java.getDeclaredField("mEditor")
                fEditor.isAccessible = true
                val editor = fEditor.get(editText)
                val clazz = editor.javaClass
                val fCursorDrawable = clazz.getDeclaredField("mCursorDrawable")
                fCursorDrawable.isAccessible = true
                val drawables = emptyArray<Drawable>()
                drawables[0] = ContextCompat.getDrawable(editText.context, mCursorDrawableRes)
                drawables[1] = ContextCompat.getDrawable(editText.context, mCursorDrawableRes)
                drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN)
                drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN)
                fCursorDrawable.set(editor, drawables)
            } catch (e1: NoSuchFieldException) {
                Log.d("MDTintHelper", "Device issue with cursor tinting: " + e1.message)
                e1.printStackTrace()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
}
