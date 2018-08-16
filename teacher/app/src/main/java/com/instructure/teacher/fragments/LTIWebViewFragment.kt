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
package com.instructure.teacher.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.text.SpannedString
import android.text.TextUtils
import android.widget.Toast
import com.instructure.canvasapi2.managers.SubmissionManager
import com.instructure.canvasapi2.models.LTITool
import com.instructure.canvasapi2.models.Tab
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.HttpHelper
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.BooleanArg
import com.instructure.pandautils.utils.NullableParcelableArg
import com.instructure.pandautils.utils.PermissionUtils
import com.instructure.pandautils.utils.StringArg
import com.instructure.pandautils.views.CanvasWebView
import com.instructure.teacher.R
import com.instructure.teacher.utils.setupMenu
import kotlinx.android.synthetic.main.fragment_internal_webview.*
import kotlinx.coroutines.experimental.Job
import org.json.JSONObject

class LTIWebViewFragment : InternalWebViewFragment() {

    var ltiUrl: String by StringArg()
    var ltiTab: Tab? by NullableParcelableArg()
    var sessionLessLaunch: Boolean by BooleanArg()
    var skipReload: Boolean = false
    private var sessionAuthJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShouldRouteInternally(false)
        title = if(title.isNotBlank()) title else ltiUrl
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Setup the toolbar here so the LTI fragment uses this menu instead of InternalWebViewFragment's
        toolbar.setupMenu(R.menu.menu_internal_webview) {
            if(ltiTab != null) {
                //coming from a tab that is an lti tool
                sessionAuthJob = tryWeave {

                    val result = inBackground {
                        // we have to get a new sessionless url
                        getLTIUrlForTab(this@LTIWebViewFragment.context, ltiTab as Tab)
                    }
                    launchIntent(result)
                } catch {
                    Toast.makeText(this@LTIWebViewFragment.context, R.string.no_apps, Toast.LENGTH_SHORT).show()
                }
            } else {
                sessionAuthJob = tryWeave {
                    if (ApiPrefs.domain in ltiUrl) {
                        // Get an authenticated session so the user doesn't have to log in
                        val result = awaitApi<LTITool> { SubmissionManager.getLtiFromAuthenticationUrl(ltiUrl, it, true) }.url

                        launchIntent(result)
                    }
                } catch  {
                    Toast.makeText(this@LTIWebViewFragment.context, R.string.no_apps, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        skipReload = true
    }

    override fun onResume() {
        super.onResume()
        // After we request permissions to access files (like in Arc) this webview will reload and call onResume again. In order to not break any other LTI things, this flag should skip
        // reloading the url and keep the user where they are
        if(skipReload) {
            skipReload = false
            return
        }
        try {
            if (ltiTab == null) {
                if (url.isNotBlank()) {
                    //modify the url
                    if (url.startsWith("canvas-courses://")) {
                        url = url.replaceFirst("canvas-courses".toRegex(), ApiPrefs.protocol)
                    }
                    val uri = Uri.parse(url).buildUpon()
                            .appendQueryParameter("display", "borderless")
                            .appendQueryParameter("platform", "android")
                            .build()
                    if (sessionLessLaunch) {
                        val sessionless_launch = ApiPrefs.fullDomain +
                                "/api/v1/accounts/self/external_tools/sessionless_launch?url=" + url
                        GetSessionlessLtiURL().execute(sessionless_launch)
                    } else {
                        loadUrl(uri.toString())
                    }
                } else if(ltiUrl.isNotBlank()) {
                    GetSessionlessLtiURL().execute(ltiUrl)
                } else {
                    val spannedString = SpannedString(getString(R.string.errorOccurred))
                    loadHtml(Html.toHtml(spannedString))
                }
            } else {
                GetLtiURL().execute(ltiTab)
            }
        } catch (e: Exception) {
            //if it gets here we're in trouble and won't know what the tab is, so just display an error message
            val spannedString = SpannedString(getString(R.string.errorOccurred))
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                loadHtml(Html.toHtml(spannedString, Html.FROM_HTML_MODE_LEGACY))
            } else {
                loadHtml(Html.toHtml(spannedString))
            }
        }

        canvasWebView?.setCanvasWebChromeClientShowFilePickerCallback(object : CanvasWebView.VideoPickerCallback {
            override fun requestStartActivityForResult(intent: Intent, requestCode: Int) {
                startActivityForResult(intent, requestCode)
            }

            override fun permissionsGranted(): Boolean {
                return if (PermissionUtils.hasPermissions(activity, *PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE))) {
                    true
                } else {
                    requestFilePermissions()
                    false
                }
            }
        })
    }

    override fun onHandleBackPressed(): Boolean {

        if(canGoBack()) {
            //This prevents a silly bug where the arc webview cannot go back far enough to pop it's fragment, but we also want to
            // be able to navigate within the arc webview.
            val webBackForwardList = canvasWebView?.copyBackForwardList()
            val historyUrl = webBackForwardList?.getItemAtIndex(webBackForwardList.currentIndex - 1)?.url
            if(historyUrl != null && (historyUrl.contains("external_tools/")
                    && historyUrl.contains("resource_selection")
                    || (historyUrl.contains("media-picker")))) {
                canvasWebView?.handleGoBack()
                return true
            }
        }
        return super.onHandleBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((canvasWebView?.handleOnActivityResult(requestCode, resultCode, data)) != true) {
            super.onActivityResult(requestCode, resultCode, data)
        }
        // we don't want to reload the LTI now, it may cancel the upload
        skipReload = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (PermissionUtils.allPermissionsGrantedResultSummary(grantResults)) {
            canvasWebView?.clearPickerCallback()
            Toast.makeText(context, R.string.pleaseTryAgain, Toast.LENGTH_SHORT).show()
            skipReload = true
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionAuthJob?.cancel()
    }

    private fun launchIntent(result: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
        // Make sure we can handle the intent
        if (intent.resolveActivity(this@LTIWebViewFragment.context.packageManager) != null) {
            this@LTIWebViewFragment.startActivity(intent)
        } else {
            Toast.makeText(this@LTIWebViewFragment.context, R.string.no_apps, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestFilePermissions() {
        requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA), PermissionUtils.PERMISSION_REQUEST_CODE)
    }

    private inner class GetLtiURL : AsyncTask<Tab, Void, String?>() {

        override fun doInBackground(vararg params: Tab): String? {
            return getLTIUrlForTab(context, params[0])
        }

        override fun onPostExecute(result: String?) {
            if (activity == null || result == null) {
                return
            }

            //make sure we have a non null url before we add parameters
            if (!TextUtils.isEmpty(result)) {
                val uri = Uri.parse(result).buildUpon()
                        .appendQueryParameter("display", "borderless")
                        .appendQueryParameter("platform", "android")
                        .build()
                loadUrl(uri.toString())
            } else {
                loadUrl(result)
            }
        }
    }

    private inner class GetSessionlessLtiURL : AsyncTask<String, Void, String?>() {

        override fun doInBackground(vararg params: String): String? {
            return getLTIUrl(context, params[0])
        }

        override fun onPostExecute(result: String?) {
            if (activity == null || result == null) {
                return
            }

            //make sure we have a non null url before we add parameters
            if (!TextUtils.isEmpty(result)) {
                val uri = Uri.parse(result).buildUpon()
                        .appendQueryParameter("display", "borderless")
                        .appendQueryParameter("platform", "android")
                        .build()
                loadUrl(uri.toString())
            } else {
                loadUrl(result)
            }
        }
    }

    private fun getLTIUrlForTab(context: Context, tab: Tab): String? {
        return getLTIUrl(context, tab.ltiUrl)
    }

    private fun getLTIUrl(context: Context, url: String): String? {
        try {
            val result = HttpHelper.externalHttpGet(context, url, true).responseBody
            var ltiUrl: String? = null
            if (result != null) {
                val ltiJSON = JSONObject(result)
                ltiUrl = ltiJSON.getString("url")
            }
            return ltiUrl
        } catch (e: Exception) {
            return null
        }
    }

    companion object {
        const val LTI_URL = "lti_url"
        const val TAB = "tab"
        const val SESSION_LESS = "session_less"

        @JvmStatic
        fun makeLTIBundle(ltiUrl: String): Bundle {
            val args = Bundle()
            args.putString(LTI_URL, ltiUrl)
            return args
        }

        @JvmStatic
        fun makeLTIBundle(ltiTab: Tab): Bundle {
            val args = Bundle()
            args.putParcelable(TAB, ltiTab)
            return args
        }

        @JvmStatic
        fun makeLTIBundle(ltiUrl: String, title: String, sessionLessLaunch: Boolean): Bundle {
            val args = Bundle()
            args.putString(LTI_URL, ltiUrl)
            args.putBoolean(SESSION_LESS, sessionLessLaunch)
            args.putString(TITLE, title)
            return args
        }

        @JvmStatic
        fun newInstance(args: Bundle) = LTIWebViewFragment().apply {
            ltiUrl = args.getString(LTI_URL, "")
            title = args.getString(TITLE, "")
            sessionLessLaunch = args.getBoolean(SESSION_LESS, false)
            if(args.containsKey(TAB)) {
                ltiTab = args.getParcelable(TAB)
            }
            setShouldLoadUrl(false)
        }
    }
}
