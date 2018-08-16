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

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import com.instructure.canvasapi2.managers.OAuthManager
import com.instructure.canvasapi2.models.AuthenticatedSession
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.FileUtils
import com.instructure.canvasapi2.utils.isValid
import com.instructure.canvasapi2.utils.validOrNull
import com.instructure.canvasapi2.utils.weave.StatusCallbackError
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.pandautils.fragments.BaseFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.CanvasWebView
import com.instructure.teacher.R
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.isTablet
import com.instructure.teacher.utils.setupCloseButton
import com.instructure.teacher.utils.setupMenu
import kotlinx.android.synthetic.main.fragment_internal_webview.*
import kotlinx.coroutines.experimental.Job

open class InternalWebViewFragment : BaseFragment() {

    var url: String by StringArg()
    var html: String by StringArg()
    var title: String by StringArg()
    var darkToolbar: Boolean by BooleanArg()

    private var shouldRouteInternally = true
    private var shouldLoadUrl = true
    private var mSessionAuthJob: Job? = null
    private var shouldCloseFragment = false

    override fun layoutResId() = R.layout.fragment_internal_webview

    protected fun setShouldRouteInternally(shouldRouteInternally: Boolean) {
        this.shouldRouteInternally = shouldRouteInternally
    }

    override fun onPause() {
        super.onPause()
        canvasWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        canvasWebView.onResume()
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

    override fun onCreateView(view: View?) = Unit

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) = super.onViewCreated(view, savedInstanceState)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val courseId: String? = RouteMatcher.getCourseIdFromUrl(url)
        var courseColor = -1
        courseId?.let {
            courseColor = ColorKeeper.getOrGenerateColor("course_" + courseId)
        }

        toolbar.title = title.validOrNull() ?: url

        // if we are displaying html we don't want them to view the url in a different browser
        if(!html.isValid()) {
            toolbar.setupMenu(R.menu.menu_internal_webview) {
                val browserIntent = Intent("android.intent.action.VIEW").apply {
                    data = Uri.parse(url)
                }
                if (browserIntent.resolveActivity(activity.packageManager) != null) {
                    startActivity(browserIntent)
                } else {
                    toast(R.string.no_apps)
                }
            }
        }

        setupToolbar(courseColor)

        canvasWebView.settings.loadWithOverviewMode = true
        canvasWebView.settings.displayZoomControls = false
        canvasWebView.settings.setSupportZoom(true)

        canvasWebView.canvasWebChromeClientCallback = object : CanvasWebView.CanvasWebChromeClientCallback {
            override fun onProgressChangedCallback(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    loading?.setGone()
                }
            }
        }

        canvasWebView.addVideoClient(activity)

        canvasWebView.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
            override fun openMediaFromWebView(mime: String, url: String, filename: String) {
                RouteMatcher.openMedia(activity, url)
            }

            override fun onPageFinishedCallback(webView: WebView, url: String) {
                loading?.setGone()
            }

            override fun onPageStartedCallback(webView: WebView, url: String) {
                loading?.setVisible()
            }

            override fun canRouteInternallyDelegate(url: String): Boolean = shouldRouteInternally && RouteMatcher.canRouteInternally(activity, url, ApiPrefs.domain, false)

            override fun routeInternallyCallback(url: String) {
                RouteMatcher.canRouteInternally(activity, url, ApiPrefs.domain, true)
            }
        }

        if (shouldLoadUrl) {
            loadUrl(url)
        }
    }

    open fun setupToolbar(courseColor: Int) {
        toolbar.setupCloseButton {
            shouldCloseFragment = true
            activity?.onBackPressed()
        }

        if(darkToolbar) {
            if (courseColor != -1) {
                // Use course colors for toolbar
                ViewStyler.themeToolbar(activity, toolbar, courseColor, Color.WHITE)
            } else {
                // Use institution colors for toolbar
                ViewStyler.themeToolbar(activity, toolbar, ThemePrefs.primaryColor, ThemePrefs.primaryTextColor)
            }
        } else {
            ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Logic
    ///////////////////////////////////////////////////////////////////////////

    fun isShouldLoadUrl(): Boolean =  shouldLoadUrl

    fun setShouldLoadUrl(shouldLoadUrl: Boolean) {
        this.shouldLoadUrl = shouldLoadUrl
    }

    fun loadUrl(targetUrl: String) {
        if (html.isNotEmpty()) {
            loadHtml(html)
            return
        }

        url = targetUrl
        if (url.isNotEmpty() && isAdded) {
            mSessionAuthJob = weave {
                if (ApiPrefs.domain in url) {
                    try {
                        // Get an authenticated session so the user doesn't have to log in
                        url = awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(url, it) }.sessionUrl
                    } catch (e: StatusCallbackError) {
                    }
                }
            }
            canvasWebView.loadUrl(url, getReferer())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSessionAuthJob?.cancel()
    }

    fun populateWebView(content: String)= populateWebView(content, null)

    fun populateWebView(content: String, title: String?) = canvasWebView.loadHtml(content, title)

    fun loadHtml(baseUrl: String, data: String, mimeType: String, encoding: String, historyUrl: String) {
        // BaseURL is set as Referer. Referer needed for some vimeo videos to play
        canvasWebView.loadDataWithBaseURL(CanvasWebView.getReferrer(), data, mimeType, encoding, historyUrl)
    }

    // BaseURL is set as Referer. Referer needed for some vimeo videos to play
    fun loadHtml(html: String) = canvasWebView.loadDataWithBaseURL(ApiPrefs.fullDomain,
            FileUtils.getAssetsFile(context, "html_wrapper.html").replace("{\$CONTENT$}", html, ignoreCase = false),
            "text/html", "UTF-8", null)

    fun getReferer(): Map<String, String> {
        val extraHeaders = mutableMapOf(Pair("Referer", ApiPrefs.domain))
        return extraHeaders
    }

    fun canGoBack() = if (!shouldCloseFragment) canvasWebView.canGoBack() else false
    fun goBack() = canvasWebView.goBack()

    companion object {
        const val URL = "url"
        const val TITLE = "title"
        const val HTML = "html"
        const val DARK_TOOLBAR = "darkToolbar"

        @JvmStatic
        fun newInstance(url: String) = InternalWebViewFragment().apply {
            this.url = url
        }

        @JvmStatic
        fun newInstance(url: String, html: String) = InternalWebViewFragment().apply {
            this.url = url
            this.html = html
        }

        @JvmStatic
        fun newInstance(url: String, html: String, title: String) = InternalWebViewFragment().apply {
            this.url = url
            this.html = html
            this.title = title
        }

        @JvmStatic
        fun newInstance(args: Bundle) = InternalWebViewFragment().apply {
            url = args.getString(URL)
            title = args.getString(TITLE)
            darkToolbar = args.getBoolean(DARK_TOOLBAR)
        }

        @JvmStatic
        @JvmOverloads
        fun makeBundle(url: String, title: String, darkToolbar: Boolean = false, html: String = "", toolbarColor: Int = 0): Bundle {
            val args = Bundle()
            args.putString(URL, url)
            args.putString(TITLE, title)
            args.putString(HTML, html)
            args.putBoolean(DARK_TOOLBAR, darkToolbar)
            return args
        }
    }
}
