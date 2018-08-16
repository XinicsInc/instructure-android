/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.fragment

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.interactions.Navigation
import com.instructure.candroid.util.FragUtils
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.Logger
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.PermissionUtils
import com.instructure.pandautils.views.CanvasWebView
import org.apache.commons.text.StringEscapeUtils

class ArcWebviewFragment : InternalWebviewFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShouldRouteInternally(false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCanvasWebView()?.addJavascriptInterface(JSInterface(), "HtmlViewer")

        getCanvasWebView()?.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
            override fun openMediaFromWebView(mime: String, url: String, filename: String) {
                openMedia(mime, url, filename)
            }

            override fun onPageFinishedCallback(webView: WebView, url: String) {
                getCanvasLoading()?.visibility = View.GONE

                //check for a successful arc submission
                if (url.contains("success/external_tool_dialog")) {

                    webView.loadUrl("javascript:HtmlViewer.showHTML" + "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');")
                }
            }

            override fun onPageStartedCallback(webView: WebView, url: String) {
                getCanvasLoading()?.visibility = View.VISIBLE
            }

            override fun canRouteInternallyDelegate(url: String): Boolean {
                return getShouldRouteInternally() && !getIsUnsupportedFeature() && RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, false)
            }

            override fun routeInternallyCallback(url: String) {
                RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, true)
            }
        }

        getCanvasWebView()?.setCanvasWebChromeClientShowFilePickerCallback(object : CanvasWebView.VideoPickerCallback {
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

    override fun handleBackPressed(): Boolean {
        if(canGoBack()) {
            //This prevents a silly bug where the arc webview cannot go back far enough to pop it's fragment.
            val webBackForwardList = getCanvasWebView()?.copyBackForwardList()
            val historyUrl = webBackForwardList?.getItemAtIndex(webBackForwardList.currentIndex - 1)?.url
            if(historyUrl != null && historyUrl.contains("external_tools/") && historyUrl.contains("resource_selection")) {
                navigation?.popCurrentFragment()
                return true
            }
        }
        return super.handleBackPressed()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun requestFilePermissions() {
        requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA), PermissionUtils.PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (PermissionUtils.allPermissionsGrantedResultSummary(grantResults)) {
            getCanvasWebView()?.clearPickerCallback()
            Toast.makeText(context, R.string.pleaseTryAgain, Toast.LENGTH_SHORT).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    internal inner class JSInterface {

        @Suppress("unused")
        @JavascriptInterface
        fun showHTML(html: String) {

            val mark = "@id\":\""
            val index = html.indexOf(mark)
            if (index != -1) {
                val endIndex = html.indexOf(",", index)
                var url = html.substring(index + mark.length, endIndex - 1)
                url = StringEscapeUtils.unescapeJava(url)

                val intent = Intent(Const.ARC_SUBMISSION)
                val extras = Bundle()
                extras.putString(Const.URL, url)

                intent.putExtras(extras)
                //let the add submission fragment know that we have an arc submission
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                //close this page
                navigation?.popCurrentFragment()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((getCanvasWebView()?.handleOnActivityResult(requestCode, resultCode, data)) != true) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {

        @JvmStatic
        fun loadInternalWebView(activity: FragmentActivity?, navigation: Navigation?, bundle: Bundle) {
            if (activity == null || navigation == null) {
                Logger.e("loadInternalWebView could not complete, activity or navigation null")
                return
            }

            navigation.addFragment(FragUtils.getFrag(ArcWebviewFragment::class.java, bundle))
        }
    }
}
