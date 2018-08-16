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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.activity.InternalWebViewActivity
import com.instructure.interactions.Navigation
import com.instructure.candroid.util.FragUtils
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.managers.OAuthManager
import com.instructure.canvasapi2.managers.SubmissionManager
import com.instructure.canvasapi2.models.AuthenticatedSession
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.LTITool
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.FileUtils
import com.instructure.canvasapi2.utils.Logger
import com.instructure.canvasapi2.utils.isValid
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.canvasapi2.utils.weave.StatusCallbackError
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.CanvasWebView
import kotlinx.android.synthetic.main.fragment_webview.*
import kotlinx.android.synthetic.main.fragment_webview.view.*
import kotlinx.coroutines.experimental.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

open class InternalWebviewFragment : ParentFragment() {

    var toolbar: Toolbar? = null

    var url: String? by NullableStringArg()
    var html: String? by NullableStringArg()
    var title: String? by NullableStringArg()
    var isUnsupportedFeature: Boolean by BooleanArg()
    var isLTITool: Boolean by BooleanArg()
    private var shouldAuthenticate: Boolean by BooleanArg()
    private var externalLTIUrl: String? = null
    private var assignmentLtiUrl: String? = null    //if we're coming from an lti assignment we need the original assignment url, not the sessionless one

    private var shouldRouteInternally = true
    private var shouldLoadUrl = true
    private var sessionAuthJob: Job? = null
    private var shouldCloseFragment = false

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        return if (activity is InternalWebViewActivity) {
            FragmentInteractions.Placement.MASTER
        } else FragmentInteractions.Placement.DETAIL
    }

    override fun title(): String {
        return title ?: canvasContext.name
    }

    protected fun setShouldRouteInternally(shouldRouteInternally: Boolean) {
        this.shouldRouteInternally = shouldRouteInternally
    }

    override fun onPause() {
        super.onPause()
        canvasWebView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        canvasWebView?.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // notify that we have action bar items
        retainInstance = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        canvasWebView?.saveState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_webview, container, false)
        toolbar = rootView.findViewById(R.id.toolbar)

        with(rootView) {
            canvasWebView.settings.loadWithOverviewMode = true

            canvasWebView.canvasWebChromeClientCallback = CanvasWebView.CanvasWebChromeClientCallback { view, newProgress ->
                if(newProgress == 100) {
                    webViewLoading?.setGone()
                }
            }

            // open a new page to view some types of embedded video content
            canvasWebView.addVideoClient(activity)
            canvasWebView.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
                override fun openMediaFromWebView(mime: String, url: String, filename: String) {
                    openMedia(canvasContext, url)
                }

                override fun onPageFinishedCallback(webView: WebView, url: String) {
                    webViewLoading?.setGone()
                }

                override fun onPageStartedCallback(webView: WebView, url: String) {
                    webViewLoading?.setVisible()
                }

                override fun canRouteInternallyDelegate(url: String): Boolean {
                    return shouldRouteInternally && !isUnsupportedFeature && RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, false)
                }

                override fun routeInternallyCallback(url: String) {
                    RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, true)
                }
            }
            canvasWebView?.restoreState(savedInstanceState)
        }

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (shouldLoadUrl && !url.isNullOrBlank()) {
            loadUrl(url!!)
        } else if(shouldLoadUrl && !html.isNullOrBlank()) {
            loadUrl(html!!)
        }

        val hideToolbar = arguments?.getBoolean(InternalWebViewActivity.HIDE_TOOLBAR, false) ?: false
        toolbar?.visibility = if(hideToolbar) View.GONE else View.VISIBLE

        if(isLTITool) {
            toolbar?.let {
                setupToolbarMenu(it, R.menu.menu_internal_webview)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.launchExternalWeb) {
            if(externalLTIUrl.isValid() && !assignmentLtiUrl.isValid()) {
                sessionAuthJob = tryWeave {
                    val result = awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(externalLTIUrl, it) }.sessionUrl
                    launchIntent(result)
                } catch {
                    Toast.makeText(this@InternalWebviewFragment.context, R.string.utils_unableToViewInBrowser, Toast.LENGTH_SHORT).show()
                }
            } else if(assignmentLtiUrl.isValid()) {
                sessionAuthJob = tryWeave {
                    val result = awaitApi<LTITool> { SubmissionManager.getLtiFromAuthenticationUrl(assignmentLtiUrl, it, true) }.url
                    launchIntent(result)
                } catch {
                    Toast.makeText(this@InternalWebviewFragment.context, R.string.utils_unableToViewInBrowser, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchIntent(result: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
        // Make sure we can handle the intent
        if (intent.resolveActivity(this@InternalWebviewFragment.context.packageManager) != null) {
            this@InternalWebviewFragment.startActivity(intent)
        } else {
            Toast.makeText(this@InternalWebviewFragment.context, R.string.utils_unableToViewInBrowser, Toast.LENGTH_SHORT).show()
        }
    }


    override fun applyTheme() {
        toolbar?.title = title()
        toolbar?.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, canvasContext)
    }

    fun setShouldLoadUrl(shouldLoadUrl: Boolean) {
        this.shouldLoadUrl = shouldLoadUrl
    }

    fun loadUrl(targetUrl: String?) {
        if (!html.isNullOrBlank()) {
            loadHtml(html!!)
            return
        }

        if(isLTITool) {
            externalLTIUrl = targetUrl
        }

        url = targetUrl
        if (!url.isNullOrBlank() && isAdded) {
            sessionAuthJob = weave {
                if (ApiPrefs.domain in url!! && shouldAuthenticate) {
                    try {
                        // Get an authenticated session so the user doesn't have to log in
                        url = awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(url, it) }.sessionUrl
                    } catch (e: StatusCallbackError) {}
                }

                if(getIsUnsupportedFeature()) {
                    //Add query param
                    val builder = Uri.parse(url).buildUpon()
                    builder.appendQueryParameter("embedded", "1")
                    url = builder.build().toString()
                }

                canvasWebView?.loadUrl(url, getReferer())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionAuthJob?.cancel()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackStackChangedEvent(event: OnBackStackChangedEvent) {
        event.get { clazz ->
            if (InternalWebviewFragment::class.java.isAssignableFrom(clazz)) {
                canvasWebView.onResume()
            } else {
                canvasWebView.onPause()
            }
        }
    }

    override fun handleBackPressed() = canvasWebView?.handleGoBack() ?: false

    protected fun populateWebView(content: String) = populateWebView(content, null)

    protected fun populateWebView(content: String, title: String?) = canvasWebView?.loadHtml(content, title)

    fun loadHtml(baseUrl: String, data: String, mimeType: String, encoding: String, historyUrl: String?) {
        // BaseURL is set as Referer. Referer needed for some vimeo videos to play
        canvasWebView?.loadDataWithBaseURL(CanvasWebView.getReferrer(), data, mimeType, encoding, historyUrl)
    }

    // BaseURL is set as Referer. Referer needed for some vimeo videos to play
    fun loadHtml(html: String) {
        canvasWebView?.loadDataWithBaseURL(ApiPrefs.fullDomain,
                FileUtils.getAssetsFile(context, "html_wrapper.html").replace("{\$CONTENT$}", html, ignoreCase = false),
                "text/html", "UTF-8", null)
    }

    fun getReferer(): Map<String, String> {
        val extraHeaders = mutableMapOf(Pair("Referer", ApiPrefs.domain))
        return extraHeaders
    }

    fun canGoBack(): Boolean {
        return if (!shouldCloseFragment) {
            canvasWebView?.canGoBack() ?: false
        } else false
    }
    fun goBack() = canvasWebView?.goBack()

    fun getCanvasWebView(): CanvasWebView? {
        return canvasWebView
    }

    fun getCanvasLoading(): ProgressBar? {
        return webViewLoading
    }

    fun getShouldRouteInternally(): Boolean {
        return shouldRouteInternally
    }

    fun getIsUnsupportedFeature(): Boolean {
        return isUnsupportedFeature
    }

    override fun handleIntentExtras(extras: Bundle?) {
        extras?.let {
            url = it.getString(Const.INTERNAL_URL)
            title = it.getString(Const.ACTION_BAR_TITLE)
            isUnsupportedFeature = it.getBoolean(Const.IS_UNSUPPORTED_FEATURE)
            html = it.getString(Const.HTML)
            isLTITool = it.getBoolean(Const.IS_EXTERNAL_TOOL)
            shouldAuthenticate = it.getBoolean(Const.AUTHENTICATE)
            assignmentLtiUrl = it.getString(Const.API_URL)
        }
        super.handleIntentExtras(extras)
    }

    override fun allowBookmarking(): Boolean {
        return false
    }

    companion object {

        fun createDefaultBundle(canvasContext: CanvasContext): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, "https://play.google.com/store/apps/details?id=com.instructure.candroid")
            return extras
        }

        /*
     * Do not use this method if the InternalWebViewFragment has the ActionBar DropDownMenu visable,
     * Otherwise the canvasContext won't be saved and will cause issues with the dropdown navigation
     * -dw
     */
        fun createBundle(url: String, title: String, authenticate: Boolean, html: String): Bundle {
            val extras = ParentFragment.createBundle(CanvasContext.emptyUserContext())
            extras.putString(Const.INTERNAL_URL, url)
            extras.putString(Const.ACTION_BAR_TITLE, title)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            extras.putString(Const.HTML, html)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String?, title: String?, authenticate: Boolean, html: String): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, url)
            extras.putString(Const.ACTION_BAR_TITLE, title)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            extras.putString(Const.HTML, html)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String, title: String, authenticate: Boolean, isUnsupportedFeature: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, url)
            extras.putString(Const.ACTION_BAR_TITLE, title)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            extras.putBoolean(Const.IS_UNSUPPORTED_FEATURE, isUnsupportedFeature)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String, title: String, authenticate: Boolean, isUnsupportedFeature: Boolean, isLTITool: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, url)
            extras.putString(Const.ACTION_BAR_TITLE, title)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            extras.putBoolean(Const.IS_UNSUPPORTED_FEATURE, isUnsupportedFeature)
            extras.putBoolean(Const.IS_EXTERNAL_TOOL, isLTITool)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String, title: String, authenticate: Boolean, isUnsupportedFeature: Boolean, isLTITool: Boolean, ltiUrl: String): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, url)
            extras.putString(Const.ACTION_BAR_TITLE, title)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            extras.putBoolean(Const.IS_UNSUPPORTED_FEATURE, isUnsupportedFeature)
            extras.putBoolean(Const.IS_EXTERNAL_TOOL, isLTITool)
            extras.putString(Const.API_URL, ltiUrl)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String, title: String, authenticate: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, url)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            extras.putString(Const.ACTION_BAR_TITLE, title)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String, authenticate: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, url)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String, authenticate: Boolean, isUnsupportedFeature: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.INTERNAL_URL, url)
            extras.putBoolean(Const.AUTHENTICATE, authenticate)
            extras.putBoolean(Const.IS_UNSUPPORTED_FEATURE, isUnsupportedFeature)
            return extras
        }

        fun createBundleHTML(canvasContext: CanvasContext, html: String): Bundle {
            return createBundle(canvasContext, null, null, false, html)
        }

        fun loadInternalWebView(activity: FragmentActivity?, navigation: Navigation?, bundle: Bundle) {
            if (activity == null || navigation == null) {
                Logger.e("loadInternalWebView could not complete, activity or navigation null")
                return
            }

            navigation.addFragment(FragUtils.getFrag(InternalWebviewFragment::class.java, bundle))
        }
    }
}


