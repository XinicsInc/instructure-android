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

import android.animation.ObjectAnimator
import android.graphics.Color
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Tab
import com.instructure.pandautils.fragments.BaseSyncFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.BuildConfig
import com.instructure.teacher.R
import com.instructure.teacher.adapters.CourseBrowserAdapter
import com.instructure.teacher.events.CourseUpdatedEvent
import com.instructure.teacher.factory.CourseBrowserPresenterFactory
import com.instructure.teacher.holders.CourseBrowserViewHolder
import com.instructure.teacher.presenters.CourseBrowserPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.view.CourseBrowserHeaderView
import com.instructure.teacher.viewinterface.CourseBrowserView
import kotlinx.android.synthetic.main.fragment_course_browser.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class CourseBrowserFragment : BaseSyncFragment<
        Tab,
        CourseBrowserPresenter,
        CourseBrowserView,
        CourseBrowserViewHolder,
        CourseBrowserAdapter>(),
        CourseBrowserView, AppBarLayout.OnOffsetChangedListener {

    private var mCourse: Course by ParcelableArg(Course())

    private val mCourseBrowserHeader by lazy { mRootView.findViewById<CourseBrowserHeaderView>(R.id.courseBrowserHeader) }

    companion object {
        @JvmStatic
        fun newInstance(course: Course) = CourseBrowserFragment().apply {
            mCourse = course
        }
    }

    lateinit private var mRecyclerView: RecyclerView

    override fun layoutResId(): Int = R.layout.fragment_course_browser

    override fun getList() = presenter.data
    override fun getRecyclerView(): RecyclerView = courseBrowserRecyclerView
    override fun withPagination() = false
    override fun getPresenterFactory() = CourseBrowserPresenterFactory(mCourse) { tab, attendanceId ->
        //Filter for white-list supported features
        //TODO: support other things like it.isHidden
        when(tab.tabId) {
            Tab.ASSIGNMENTS_ID -> true
            Tab.QUIZZES_ID -> BuildConfig.POINT_THREE
            Tab.DISCUSSIONS_ID -> BuildConfig.POINT_FIVE
            Tab.ANNOUNCEMENTS_ID -> BuildConfig.POINT_FIVE
            Tab.PEOPLE_ID -> true
            Tab.FILES_ID -> true
            Tab.PAGES_ID -> true
            else -> {
                if(attendanceId != 0 && tab.tabId.endsWith(attendanceId.toString())) {
                    TeacherPrefs.attendanceExternalToolId = tab.tabId
                }
                tab.type == Tab.TYPE_EXTERNAL
            }
        }
    }

    override fun onCreateView(view: View?) = Unit
    override fun onPresenterPrepared(presenter: CourseBrowserPresenter?) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.courseBrowserRecyclerView, R.id.emptyView, getString(R.string.no_items_to_display_short))
        appBarLayout.addOnOffsetChangedListener(this)
        collapsingToolbarLayout.isTitleEnabled = false
    }

    override fun onReadySetGo(presenter: CourseBrowserPresenter) {
        if (recyclerView.adapter == null) {
            mRecyclerView.adapter = adapter
        }
        presenter.loadData(false)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        courseImage.setCourseImage(presenter.course, presenter.course.color)
        courseBrowserTitle.text = presenter.course.name
        courseBrowserSubtitle.text = presenter.course.term?.name ?: ""
        mCourseBrowserHeader.setTitleAndSubtitle(presenter.course.name, presenter.course.term?.name ?: "")
        setupToolbar()
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onCourseUpdated(event: CourseUpdatedEvent) {
        event.once(javaClass.simpleName) { course ->
            if (course.id == presenter.course.id) presenter.course.apply {
                name = course.name
                originalName = course.originalName
                homePage = course.homePage
            }
        }
    }

    private fun setupToolbar() {
        toolbar.setupBackButton(this)
        toolbar.setupMenu(R.menu.menu_course_browser, menuItemCallback)
        ViewStyler.colorToolbarIconsAndText(activity, toolbar, Color.WHITE)
        ViewStyler.setStatusBarDark(activity, presenter.course.color)

        collapsingToolbarLayout.setContentScrimColor(presenter.course.color)

        if(isTablet) {
            appBarLayout.setExpanded(false, false)
            appBarLayout.isActivated = false
            val layoutParams = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            (layoutParams.behavior as DisableableAppBarLayoutBehavior).isEnabled = false
        }

        courseBrowserTitle.requestAccessibilityFocus(600)
    }

    val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menu_course_browser_settings -> {
                RouteMatcher.route(context, Route(CourseSettingsFragment::class.java, presenter.course))
            }
        }
    }

    override fun getAdapter(): CourseBrowserAdapter {
        if (mAdapter == null) {
            mAdapter = CourseBrowserAdapter(activity, presenter, presenter.course.color) { tab ->
                when (tab.tabId) {
                    Tab.ASSIGNMENTS_ID -> RouteMatcher.route(context, Route(AssignmentListFragment::class.java, presenter.course))
                    Tab.QUIZZES_ID -> RouteMatcher.route(context, Route(QuizListFragment::class.java, presenter.course))
                    Tab.DISCUSSIONS_ID -> RouteMatcher.route(context, Route(DiscussionsListFragment::class.java, presenter.course))
                    Tab.ANNOUNCEMENTS_ID -> RouteMatcher.route(context, Route(AnnouncementListFragment::class.java, presenter.course))
                    Tab.PEOPLE_ID -> RouteMatcher.route(context, Route(PeopleListFragment::class.java, presenter.course))
                    Tab.FILES_ID -> {
                        val args = FileListFragment.makeBundle(presenter.course)
                        RouteMatcher.route(context, Route(FileListFragment::class.java, presenter.course, args))
                    }
                    Tab.PAGES_ID -> RouteMatcher.route(context, Route(PageListFragment::class.java, presenter.course))
                    else -> {
                        if(tab.type == Tab.TYPE_EXTERNAL) {
                            // if the user is a designer we don't want to let them look at LTI tools (like attendance)
                            if (presenter.course.isDesigner) {
                                toast(R.string.errorIsDesigner)
                                return@CourseBrowserAdapter
                            }
                            val attendanceExternalToolId = TeacherPrefs.attendanceExternalToolId
                            if(attendanceExternalToolId.isNotBlank() && attendanceExternalToolId == tab.tabId) {
                                val args = AttendanceListFragment.makeBundle(tab)
                                RouteMatcher.route(context, Route(AttendanceListFragment::class.java, presenter.course, args))
                            } else {
                                val args = LTIWebViewFragment.makeLTIBundle(tab)
                                RouteMatcher.route(context, Route(LTIWebViewFragment::class.java, presenter.course, args))
                            }
                        }
                    }
                }
            }
        }
        return mAdapter
    }

    override fun onRefreshFinished() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onRefreshStarted() = emptyView.setLoading()
    override fun checkIfEmpty() = RecyclerViewUtils.checkIfEmpty(emptyView, courseBrowserRecyclerView,
            swipeRefreshLayout, adapter, presenter.isEmpty)

    /**
     * Manages state of titles & subtitles when users scrolls
     */
    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {

        val percentage = Math.abs(verticalOffset).div(appBarLayout?.totalScrollRange?.toFloat() ?: 1F)

        if(percentage <= 0.3F) {
            val toolbarAnimation = ObjectAnimator.ofFloat(mCourseBrowserHeader, View.ALPHA, mCourseBrowserHeader.alpha, 0F)
            val titleAnimation = ObjectAnimator.ofFloat(courseBrowserTitle, View.ALPHA, courseBrowserTitle.alpha, 1F)
            val subtitleAnimation = ObjectAnimator.ofFloat(courseBrowserSubtitle, View.ALPHA, courseBrowserSubtitle.alpha, 0.8F)

            toolbarAnimation.setAutoCancel(true)
            titleAnimation.setAutoCancel(true)
            subtitleAnimation.setAutoCancel(true)

            toolbarAnimation.target = mCourseBrowserHeader
            titleAnimation.target = courseBrowserTitle
            subtitleAnimation.target = courseBrowserSubtitle

            toolbarAnimation.duration = 200
            titleAnimation.duration = 320
            subtitleAnimation.duration = 320

            toolbarAnimation.start()
            titleAnimation.start()
            subtitleAnimation.start()

        } else if(percentage > 0.7F) {
            val toolbarAnimation = ObjectAnimator.ofFloat(mCourseBrowserHeader, View.ALPHA, mCourseBrowserHeader.alpha, 1F)
            val titleAnimation = ObjectAnimator.ofFloat(courseBrowserTitle, View.ALPHA, courseBrowserTitle.alpha, 0F)
            val subtitleAnimation = ObjectAnimator.ofFloat(courseBrowserSubtitle, View.ALPHA, courseBrowserSubtitle.alpha, 0F)

            toolbarAnimation.setAutoCancel(true)
            titleAnimation.setAutoCancel(true)
            subtitleAnimation.setAutoCancel(true)

            toolbarAnimation.target = mCourseBrowserHeader
            titleAnimation.target = courseBrowserTitle
            subtitleAnimation.target = courseBrowserSubtitle

            toolbarAnimation.duration = 200
            titleAnimation.duration = 200
            subtitleAnimation.duration = 200

            toolbarAnimation.start()
            titleAnimation.start()
            subtitleAnimation.start()
        }
    }
}
