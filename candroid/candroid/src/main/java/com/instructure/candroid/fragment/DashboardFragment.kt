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
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.instructure.candroid.R
import com.instructure.candroid.adapter.DashboardRecyclerAdapter
import com.instructure.candroid.decorations.VerticalGridSpacingDecoration
import com.instructure.candroid.dialog.ColorPickerDialog
import com.instructure.candroid.dialog.EditCourseNicknameDialog
import com.instructure.candroid.events.CoreDataFinishedLoading
import com.instructure.candroid.events.ShowGradesToggledEvent
import com.instructure.candroid.interfaces.CourseAdapterToFragmentCallback
import com.instructure.canvasapi2.managers.CourseNicknameManager
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_course_grid.*
import kotlinx.android.synthetic.main.recycler_swipe_refresh_layout.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlinx.android.synthetic.main.fragment_course_grid.fragment_container as mRootView
import kotlinx.android.synthetic.main.recycler_swipe_refresh_layout.listView as mRecyclerView

@PageView
class DashboardFragment : ParentFragment() {

    private var mRecyclerAdapter: DashboardRecyclerAdapter? = null

    private val somethingChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (mRecyclerAdapter != null && intent?.extras?.getBoolean(Const.COURSE_FAVORITES) == true) {
                swipeRefreshLayout?.isRefreshing = true
                mRecyclerAdapter?.refresh()
            }
        }
    }

    override fun getFragmentPlacement() = FragmentInteractions.Placement.MASTER

    override fun title(): String = if (isAdded) getString(R.string.dashboard) else ""

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_course_grid, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mRecyclerAdapter = DashboardRecyclerAdapter(activity, object : CourseAdapterToFragmentCallback {
            override fun onHandleCourseInvitation(course: Course, accepted: Boolean) {
                swipeRefreshLayout?.isRefreshing = true
                mRecyclerAdapter?.refresh()
            }

            override fun onRefreshFinished() {
                swipeRefreshLayout?.isRefreshing = false
            }

            override fun onSeeAllCourses() {
                navigation?.addFragment(createFragment(AllCoursesFragment::class.java, ParentFragment.createBundle(canvasContext)))
            }

            override fun onRemoveAnnouncement(announcement: AccountNotification, position: Int) {
                mRecyclerAdapter?.removeItem(announcement)
            }

            override fun onGroupSelected(group: Group) {
                canvasContext = group
                navigation?.addFragment(CourseBrowserFragment.newInstance(canvasContext))
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
                        mRecyclerAdapter?.addOrUpdateItem(DashboardRecyclerAdapter.ItemType.COURSE_HEADER, course)
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
        setupToolbarMenu(toolbar, R.menu.menu_favorite)
        toolbar.title = title()
        navigation?.attachNavigationDrawer(this, toolbar)
        //Styling done in attachNavigationDrawer
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        configureRecyclerView()
    }

    private fun configureRecyclerView() {
        // Set up GridLayoutManager
        val courseColumns = resources.getInteger(R.integer.course_card_columns)
        val groupColumns = resources.getInteger(R.integer.group_card_columns)
        val totalColumns = courseColumns * groupColumns
        val layoutManager = GridLayoutManager(context, totalColumns, GridLayoutManager.VERTICAL, false)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = mRecyclerView.adapter.getItemViewType(position)
                return when (DashboardRecyclerAdapter.ItemType.values()[viewType]) {
                    DashboardRecyclerAdapter.ItemType.COURSE -> groupColumns
                    DashboardRecyclerAdapter.ItemType.GROUP -> courseColumns
                    else -> totalColumns
                }
            }
        }

        // Add decoration
        mRecyclerView.removeAllItemDecorations()
        mRecyclerView.addItemDecoration(VerticalGridSpacingDecoration(context, layoutManager))

        mRecyclerView.layoutManager = layoutManager
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.setEmptyView(emptyCoursesView)
        mRecyclerView.adapter = mRecyclerAdapter
        swipeRefreshLayout.setOnRefreshListener {
            if (!Utils.isNetworkAvailable(context)) {
                swipeRefreshLayout.isRefreshing = false
            } else {
                mRecyclerAdapter?.refresh()
            }
        }

        // Set up RecyclerView padding
        val padding = resources.getDimensionPixelSize(R.dimen.courseListPadding)
        mRecyclerView.setPaddingRelative(padding, padding, padding, padding)
        mRecyclerView.clipToPadding = false

        emptyCoursesView.onClickAddCourses {
            if (!APIHelper.hasNetworkConnection()) {
                toast(R.string.notAvailableOffline)
            } else {
                navigation?.addFragment(EditFavoritesFragment())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_favorite, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.selectFavorites) {
            if (!APIHelper.hasNetworkConnection()) {
                toast(R.string.notAvailableOffline)
                return true
            }
            navigation?.addFragment(EditFavoritesFragment())
        }
        return super.onOptionsItemSelected(item)
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

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe
    fun onCoreDataLoaded(event: CoreDataFinishedLoading) {
        applyTheme()
    }

    override fun onDestroy() {
        if (mRecyclerAdapter != null) mRecyclerAdapter?.cancel()
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun newInstance(canvasContext: CanvasContext) = DashboardFragment().apply {
            this.canvasContext = canvasContext
        }
    }
}
