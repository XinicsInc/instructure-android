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
package com.instructure.parentapp.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AlertDialog
import android.webkit.WebView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.apis.AlertAPI
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.*
import com.instructure.loginapi.login.activities.BaseLoginSignInActivity
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.Prefs
import com.instructure.parentapp.BuildConfig
import com.instructure.parentapp.R
import com.instructure.parentapp.dialogs.OutOfRegionDialog
import com.instructure.parentapp.util.ApplicationManager
import com.instructure.parentapp.util.Const.CANVAS_PARENT_SP
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

class SignInActivity : BaseLoginSignInActivity() {

    override fun launchApplicationMainActivityIntent(): Intent? {
        return null
    }

    override fun handleLaunchApplicationMainActivityIntent() {
        //Get the parent students
        UserManager.getStudentsForParentAirwolf(ApiPrefs.airwolfDomain,
                ApplicationManager.getParentId(ContextKeeper.appContext), getStudentsStatusCallback)
    }

    private fun launchIntoApplicationMainActivityIntent(students: List<Student>) {
        if(asActivityResult()) {
            val intent = Intent()
            intent.putParcelableArrayListExtra(Const.STUDENT, ArrayList<Parcelable>(students))
            setResult(Activity.RESULT_OK, intent)
        } else {
            val intent = StudentViewActivity.createIntent(ContextKeeper.appContext, students)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun launchIntoFindStudentsActivityIntent() {
        val intent = FindSchoolActivity.createIntent(this, true, asActivityResult())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun refreshWidgets() {
        //No widgets in Canvas Parent
    }

    override fun userAgent(): String {
        return "androidParent"
    }

    private fun findStudent(): Boolean {
        return intent.getBooleanExtra(FIND_STUDENT, true)
    }

    private fun asActivityResult(): Boolean {
        return intent.getBooleanExtra(AS_ACTIVITY_FOR_RESULT, false)
    }

    /**
     * Overrides the beginSignIn because airwolf will validate via mobileverify for us. This works around
     * using mobile verify for the initial airwolf calls that occur.
     */
    override fun beginSignIn(accountDomain: AccountDomain) {
        if (findStudent()) {
            UserManager.addStudentToParentAirwolf(
                    ApiPrefs.airwolfDomain, ApplicationManager.getParentId(ContextKeeper.appContext), accountDomain.domain, object : StatusCallback<ResponseBody>() {
                override fun onResponse(response: Response<ResponseBody>, linkHeaders: LinkHeaders, type: ApiType) {}

                override fun onFail(call: Call<ResponseBody>?, error: Throwable, response: Response<*>?) {
                    if (response?.code() == 302) {
                        val headers = response.headers()

                        if (headers.values("Location") != null) {
                            val domain = headers.values("Location")[0]
                            setAuthenticationUrl(addAuthenticationProvider(accountDomain, domain))
                            loadAuthenticationUrl("", domain, BuildConfig.LOGIN_CLIENT_ID, BuildConfig.LOGIN_CLIENT_SECRET)
                        }

                    } else if (response?.code() == 400 || response?.code() == 401) {
                        Toast.makeText(this@SignInActivity, getString(R.string.badDomainError), Toast.LENGTH_SHORT).show()
                    } else if (response?.code() == 403) {
                        showAccountDisabledDialog()
                    } else if(response?.code() == 451) {
                        try {
                            val json = JsonParser().parse(response.errorBody()?.string())
                            val mismatchedRegionResponse = Gson().fromJson<MismatchedRegionResponse>(json, MismatchedRegionResponse::class.java)
                            OutOfRegionDialog.newInstance(BaseParentActivity.getReadableRegion(
                                    this@SignInActivity, mismatchedRegionResponse.studentRegion), { finish() })
                                    .show(supportFragmentManager, OutOfRegionDialog::class.java.simpleName)
                        } catch (e: Throwable) {
                            finish()
                        }
                    }
                }
            })
        } else {
            UserManager.authenticateCanvasParentAirwolf(ApiPrefs.airwolfDomain, accountDomain.domain, airwolfAuthCallback)
        }
    }

    /**
     * Adds an authentication provider if one exists and returns a domain. This is typically
     * done in the BaseLoginSignInActivity.java but is overridden because of the Airwolf login flow.
     */
    private fun addAuthenticationProvider(accountDomain: AccountDomain, domain: String): String {
        val authenticationProvider = accountDomain.authenticationProvider
        if (authenticationProvider != null && authenticationProvider.isNotEmpty()) {
            Logger.d("authentication_provider=" + authenticationProvider)
            val uri = Uri.parse(domain)
            val newDomain = uri.buildUpon().appendQueryParameter("authentication_provider", authenticationProvider).build().toString()
            Logger.d("Modified Domain: " + newDomain)
            return newDomain
        }
        return domain
    }

    private fun loadAirwolfDomain(airwolfDomain: String) {
        ApiPrefs.airwolfDomain = getAirwolfDomainFromUrl(airwolfDomain)
        UserManager.authenticateCanvasParentAirwolf(
                getAirwolfDomainFromUrl(airwolfDomain),
                addAuthenticationProvider(accountDomain, accountDomain.domain),
                airwolfAuthCallback
        )
    }

    private val airwolfAuthCallback = object: StatusCallback<ParentResponse>() {
        override fun onResponse(response: Response<ParentResponse>, linkHeaders: LinkHeaders, type: ApiType) {}

        override fun onFail(call: Call<ParentResponse>?, error: Throwable, response: Response<*>?) {
            if (response?.code() == 302) {
                val headers = response.headers()

                if (headers.values("Location") != null
                        && headers.values("Location").size > 0
                        && headers.values("Location")[0] != null) {

                    // Check for redirect to different region
                    val redirectUrl = headers.values("Location")[0]
                    if (redirectUrl.contains("login/oauth2")) {
                        // We've redirected successfully; move along

                        val domain = headers.values("Location")[0]
                        setAuthenticationUrl(addAuthenticationProvider(accountDomain, domain))
                        loadAuthenticationUrl("", domain, BuildConfig.LOGIN_CLIENT_ID, BuildConfig.LOGIN_CLIENT_SECRET)
                    } else {
                        // Different region, update it and reauthenticate with AirWolf
                        loadAirwolfDomain(redirectUrl)
                    }
                }

            } else if (response?.code() == 400 || response?.code() == 401) {
                Toast.makeText(this@SignInActivity, getString(R.string.badDomainError), Toast.LENGTH_SHORT).show()
            } else if (response?.code() == 403) {
                showAccountDisabledDialog()
            }
        }
    }

    /**
     * Overrides the shouldOverrideUrlLoading to handle Canvas Parents specific use case.
     */
    override fun overrideUrlLoading(view: WebView, url: String): Boolean {
        when {
            url.contains(PARENT_SUCCESS_URL) -> {
                handleLaunchApplicationMainActivityIntent()
                return true
            }
            url.contains(PARENT_CANCEL_URL) -> { return false }
            url.contains(PARENT_ERROR_URL) -> {
                clearCookies()
                if (findStudent()) {
                    Toast.makeText(this@SignInActivity, R.string.unableToAddStudentError, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@SignInActivity, R.string.onlyCanvasObservers, Toast.LENGTH_SHORT).show()
                }
                return true
            }
            url.contains(PARENT_TOKEN_URL) -> {
                //when a parent logs in with observer credentials
                //get the parent id from the url
                val parentId = "parent_id="
                val token = "token="
                var index = url.indexOf(parentId)
                if (index != -1) {
                    val endIndex = url.indexOf("&", index)
                    val prefs = Prefs(ContextKeeper.appContext, CANVAS_PARENT_SP)
                    prefs.save(Const.ID, url.substring(index + parentId.length, endIndex))
                }
                index = url.indexOf(token)
                if (index != -1) {
                    ApiPrefs.token = url.substring(index + token.length)
                }
                handleLaunchApplicationMainActivityIntent()
                return true
            }
            else -> return false
        }
    }

    private val getStudentsStatusCallback = object : StatusCallback<List<Student>>() {
        override fun onResponse(response: Response<List<Student>>, linkHeaders: LinkHeaders, type: ApiType) {
            if (findStudent()) {
                if (type == ApiType.API) {
                    //they have students that they are observing
                    clearCookies() //clear cookies for security
                    response.body()?.let { launchIntoApplicationMainActivityIntent(it) }
                }
            } else {
                clearCookies() //clear cookies for security
                if (response.body() != null) {
                    response.body()?.let {
                        if (!it.isEmpty()) {
                            launchIntoApplicationMainActivityIntent(it)
                        } else {
                            launchIntoFindStudentsActivityIntent()
                        }
                    }
                } else {
                    //Take the parent to the add user page.
                    launchIntoFindStudentsActivityIntent()
                }
            }
        }
    }

    private fun showAccountDisabledDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.access_not_enabled)
        builder.setPositiveButton(R.string.dismiss) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    override fun onDestroy() {
        super.onDestroy()
        getStudentsStatusCallback.cancel()
        airwolfAuthCallback.cancel()
    }

    companion object {
        private val PARENT_SUCCESS_URL = "/oauthSuccess"
        private val PARENT_CANCEL_URL = "/oauth2/deny"
        private val PARENT_ERROR_URL = "/oauthFailure"
        private val PARENT_TOKEN_URL = "/canvas/tokenReady"

        private val FIND_STUDENT = "findStudent"
        private val AS_ACTIVITY_FOR_RESULT = "asActivityForResult"

        fun createIntent(context: Context, accountDomain: AccountDomain, findStudent: Boolean): Intent {
            val intent = Intent(context, SignInActivity::class.java)
            val extras = Bundle()
            extras.putParcelable(BaseLoginSignInActivity.ACCOUNT_DOMAIN, accountDomain)
            extras.putBoolean(FIND_STUDENT, findStudent)
            intent.putExtras(extras)
            return intent
        }

        fun createIntent(context: Context, accountDomain: AccountDomain, findStudent: Boolean, asActivityForResult: Boolean): Intent {
            val intent = Intent(context, SignInActivity::class.java)
            val extras = Bundle()
            extras.putParcelable(BaseLoginSignInActivity.ACCOUNT_DOMAIN, accountDomain)
            extras.putBoolean(AS_ACTIVITY_FOR_RESULT, asActivityForResult)
            extras.putBoolean(FIND_STUDENT, findStudent)
            intent.putExtras(extras)
            return intent
        }

        /**
         * Compares the url with airwolf regions and passes back the one that matches
         * Used specifically for the case of a redirect when we need to change the airwolf domain based on the
         * url in the redirect from AirWolf authentication
         *
         * @param url The url to check
         * @return The airwolf domain that matches the url passed in
         */
        private fun getAirwolfDomainFromUrl(url: String): String {
            if (url.contains(AlertAPI.AIRWOLF_DOMAIN_AMERICA)) return AlertAPI.AIRWOLF_DOMAIN_AMERICA
            if (url.contains(AlertAPI.AIRWOLF_DOMAIN_DUBLIN)) return AlertAPI.AIRWOLF_DOMAIN_DUBLIN
            if (url.contains(AlertAPI.AIRWOLF_DOMAIN_SYDNEY)) return AlertAPI.AIRWOLF_DOMAIN_SYDNEY
            if (url.contains(AlertAPI.AIRWOLF_DOMAIN_SINGAPORE)) return AlertAPI.AIRWOLF_DOMAIN_SINGAPORE
            if (url.contains(AlertAPI.AIRWOLF_DOMAIN_FRANKFURT)) return AlertAPI.AIRWOLF_DOMAIN_FRANKFURT
            return if (url.contains(AlertAPI.AIRWOLF_DOMAIN_CANADA)) AlertAPI.AIRWOLF_DOMAIN_CANADA else ""
        }
    }
}