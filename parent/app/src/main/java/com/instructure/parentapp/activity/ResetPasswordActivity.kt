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

package com.instructure.parentapp.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.ResetParent
import com.instructure.canvasapi2.models.Student
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.ContextKeeper
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.Prefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.parentapp.R
import kotlinx.android.synthetic.main.activity_reset_password.*

import retrofit2.Call
import retrofit2.Response

/**
 * This activity will show when the user clicks the link in their email that is sent when
 * they try to reset their password.
 */
class ResetPasswordActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewStyler.setStatusBarLight(this)
        setContentView(R.layout.activity_reset_password)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        //Setting edit text to password here prevents the font from being changed
        resetPasswordEditText.transformationMethod = PasswordTransformationMethod()
        resetPasswordConfirmEditText.transformationMethod = PasswordTransformationMethod()
        //we don't want the keyboard to display suggestions for passwords
        resetPasswordEditText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        resetPasswordConfirmEditText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val editTextColor = ContextCompat.getColor(this, R.color.login_textButton)
        ViewStyler.themeInputTextLayout(resetPasswordEditTextWrapper, editTextColor)
        ViewStyler.themeInputTextLayout(resetPasswordConfirmEditTextWrapper, editTextColor)

        val themeColor = ContextCompat.getColor(this, R.color.login_loginFlowBlue)
        ViewStyler.themeEditText(this, resetPasswordEditText, themeColor)
        ViewStyler.themeEditText(this, resetPasswordConfirmEditText, themeColor)
    }

    private fun setupListeners() {
        resetPasswordButton.setOnClickListener {
            resetPasswordRequest(resetPasswordEditText!!.text.toString(), resetPasswordConfirmEditText!!.text.toString())
        }

        resetPasswordEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrBlank()) {
                    invalidPasswordsTextView?.visibility = View.INVISIBLE
                }
            }
        })

        resetPasswordConfirmEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(!s.isNullOrBlank()) {
                    invalidPasswordsTextView?.visibility = View.INVISIBLE
                }
            }
        })
    }

    private fun resetPasswordRequest(password: String, passwordConfirmation: String) {
        if(password.isNotBlank() && passwordConfirmation.isNotBlank()) {
            //Parse the url
            val resetUrl = intent.dataString
            val data = Uri.parse(resetUrl)

            if (data == null) {
                Toast.makeText(this@ResetPasswordActivity, R.string.passwordResetFailed, Toast.LENGTH_SHORT).show()
                return
            }

            var username: String? = null
            var token: String? = null

            //try to get the parent id
            var index = resetUrl.indexOf("username")
            if (index != -1) {
                index += "username=".length

                val endIndex = resetUrl.indexOf("&", index)

                if (endIndex != -1) {
                    username = resetUrl.substring(index, endIndex)
                }
            }

            //try to get the token
            index = resetUrl.indexOf("recovery_token")
            if (index != -1) {
                index += "recovery_token=".length

                val endIndex = resetUrl.length

                if (endIndex != -1) {
                    token = resetUrl.substring(index, endIndex)
                }
            }

            if(!username.isNullOrBlank() || !token.isNullOrBlank()) {
                if(password == passwordConfirmation) {
                    //set the user's token to the temporary token
                    ApiPrefs.token = token!!
                    UserManager.resetParentPasswordAirwolf(ApiPrefs.airwolfDomain, username, password, statusCallback)
                } else {
                    showInvalidPasswordMessage(R.string.passwordsDoNotMatch)
                }
            } else {
                showInvalidPasswordMessage(R.string.passwordsCannotBeBlank)
            }
        } else {
            showInvalidPasswordMessage(R.string.passwordsCannotBeBlank)
        }
    }

    private fun showInvalidPasswordMessage(message: Int) {
        invalidPasswordsTextView?.setText(message)
        invalidPasswordsTextView?.visibility = View.VISIBLE
    }

    private val statusCallback = object : StatusCallback<ResetParent>() {
        override fun onResponse(response: Response<ResetParent>, linkHeaders: LinkHeaders, type: ApiType) {
            //success, now set the user's token to the one just created
            response.body()?.let {
                ApiPrefs.token = it.token

                val prefs = Prefs(ContextKeeper.appContext, getString(R.string.app_name_parent))
                prefs.save(Const.ID, it.parentId)

                //try to get the students. when we start the main activity it will check the cached values,
                //which at this point there won't be any
                UserManager.getStudentsForParentAirwolf(ApiPrefs.airwolfDomain, it.parentId, object : StatusCallback<List<Student>>() {
                    override fun onResponse(response: Response<List<Student>>, linkHeaders: LinkHeaders, type: ApiType) {
                        Toast.makeText(this@ResetPasswordActivity, R.string.requestPasswordSuccess, Toast.LENGTH_SHORT).show()
                        val intent = SplashActivity.createIntent(this@ResetPasswordActivity)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                })
            }
        }

        override fun onFail(call: Call<ResetParent>?, error: Throwable, response: Response<*>?) {
            //reset the token to be nothing so when we open the app it won't try to use the token
            ApiPrefs.token = ""
            Toast.makeText(this@ResetPasswordActivity, R.string.passwordResetFailed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        statusCallback.cancel()
    }
}