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

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.events.PageUpdatedEvent
import com.instructure.candroid.util.FragUtils
import com.instructure.candroid.util.LockInfoHTMLHelper
import com.instructure.candroid.util.Param
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.PageManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Page
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.pageview.BeforePageView
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.pageview.PageViewUrl
import com.instructure.interactions.FragmentInteractions
import com.instructure.interactions.Navigation
import com.instructure.loginapi.login.dialog.NoInternetConnectionDialog
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.getModuleItemId
import com.instructure.pandautils.utils.setupAsBackButton
import com.instructure.pandautils.views.CanvasWebView
import org.greenrobot.eventbus.Subscribe
import retrofit2.Call
import retrofit2.Response
import java.util.*

@PageView
class PageDetailsFragment : InternalWebviewFragment() {

    // logic
    private var pageName: String? = null

    @PageViewUrl
    @Suppress("unused")
    private fun makePageViewUrl(): String {
        val url = StringBuilder(ApiPrefs.fullDomain)
        page?.let {
            url.append(canvasContext.toAPIString())
            if (!it.isFrontPage) url.append("/pages/$pageName")
            getModuleItemId()?.let { url.append("?module_item_id=$it") }
        }
        return url.toString()
    }

    // asyncTasks
    private var pageCallback: StatusCallback<Page>? = null

    private var page: Page? = null

    override fun getFragmentPlacement() = FragmentInteractions.Placement.DETAIL
    override fun title(): String = page?.title ?: getString(R.string.pages)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShouldLoadUrl(false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpCallBack()

        getPageDetails()
    }

    private fun getPageDetails() {
        if (pageName == null || pageName == Page.FRONT_PAGE_NAME) {
            PageManager.getFrontPage(canvasContext, true, pageCallback!!)
        } else {
            PageManager.getPageDetails(canvasContext, pageName!!, true, pageCallback!!)
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCanvasWebView()?.canvasEmbeddedWebViewCallback = object : CanvasWebView.CanvasEmbeddedWebViewCallback {
            override fun shouldLaunchInternalWebViewFragment(url: String): Boolean {
                return true
            }

            override fun launchInternalWebViewFragment(url: String) {
                InternalWebviewFragment.loadInternalWebView(activity, activity as Navigation, InternalWebviewFragment.createBundle(canvasContext, url, isLTITool))
            }
        }
    }

    override fun applyTheme() {
        toolbar?.let {
            setupToolbarMenu(it, R.menu.menu_page_details)
            it.title = title()
            it.setupAsBackButton(this)
            // Set the edit option false by default
            it.menu.findItem(R.id.menu_edit).isVisible = false
            checkCanEdit()

            ViewStyler.themeToolbar(activity, it, canvasContext)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_edit -> { openEditPage(page!!)}
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getParamForBookmark(): HashMap<String, String> {
        val map = super.getParamForBookmark()
        if (Page.FRONT_PAGE_NAME == pageName) {
            map.put(Param.PAGE_ID, Page.FRONT_PAGE_NAME)
        } else if (!pageName.isNullOrBlank()) {
            map.put(Param.PAGE_ID, pageName!!)
        }
        return map
    }

    override fun allowBookmarking() = true


    private fun openEditPage(page: Page) {
        if (APIHelper.hasNetworkConnection()) {

            if (navigation != null) {
                val bundle = EditPageDetailsFragment.createBundle(page, canvasContext)
                navigation!!.addFragment(
                        FragUtils.getFrag(EditPageDetailsFragment::class.java, bundle))
            }
        } else {
            NoInternetConnectionDialog.show(fragmentManager)
        }
    }

    private fun checkCanEdit() {
        if (page?.editingRoles?.contains("public") == true) {
            toolbar?.menu?.findItem(R.id.menu_edit)?.isVisible = true
        } else if (page?.editingRoles?.contains("student") == true && (canvasContext as Course).isStudent) {
            toolbar?.menu?.findItem(R.id.menu_edit)?.isVisible = true
        } else if (page?.editingRoles?.contains("teacher") == true && (canvasContext as Course).isTeacher) {
            toolbar?.menu?.findItem(R.id.menu_edit)?.isVisible = true
        }
    }

    @BeforePageView
    private fun setPage(page: Page) {
        this.page = page
    }

    private fun setUpCallBack() {
        pageCallback = object : StatusCallback<Page>() {

            override fun onResponse(response: Response<Page>, linkHeaders: LinkHeaders, type: ApiType) {
                if (!apiCheck()) {
                    return
                }

                setPage(response.body()!!)

                if (page?.lockInfo != null) {
                    val lockedMessage = LockInfoHTMLHelper.getLockedInfoHTML(page?.lockInfo, activity, R.string.lockedPageDesc, R.string.lockedAssignmentDescLine2)
                    populateWebView(lockedMessage, getString(R.string.pages))
                    return
                }
                if (page?.body != null && page?.body != "null" && page?.body != "") {
                    //this sets the width to be the device width and makes the images not be bigger than the width of the screen
                    populateWebView(page?.body!!, getString(R.string.pages))

                } else if (page?.body == null || page?.body?.endsWith("") == true) {
                    loadHtml("file:///android_asset/", resources.getString(R.string.noPageFound), "text/html", "utf-8", null)
                }

                toolbar?.title = title()

                checkCanEdit()
            }

            override fun onFail(call: Call<Page>?, error: Throwable, response: Response<*>?) {
                if (response != null && response.code() >= 400 && response.code() < 500 && pageName != null && pageName == Page.FRONT_PAGE_NAME) {

                    var context: String = if (canvasContext.type == CanvasContext.Type.COURSE) {
                        getString(R.string.course)
                    } else {
                        getString(R.string.group)
                    }

                    //We want a complete sentence.
                    context += "."

                    //We want it to be lowercase.
                    context = context.toLowerCase(Locale.getDefault())

                    loadHtml("file:///android_asset/", resources.getString(R.string.noPagesInContext) + " " + context, "text/html", "utf-8", null)
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onUpdatePage(event: PageUpdatedEvent) {
        event.once(javaClass.simpleName) {
            getCanvasWebView()?.clearHistory()
            getPageDetails()
        }
    }

    @BeforePageView
    override fun handleIntentExtras(extras: Bundle?) {
        super.handleIntentExtras(extras)

        pageName = if (urlParams != null) {
            urlParams[Param.PAGE_ID]
        } else {
            extras?.getString(Const.PAGE_NAME)
        }
    }

    companion object {

        fun createBundle(pageName: String?, canvasContext: CanvasContext): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.PAGE_NAME, pageName)
            return extras
        }
    }
}
