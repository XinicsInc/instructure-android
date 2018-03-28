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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.instructure.candroid.R
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.ScheduleItem
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.NullableParcelableArg
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.setupAsBackButton
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.views.CanvasWebView
import kotlinx.android.synthetic.main.fragment_syllabus.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Response

@PageView(url = "{canvasContext}/assignments/syllabus")
class SyllabusFragment : ParentFragment() {

    // model variables
    private var syllabus by NullableParcelableArg<ScheduleItem>()

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        return FragmentInteractions.Placement.DETAIL
    }

    override fun title(): String {
        return if(syllabus != null && syllabus!!.title.isNotBlank()) syllabus!!.title
        else getString(R.string.syllabus)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_syllabus, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        canvasWebView.addVideoClient(activity)
        canvasWebView.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
            override fun openMediaFromWebView(mime: String, url: String, filename: String) {
                openMedia(mime, url, filename)
            }

            override fun onPageStartedCallback(webView: WebView, url: String) {}
            override fun onPageFinishedCallback(webView: WebView, url: String) {}

            override fun canRouteInternallyDelegate(url: String): Boolean {
                return RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, false)
            }

            override fun routeInternallyCallback(url: String) {
                RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, true)
            }
        }

        canvasWebView.canvasEmbeddedWebViewCallback = object : CanvasWebView.CanvasEmbeddedWebViewCallback {
            override fun shouldLaunchInternalWebViewFragment(url: String): Boolean {
                return true
            }

            override fun launchInternalWebViewFragment(url: String) {
                InternalWebviewFragment.loadInternalWebView(activity, navigation, InternalWebviewFragment.createBundle(canvasContext, url, false))
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (syllabus == null || syllabus!!.description == null) {
            CourseManager.getCourseWithSyllabus(canvasContext.id, syllabusCallback, true)
        } else {
            populateViews()
        }
    }

    override fun applyTheme() {
        toolbar.title = title()
        setupToolbarMenu(toolbar)
        toolbar.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, canvasContext)
    }


    override fun onPause() {
        super.onPause()
        canvasWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        canvasWebView.onResume()
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
            if (clazz?.isAssignableFrom(SyllabusFragment::class.java) == true) {
                canvasWebView.onResume()
            } else {
                canvasWebView.onPause()
            }
        }
    }

    override fun handleBackPressed(): Boolean {
        return canvasWebView.handleGoBack()
    }

    internal fun populateViews() {
        if (activity == null || syllabus?.itemType != ScheduleItem.Type.TYPE_SYLLABUS) {
            return
        }

        toolbar.title = title()
        canvasWebView.formatHTML(syllabus!!.description, syllabus!!.title)
    }

    private val syllabusCallback = object : StatusCallback<Course>() {
        override fun onResponse(response: Response<Course>, linkHeaders: LinkHeaders, type: ApiType) {
            if (!apiCheck()) {
                return
            }
            val course = response.body()
            if (course != null && !course.syllabusBody.isNullOrEmpty()) {
                emptyPandaView.visibility = View.GONE
                syllabus = ScheduleItem()
                syllabus!!.itemType = ScheduleItem.Type.TYPE_SYLLABUS
                syllabus!!.title = course.name
                syllabus!!.description = course.syllabusBody
                populateViews()
            } else {
                //No syllabus
                emptyPandaView.emptyViewText(R.string.syllabusMissing)
                emptyPandaView.setListEmpty()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syllabusCallback.cancel()
    }

    override fun allowBookmarking(): Boolean {
        return false
    }

    companion object {
        fun createBundle(course: Course, syllabus: ScheduleItem): Bundle {
            val bundle = ParentFragment.createBundle(course)
            bundle.putParcelable(Const.ADD_SYLLABUS, syllabus)
            bundle.putParcelable(Const.SYLLABUS, syllabus)
            return bundle
        }
    }
}
