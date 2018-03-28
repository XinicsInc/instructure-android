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

package com.instructure.candroid.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.instructure.candroid.R
import com.instructure.candroid.adapter.AllCoursesRecyclerAdapter
import com.instructure.candroid.dialog.ColorPickerDialog
import com.instructure.candroid.dialog.EditCourseNicknameDialog
import com.instructure.candroid.events.ShowGradesToggledEvent
import com.instructure.candroid.interfaces.CourseAdapterToFragmentCallback
import com.instructure.canvasapi2.managers.CourseNicknameManager
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_all_courses.*
import kotlinx.android.synthetic.main.recycler_swipe_refresh_layout.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlinx.android.synthetic.main.fragment_all_courses.fragment_container as mRootView
import kotlinx.android.synthetic.main.recycler_swipe_refresh_layout.listView as mRecyclerView

@PageView(url = "courses")
class AllCoursesFragment : ParentFragment() {

    private var mRecyclerAdapter: AllCoursesRecyclerAdapter? = null

    private val somethingChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (mRecyclerAdapter != null && intent?.extras?.getBoolean(Const.COURSE_FAVORITES) == true) {
                swipeRefreshLayout?.isRefreshing = true
                mRecyclerAdapter?.refresh()
            }
        }
    }

    override fun getFragmentPlacement() = FragmentInteractions.Placement.MASTER

    override fun title(): String = if (isAdded) getString(R.string.allCourses) else ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_all_courses, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mRecyclerAdapter = AllCoursesRecyclerAdapter(activity, object : CourseAdapterToFragmentCallback {
            override fun onRemoveAnnouncement(announcement: AccountNotification, position: Int) = Unit
            override fun onHandleCourseInvitation(course: Course, accepted: Boolean) = Unit
            override fun onSeeAllCourses() = Unit
            override fun onGroupSelected(group: Group) = Unit

            override fun onRefreshFinished() {
                swipeRefreshLayout?.isRefreshing = false
            }

            override fun onCourseSelected(course: Course) {
                canvasContext = course
                navigation?.addFragment(CourseBrowserFragment.newInstance(canvasContext))
            }

            @Suppress("EXPERIMENTAL_FEATURE_WARNING")
            override fun onEditCourseNickname(course: Course) {
                EditCourseNicknameDialog.getInstance(fragmentManager, course, { s ->
                    tryWeave {
                        val response = awaitApi<CourseNickname> { CourseNicknameManager.setCourseNickname(course.id, s, it) }
                        if (response.nickname == null) {
                            course.name = response.name
                            course.originalName = null
                        } else {
                            course.name = response.nickname
                            course.originalName = response.name
                        }
                        mRecyclerAdapter?.add(course)
                    } catch {
                        toast(R.string.courseNicknameError)
                    }
                }).show(fragmentManager, EditCourseNicknameDialog::class.java.simpleName)
            }

            @Suppress("EXPERIMENTAL_FEATURE_WARNING")
            override fun onPickCourseColor(course: Course) {
                ColorPickerDialog.newInstance(fragmentManager, course, { color ->
                    tryWeave {
                        awaitApi<CanvasColor> { UserManager.setColors(it, course.contextId, color) }
                        ColorKeeper.addToCache(course.contextId, color)
                        mRecyclerAdapter?.notifyDataSetChanged()
                    } catch {
                        toast(R.string.colorPickerError)
                    }
                }).show(fragmentManager, ColorPickerDialog::class.java.simpleName)
            }
        })

        configureRecyclerView()
        mRecyclerView.isSelectionEnabled = false
    }

    override fun applyTheme() {
        toolbar.title = getString(R.string.allCourses)
        toolbar.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, ThemePrefs.primaryColor, ThemePrefs.primaryTextColor)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        configureRecyclerView()
    }

    private fun configureRecyclerView() {
        val courseColumns = resources.getInteger(R.integer.course_card_columns)
        configureRecyclerViewAsGrid(view, mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView, R.string.no_courses_available, courseColumns)
    }

    override fun allowBookmarking() = false

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(context).registerReceiver(somethingChangedReceiver, IntentFilter(Const.COURSE_THING_CHANGED))
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(somethingChangedReceiver)
        EventBus.getDefault().unregister(this)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe
    fun onShowGradesToggled(event: ShowGradesToggledEvent) {
        mRecyclerAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        if (mRecyclerAdapter != null) mRecyclerAdapter?.cancel()
        super.onDestroy()
    }
}
