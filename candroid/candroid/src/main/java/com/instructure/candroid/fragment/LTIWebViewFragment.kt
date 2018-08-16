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

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.instructure.candroid.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.SpannedString
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.canvasapi2.managers.OAuthManager
import com.instructure.canvasapi2.models.AuthenticatedSession
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Tab
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.pageview.BeforePageView
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.pageview.PageViewUrl
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.pandautils.utils.*
import kotlinx.coroutines.experimental.Job
import org.json.JSONObject

@PageView
open class LTIWebViewFragment : InternalWebviewFragment() {

    private var ltiUrlLaunchJob: Job? = null
    private var ltiUrl: String by StringArg()
    private var ltiTab: Tab? by NullableParcelableArg()
    private var sessionLessLaunch: Boolean by BooleanArg()
    private var externalUrlToLoad: String? = null
    private var sessionAuthJob: Job? = null

    @PageViewUrl
    private fun makePageViewUrl() = ltiTab?.externalUrl ?: ApiPrefs.fullDomain + canvasContext.toAPIString() + "/external_tools"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShouldRouteInternally(false)
    }

    override fun title(): String {
        if(title.isNullOrBlank() && ltiUrl.isBlank()) {
            return ltiTab?.label ?: ""
        } 
        return if (title.isNullOrBlank()) ltiUrl else title!!
    }

    override fun applyTheme() {
        toolbar?.title = title()
        toolbar?.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar!!, canvasContext)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val hideToolbar = arguments?.getBoolean(HIDE_TOOLBAR, false) ?: false
        toolbar?.visibility = if(hideToolbar) View.GONE else View.VISIBLE
        toolbar?.let {
            setupToolbarMenu(it, R.menu.menu_internal_webview)
        }
    }

    override fun handleBackPressed(): Boolean {
        if(canGoBack()) {
            //This prevents users from going back to the launch url for LITs, that url shows an error message and is
            //only used to forward the user to the actual LTI.
            val webBackForwardList = getCanvasWebView()?.copyBackForwardList()
            val historyUrl = webBackForwardList?.getItemAtIndex(webBackForwardList.currentIndex - 1)?.url
            if(historyUrl != null && historyUrl.contains("external_tools/sessionless_launch")) {
                navigation?.popCurrentFragment()
                return true
            }
        }
        return super.handleBackPressed()
    }

    override fun onResume() {
        super.onResume()
        try {
            if(ltiTab != null) {
                getLtiUrl(ltiTab)
            } else {
                if (ltiUrl.isNotBlank()) {
                    //modify the url
                    if (ltiUrl.startsWith("canvas-courses://")) {
                        ltiUrl = ltiUrl.replaceFirst("canvas-courses".toRegex(), ApiPrefs.protocol)
                    }

                    if (sessionLessLaunch) {
                        getSessionlessLtiUrl(ApiPrefs.fullDomain + "/api/v1/accounts/self/external_tools/sessionless_launch?url=" + ltiUrl)
                    } else {
                        externalUrlToLoad = ltiUrl

                        loadUrl(Uri.parse(ltiUrl).buildUpon()
                                .appendQueryParameter("display", "borderless")
                                .appendQueryParameter("platform", "android")
                                .build()
                                .toString())
                    }
                } else if (ltiUrl.isNotBlank()) {
                    getSessionlessLtiUrl(ltiUrl)
                } else {
                    loadDisplayError()
                }
            }
        } catch (e: Exception) {
            //if it gets here we're in trouble and won't know what the tab is, so just display an error message
            loadDisplayError()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.launchExternalWeb) {

            if (ltiTab != null) {
                //coming from a tab that is an lti tool
                sessionAuthJob = tryWeave {

                    val result = inBackground {
                        // we have to get a new sessionless url
                        getLTIUrlForTab(this@LTIWebViewFragment.context, ltiTab as Tab)
                    }
                    launchIntent(result)
                } catch {
                    Toast.makeText(this@LTIWebViewFragment.context, R.string.utils_unableToViewInBrowser, Toast.LENGTH_SHORT).show()
                }
            } else {
                // coming from anywhere else
                var url = ltiUrl
                if (externalUrlToLoad.isValid()) {
                    url = externalUrlToLoad!!
                }

                sessionAuthJob = tryWeave {
                    if (ApiPrefs.domain in url) {
                        // Get an authenticated session so the user doesn't have to log in
                        url = awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(url, it) }.sessionUrl

                        launchIntent(url)
                    }
                } catch  {
                    Toast.makeText(this@LTIWebViewFragment.context, R.string.utils_unableToViewInBrowser, Toast.LENGTH_SHORT).show()
                }
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchIntent(result: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
        // Make sure we can handle the intent
        if (intent.resolveActivity(this@LTIWebViewFragment.context.packageManager) != null) {
            this@LTIWebViewFragment.startActivity(intent)
        } else {
            Toast.makeText(this@LTIWebViewFragment.context, R.string.utils_unableToViewInBrowser, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLtiUrl(ltiTab: Tab?) {
        if(ltiTab == null) {
            loadDisplayError()
            return
        }

        ltiUrlLaunchJob = weave {
            var result: String? = null
            inBackground {
                result = getLTIUrlForTab(this@LTIWebViewFragment.context, ltiTab)
            }

            if(result != null) {
                val uri = Uri.parse(result).buildUpon()
                        .appendQueryParameter("display", "borderless")
                        .appendQueryParameter("platform", "android")
                        .build()
                externalUrlToLoad = uri.toString()
                loadUrl(uri.toString())
            } else {
                //error
                loadDisplayError()
            }
        }
    }

    private fun getSessionlessLtiUrl(ltiUrl: String) {
        ltiUrlLaunchJob = weave {
            var result: String? = null
            inBackground {
                result = getLTIUrl(this@LTIWebViewFragment.context, ltiUrl)
            }

            if(result != null) {
                val uri = Uri.parse(result).buildUpon()
                        .appendQueryParameter("display", "borderless")
                        .appendQueryParameter("platform", "android")
                        .build()
                // Set the sessionless url here in case the user wants to use an external browser
                externalUrlToLoad = uri.toString()

                loadUrl(uri.toString())
            } else {
                //error
                loadDisplayError()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadDisplayError() {
        val spannedString = SpannedString(getString(R.string.errorOccurred))
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            loadHtml(Html.toHtml(spannedString, Html.FROM_HTML_MODE_LEGACY))
        } else {
            loadHtml(Html.toHtml(spannedString))
        }
    }

    @BeforePageView
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

    override fun allowBookmarking(): Boolean {
        return false
    }

    override fun handleIntentExtras(extras: Bundle?) {
        super.handleIntentExtras(extras)

        extras?.let {
            if (it.containsKey(Const.TITLE)) title = it.getString(Const.TITLE)
            if (it.containsKey(Const.TAB)) ltiTab = it.getParcelable(Const.TAB)
            ltiUrl = it.getString(LTI_URL, "")
            sessionLessLaunch = it.getBoolean(Const.SESSIONLESS_LAUNCH, false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ltiUrlLaunchJob?.cancel()
        sessionAuthJob?.cancel()
    }

    companion object {
        const val LTI_URL = "ltiUrl"
        val HIDE_TOOLBAR = "hideToolbar"

        fun newInstance(args: Bundle): LTIWebViewFragment {
            val fragment = LTIWebViewFragment()
            fragment.arguments = args
            return fragment
        }

        fun createBundle(canvasContext: CanvasContext, ltiTab: Tab?): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putBoolean(Const.AUTHENTICATE, false)
            extras.putParcelable(Const.TAB, ltiTab)
            return extras
        }

        @JvmStatic
        fun createBundle(canvasContext: CanvasContext, url: String): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putBoolean(Const.AUTHENTICATE, false)
            extras.putString(LTI_URL, url)
            return extras
        }

        @JvmStatic
        fun createBundle(canvasContext: CanvasContext, url: String, title: String, sessionLessLaunch: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putBoolean(Const.AUTHENTICATE, false)
            extras.putString(LTI_URL, url)
            extras.putBoolean(Const.SESSIONLESS_LAUNCH, sessionLessLaunch)
            extras.putString(Const.TITLE, title)
            return extras
        }

        @JvmStatic
        fun createBundle(canvasContext: CanvasContext, url: String, title: String, sessionLessLaunch: Boolean, hideToolbar: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putBoolean(Const.AUTHENTICATE, false)
            extras.putString(LTI_URL, url)
            extras.putBoolean(HIDE_TOOLBAR, hideToolbar)
            extras.putBoolean(Const.SESSIONLESS_LAUNCH, sessionLessLaunch)
            extras.putString(Const.TITLE, title)
            return extras
        }
    }
}
