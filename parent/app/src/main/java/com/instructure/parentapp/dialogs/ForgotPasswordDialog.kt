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
package com.instructure.parentapp.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.parentapp.R
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import kotlin.properties.Delegates

class ForgotPasswordDialog : AppCompatDialogFragment() {

    init {
        retainInstance = true
    }

    private var callback: (Int) -> Unit by Delegates.notNull()
    private var emailEditText: AppCompatEditText? = null
    private var emailInvalidTextView: TextView? = null

    companion object {
        val SUCCESS = 1
        val CANCEL = 0

        @JvmStatic
        fun newInstance(callback: (Int) -> Unit): ForgotPasswordDialog {
            val dialog = ForgotPasswordDialog()
            dialog.callback = callback
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val view = View.inflate(ContextThemeWrapper(activity, 0), R.layout.dialog_forgot_password, null)
        setupViews(view)
        builder.setView(view)
        builder.setTitle(R.string.forgotPasswordWithoutQuestion)
        builder.setCancelable(true)
        builder.setPositiveButton(android.R.string.ok, null)
        builder.setNegativeButton(android.R.string.cancel) { _, _ ->
            closeKeyboard()
            callback.invoke(CANCEL)
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            val buttonColor = ContextCompat.getColor(context, R.color.login_loginFlowBlue)
            positiveButton.setTextColor(buttonColor)
            positiveButton.setOnClickListener {
                closeKeyboard()
                forgotPasswordRequest()
            }
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(buttonColor)
        }
        return dialog
    }

    private fun setupViews(view: View) {
        emailEditText = view.findViewById(R.id.forgotPasswordEditText)
        emailInvalidTextView = view.findViewById(R.id.invalidEmailTextView)
        val emailEdtTextTextInputLayout = view.findViewById<TextInputLayout>(R.id.forgotPasswordWrapper)

        val editTextColor = ContextCompat.getColor(context, R.color.login_textButton)
        val themeColor = ContextCompat.getColor(context, R.color.login_loginFlowBlue)

        ViewStyler.themeEditText(context, emailEditText!!, themeColor)
        ViewStyler.themeInputTextLayout(emailEdtTextTextInputLayout, editTextColor)

        emailEditText?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrBlank()) {
                    emailInvalidTextView?.visibility = View.INVISIBLE
                }
            }
        })
    }

    private fun forgotPasswordRequest() {
        val email = emailEditText?.text.toString()
        if(email.isNotBlank()) {
            processRequestPassword(emailEditText?.text.toString())
        } else {
            emailInvalidTextView?.setText(R.string.invalidEmail)
            emailInvalidTextView?.visibility = View.VISIBLE
        }
    }

    private fun processRequestPassword(email: String) {
        UserManager.sendPasswordResetForParentAirwolf(ApiPrefs.airwolfDomain, email, statusCallback)
    }

    private val statusCallback = object : StatusCallback<ResponseBody>() {
        override fun onResponse(response: Response<ResponseBody>, linkHeaders: LinkHeaders, type: ApiType) {
            if (response.code() == 200) {
                //successfully sent, let the user know.
                closeKeyboard()
                callback.invoke(SUCCESS)
                dismiss()
            }
        }

        override fun onFail(call: Call<ResponseBody>?, error: Throwable, response: Response<*>?) {
            if (response?.code() == 404) {
                emailInvalidTextView?.setText(R.string.password_reset_no_user)
                emailInvalidTextView?.visibility = View.VISIBLE
            }
        }
    }

    private fun closeKeyboard() {
        //close the keyboard
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if(emailEditText != null) {
            inputManager?.hideSoftInputFromWindow(emailEditText!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        statusCallback.cancel()
    }

    override fun onDestroyView() {
        val dialog = dialog
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && retainInstance) dialog.setDismissMessage(null)
        super.onDestroyView()
    }
}
