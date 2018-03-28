/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.Parent
import com.instructure.canvasapi2.models.ParentResponse
import com.instructure.canvasapi2.models.Student
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.Prefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.parentapp.R
import kotlinx.android.synthetic.main.activity_create_account.*

import retrofit2.Call
import retrofit2.Response

class CreateAccountActivity : AppCompatActivity() {

    private val saveButton: TextView? get() = findViewById(R.id.createAccount)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewStyler.setStatusBarLight(this)
        setContentView(R.layout.activity_create_account)
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        //Setting edit text to password here prevents the font from being changed
        createAccountPassword.transformationMethod = PasswordTransformationMethod()
        createAccountPasswordConfirm.transformationMethod = PasswordTransformationMethod()
        //we don't want the keyboard to display suggestions for passwords
        createAccountPassword.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        createAccountPasswordConfirm.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        val editTextColor = ContextCompat.getColor(this, R.color.login_textButton)
        ViewStyler.themeInputTextLayout(emailWrapper, editTextColor)
        ViewStyler.themeInputTextLayout(firstNameWrapper, editTextColor)
        ViewStyler.themeInputTextLayout(lastNameWrapper, editTextColor)
        ViewStyler.themeInputTextLayout(passwordWrapper, editTextColor)
        ViewStyler.themeInputTextLayout(passwordConfirmationWrapper, editTextColor)

        val themeColor = ContextCompat.getColor(this, R.color.login_loginFlowBlue)
        ViewStyler.themeEditText(this, email, themeColor)
        ViewStyler.themeEditText(this, firstName, themeColor)
        ViewStyler.themeEditText(this, lastName, themeColor)
        ViewStyler.themeEditText(this, createAccountPassword, themeColor)
        ViewStyler.themeEditText(this, createAccountPasswordConfirm, themeColor)

        //Setup the toolbar
        toolbar.setNavigationIcon(R.drawable.vd_close_white)
        toolbar.setNavigationContentDescription(R.string.close)
        toolbar.setTitle(R.string.createAccount)
        toolbar.inflateMenu(R.menu.menu_create_account)
        ViewStyler.themeToolbar(this, toolbar, Color.WHITE, Color.BLACK)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setOnMenuItemClickListener {
            if(it.itemId == R.id.createAccount) {
                createAccount()
            }
            true
        }

        saveButton?.isEnabled = false
        saveButton?.setTextColor(ContextCompat.getColor(this, R.color.login_grayCanvasLogo))
    }

    private fun passwordsEqual(): Boolean {
        return createAccountPassword.text.toString().isNotBlank()
                && createAccountPasswordConfirm.text.toString().isNotBlank()
                && createAccountPassword.text.toString() == createAccountPasswordConfirm.text.toString()
    }

    private fun setupListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (charSequence.isNotEmpty() && dataIsValid()) {
                    saveButton?.isEnabled = true
                    saveButton?.setTextColor(ContextCompat.getColor(this@CreateAccountActivity, R.color.login_loginFlowBlue))
                } else {
                    saveButton?.isEnabled = false
                    saveButton?.setTextColor(ContextCompat.getColor(this@CreateAccountActivity, R.color.login_grayCanvasLogo))
                }
            }
        }

        createAccountPassword.addTextChangedListener(textWatcher)
        createAccountPasswordConfirm.addTextChangedListener(textWatcher)

        email.addTextChangedListener(textWatcher)
        firstName.addTextChangedListener(textWatcher)
        lastName.addTextChangedListener(textWatcher)

        createAccountPasswordConfirm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                //create account
                createAccount()
            }
            false
        }
    }

    private fun dataIsValid(): Boolean {
        return email.text.toString().isNotBlank()
                && firstName.text.toString().isNotBlank()
                && lastName.text.toString().isNotBlank()
                && passwordsEqual()
    }

    private fun createAccount() {
        if(dataIsValid()) {
            val parent = Parent()
            parent.username = email.text.toString()
            parent.password = createAccountPassword.text.toString()
            parent.firstName = firstName.text.toString()
            parent.lastName = lastName.text.toString()

            UserManager.addParentAirwolf(ApiPrefs.airwolfDomain, parent, object : StatusCallback<ParentResponse>() {
                override fun onResponse(response: Response<ParentResponse>, linkHeaders: LinkHeaders, type: ApiType) {
                    val prefs = Prefs(this@CreateAccountActivity, com.instructure.parentapp.util.Const.CANVAS_PARENT_SP)
                    prefs.save(Const.ID, response.body()?.parentId)
                    prefs.save(Const.NAME, email.text.toString())

                    //success. Save the id and token
                    response.body()?.let { ApiPrefs.token = it.token }

                    //Take the parent to the add user page.
                    //We want to refresh cache so the main activity can load quickly with accurate information
                    UserManager.getStudentsForParentAirwolf(
                            ApiPrefs.airwolfDomain,
                            response.body()?.parentId,
                            object : StatusCallback<List<Student>>() {
                                override fun onResponse(response: Response<List<Student>>, linkHeaders: LinkHeaders, type: ApiType) {
                                    //restart the main activity
                                    val intent = SplashActivity.createIntent(this@CreateAccountActivity)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            })
                }

                override fun onFail(call: Call<ParentResponse>?, error: Throwable, response: Response<*>?) {
                    if (response?.code() == 400) {
                        Toast.makeText(this@CreateAccountActivity, getString(R.string.email_already_exists), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CreateAccountActivity, getString(R.string.errorOccurred), Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, CreateAccountActivity::class.java)
        }
    }
}
