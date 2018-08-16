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

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Page
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.CanvasWebView
import com.instructure.teacher.R
import com.instructure.teacher.activities.InternalWebViewActivity
import com.instructure.teacher.dialog.NoInternetConnectionDialog
import com.instructure.teacher.events.PageDeletedEvent
import com.instructure.teacher.events.PageUpdatedEvent
import com.instructure.teacher.factory.PageDetailsPresenterFactory
import com.instructure.interactions.FullScreenInteractions
import com.instructure.interactions.Identity
import com.instructure.interactions.MasterDetailInteractions
import com.instructure.teacher.presenters.PageDetailsPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.isTablet
import com.instructure.teacher.utils.setupBackButtonWithExpandCollapseAndBack
import com.instructure.teacher.utils.setupMenu
import com.instructure.teacher.utils.updateToolbarExpandCollapseIcon
import com.instructure.teacher.viewinterface.PageDetailsView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_page_details.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class PageDetailsFragment : BasePresenterFragment<
        PageDetailsPresenter,
        PageDetailsView>(),
        PageDetailsView, Identity {

    private var mCanvasContext: CanvasContext by ParcelableArg(default = Course())
    private var mPage: Page by ParcelableArg(default = Page())
    private var mPageId: String by StringArg()

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onRefreshFinished() {
        loading.setGone()
    }

    override fun onRefreshStarted() {
        loading.setVisible()
    }

    override fun onReadySetGo(presenter: PageDetailsPresenter?) {

        if (mPage.isFrontPage) {
            presenter?.getFrontPage(mCanvasContext, true)
        } else if (!mPageId.isBlank()) {
            presenter?.getPage(mPageId, mCanvasContext, true)
        } else {
            presenter?.getPage(mPage.url ?: "", mCanvasContext, true)
        }
        setupToolbar()

        canvasWebView.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
            override fun openMediaFromWebView(mime: String, url: String, filename: String) {
                RouteMatcher.openMedia(activity, url)
            }

            override fun onPageFinishedCallback(webView: WebView, url: String) {
                loading.setGone()
            }

            override fun onPageStartedCallback(webView: WebView, url: String) {
                loading.setVisible()
            }

            override fun canRouteInternallyDelegate(url: String): Boolean = RouteMatcher.canRouteInternally(activity, url, ApiPrefs.domain, false)

            override fun routeInternallyCallback(url: String) {
                RouteMatcher.canRouteInternally(activity, url, ApiPrefs.domain, true)
            }
        }

        canvasWebView.webChromeClient = (object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress >= 100) {
                    loading?.setGone()
                }
            }
        })

        canvasWebView.canvasEmbeddedWebViewCallback = object : CanvasWebView.CanvasEmbeddedWebViewCallback {
            override fun launchInternalWebViewFragment(url: String) = activity.startActivity(InternalWebViewActivity.createIntent(activity, url, "", true))
            override fun shouldLaunchInternalWebViewFragment(url: String): Boolean = !RouteMatcher.canRouteInternally(activity, url, ApiPrefs.domain, false)

        }

        EventBus.getDefault().getStickyEvent(PageDeletedEvent::class.java)?.once(javaClass.simpleName + ".onResume()") {
            if (it.pageId == mPage.pageId) {
                if (activity is MasterDetailInteractions) {
                    (activity as MasterDetailInteractions).popFragment(mCanvasContext)
                } else if(activity is FullScreenInteractions) {
                    activity.finish()
                }
            }
        }
    }

    override fun getPresenterFactory(): PresenterFactory<PageDetailsPresenter> = PageDetailsPresenterFactory(mCanvasContext, mPage)
    override fun onPresenterPrepared(presenter: PageDetailsPresenter?) = Unit

    override fun layoutResId() = R.layout.fragment_page_details

    override val identity: Long? get() = mPage.pageId
    override val skipCheck: Boolean get() = false

    override fun populatePageDetails(page: Page) {
        mPage = page
        canvasWebView.loadHtml(page.body, page.title)
        setupToolbar()
    }

    override fun onError(stringId: Int) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show()
    }

    private fun setupToolbar() {
        toolbar.setupMenu(R.menu.menu_page_details) { openEditPage(mPage) }

        toolbar.setupBackButtonWithExpandCollapseAndBack(this) {
            toolbar.updateToolbarExpandCollapseIcon(this)
            ViewStyler.themeToolbar(activity, toolbar, mCanvasContext.color, Color.WHITE)
            (activity as MasterDetailInteractions).toggleExpandCollapse()
        }

        toolbar.title = mPage.title
        if (!isTablet) {
            toolbar.subtitle = mCanvasContext.name
        }
        ViewStyler.themeToolbar(activity, toolbar, mCanvasContext.color, Color.WHITE)
    }

    private fun openEditPage(page: Page) {
        if(APIHelper.hasNetworkConnection()) {
            val args = CreateOrEditPageDetailsFragment.newInstanceEdit(mCanvasContext, page).arguments
            RouteMatcher.route(context, Route(CreateOrEditPageDetailsFragment::class.java, mCanvasContext, args))
        } else {
            NoInternetConnectionDialog.show(fragmentManager)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onPageUpdated(event: PageUpdatedEvent) {
        event.once(javaClass.simpleName) {
            // need to set a flag here. Because we use the event bus in the fragment instead of the presenter for unit testing purposes,
            // when we come back to this fragment it will go through the life cycle events again and the cached data will immediately
            // overwrite the data from the network if we refresh the presenter from here.
            event.get { mPage = it }
        }
    }

    companion object {
        const val PAGE = "pageDetailsPage"

        const val PAGE_ID = "pageDetailsId"

        @JvmStatic
        fun makeBundle(page: Page): Bundle = Bundle().apply { putParcelable(PageDetailsFragment.PAGE, page) }

        @JvmStatic
        fun makeBundle(pageId: String): Bundle = Bundle().apply { putString(PageDetailsFragment.PAGE_ID, pageId) }


        @JvmStatic
        fun newInstance(canvasContext: CanvasContext, args: Bundle) = PageDetailsFragment().apply {
            with(args) {
                if (containsKey(PAGE)) {
                    mPage = getParcelable(PAGE)
                }
                if (containsKey(PAGE_ID)) {
                    mPageId = getString(PAGE_ID)
                }
            }

            mCanvasContext = canvasContext
        }
    }
}
