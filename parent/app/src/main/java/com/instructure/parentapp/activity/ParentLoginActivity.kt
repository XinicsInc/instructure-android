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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.Toast

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.apis.AlertAPI
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.ParentResponse
import com.instructure.canvasapi2.models.Student
import com.instructure.canvasapi2.utils.*
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.Prefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.parentapp.R
import com.instructure.parentapp.dialogs.ForgotPasswordDialog
import com.instructure.parentapp.dialogs.SelectRegionDialog
import kotlinx.android.synthetic.main.activity_parent_login.*

import retrofit2.Call
import retrofit2.Response

class ParentLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewStyler.setStatusBarLight(this)
        setContentView(R.layout.activity_parent_login)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        //Setting edit text to password here prevents the font from being changed
        password.transformationMethod = PasswordTransformationMethod()
        //we don't want the keyboard to display suggestions for passwords
        password.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val editTextColor = ContextCompat.getColor(this, R.color.login_textButton)
        val themeColor = ContextCompat.getColor(this, R.color.login_loginFlowBlue)

        ViewStyler.themeInputTextLayout(usernameWrapper, editTextColor)
        ViewStyler.themeInputTextLayout(passwordWrapper, editTextColor)
        ViewStyler.themeEditText(this, username, themeColor)
        ViewStyler.themeEditText(this, password, themeColor)
    }

    private fun setupListeners() {
        parentLoginButton.setOnClickListener {
            UserManager.authenticateParentAirwolf(ApiPrefs.airwolfDomain, username.text.toString(), password.text.toString(), object : StatusCallback<ParentResponse>() {
                override fun onResponse(response: Response<ParentResponse>, linkHeaders: LinkHeaders, type: ApiType) {
                    response.body()?.let { ApiPrefs.token = it.token }
                    val prefs = Prefs(ContextKeeper.appContext, getString(R.string.app_name_parent))
                    prefs.save(Const.ID, response.body()?.parentId)
                    prefs.save(Const.NAME, username.text.toString())

                    UserManager.getStudentsForParentAirwolf(ApiPrefs.airwolfDomain, response.body()?.parentId, object : StatusCallback<List<Student>>() {
                        override fun onResponse(response: Response<List<Student>>, linkHeaders: LinkHeaders, type: ApiType) {
                            if (!APIHelper.isCachedResponse(response)) {
                                val body = response.body()
                                if (body != null && body.isNotEmpty()) {
                                    // They have students that they are observing, take them to that activity
                                    startActivity(StudentViewActivity.createIntent(ContextKeeper.appContext, body))
                                } else {
                                    // No response or no students observable; Take the parent to the add user page.
                                    startActivity(FindSchoolActivity.createIntent(ContextKeeper.appContext, true))
                                }
                                // Finish the activity so they can't hit the back button and see the login screen again
                                finish()
                            }
                        }
                    })
                }

                override fun onFail(call: Call<ParentResponse>?, error: Throwable, response: Response<*>?) {
                    Toast.makeText(this@ParentLoginActivity, getString(R.string.invalid_username_password), Toast.LENGTH_SHORT).show()
                }
            })
        }

        createAccount.setOnClickListener { startActivity(CreateAccountActivity.createIntent(ContextKeeper.appContext)) }

        forgotPassword.setOnClickListener {
            ForgotPasswordDialog.newInstance {
                if (it == ForgotPasswordDialog.SUCCESS) {
                    Toast.makeText(ContextKeeper.appContext, getString(R.string.password_reset_success), Toast.LENGTH_SHORT).show()
                }
            }.show(supportFragmentManager, ForgotPasswordDialog::class.java.simpleName)
        }

        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                parentLoginButton.performClick() //log in
            }
            false
        }

        canvasLoginButton.setOnClickListener {
            startActivity(FindSchoolActivity.createIntent(ContextKeeper.appContext, false))
        }

        selectRegion.setOnClickListener {
            SelectRegionDialog.newInstance {
                ApiPrefs.airwolfDomain = it
            }.show(supportFragmentManager, SelectRegionDialog::class.java.simpleName)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, ParentLoginActivity::class.java)
        }
    }
}
