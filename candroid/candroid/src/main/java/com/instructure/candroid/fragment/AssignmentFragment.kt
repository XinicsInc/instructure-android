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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.activity.BaseRouterActivity
import com.instructure.candroid.util.Param
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.AssignmentManager
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.StreamItem
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.ReflectField
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_assignment.*
import retrofit2.Call
import retrofit2.Response
import java.lang.ref.WeakReference
import java.util.*

class AssignmentFragment : ParentFragment(), SubmissionDetailsFragment.SubmissionDetailsFragmentCallback {

    private var currentTab = ASSIGNMENT_TAB_DETAILS
    private var assignment by NullableParcelableArg<Assignment>()
    private var fragmentPagerAdapter: FragmentPagerDetailAdapter? = null
    private var assignmentId: Long? = null
    private var message: String? = null

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        return FragmentInteractions.Placement.DETAIL
    }

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            viewPager.setCurrentItem(tab.position, true)
        }
        override fun onTabUnselected(tab: TabLayout.Tab) {}
        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    private val assignmentDetailsFragment: AssignmentDetailsFragment?
        get() = if (fragmentPagerAdapter == null) {
            null
        } else fragmentPagerAdapter!!.getRegisteredFragment(ASSIGNMENT_TAB_DETAILS) as AssignmentDetailsFragment?

    val submissionDetailsFragment: SubmissionDetailsFragment?
        get() = if (fragmentPagerAdapter == null) {
            null
        } else fragmentPagerAdapter!!.getRegisteredFragment(ASSIGNMENT_TAB_SUBMISSION) as SubmissionDetailsFragment?

    val rubricFragment: RubricFragment?
        get() = if (fragmentPagerAdapter == null) {
            null
        } else fragmentPagerAdapter!!.getRegisteredFragment(ASSIGNMENT_TAB_GRADE) as RubricFragment?

    override fun title(): String {
        return if (assignment != null) assignment!!.name else getString(R.string.assignment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // DO NOT USE setRetainInstance. It breaks the FragmentStatePagerAdapter.
        // The error is "Attempt to invoke virtual method 'int java.util.ArrayList.size()' on a null object reference"
        // setRetainInstance(this, true);
    }

    override fun onDestroy() {
        super.onDestroy()
        assignmentCallback.cancel()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_assignment, container, false)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        tabLayout.tabMode = if (!isTablet(context) && !isLandscape(context)) TabLayout.MODE_SCROLLABLE else TabLayout.MODE_FIXED
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager.offscreenPageLimit = 2
        viewPager.isSaveFromParentEnabled = false // Prevents a crash with FragmentStatePagerAdapter, when the EditAssignmentFragment is dismissed
        fragmentPagerAdapter = FragmentPagerDetailAdapter(childFragmentManager, false)
        viewPager.adapter = fragmentPagerAdapter
        setupTabLayoutColors()
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.tabMode = if (!isTablet(context) && !isLandscape(context)) TabLayout.MODE_SCROLLABLE else TabLayout.MODE_FIXED
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(Const.TAB_ID, 0)
        }
        // currentTab can either be save on orientation change or handleIntentExtras (when someone opens a link from an email)
        viewPager.currentItem = currentTab
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle?) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState?.let {
            it.putParcelable(Const.ASSIGNMENT, assignment)
            it.putInt(Const.TAB_ID, viewPager.currentItem)
            currentTab = viewPager.currentItem
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(assignmentId != null) AssignmentManager.getAssignment(assignmentId!!, canvasContext.id, true, assignmentCallback)
    }

    override fun applyTheme() {
        setupToolbarMenu(toolbar)
        toolbar.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, canvasContext)
    }

    override fun handleBackPressed(): Boolean {
        return if (assignmentDetailsFragment != null) {
            // Handles closing of fullscreen video (<sarcasm> Yay! nested fragments </sarcasm>)
            assignmentDetailsFragment!!.handleBackPressed()
        } else false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == RequestCodes.EDIT_ASSIGNMENT && resultCode == Activity.RESULT_OK && intent != null) {
            if (intent.hasExtra(Const.ASSIGNMENT)) {
                this.assignment = intent.getParcelableExtra(Const.ASSIGNMENT)
                assignmentDetailsFragment?.setAssignmentWithNotification(assignment, message, false, false)
            }
        } else if(submissionDetailsFragment != null) {
            submissionDetailsFragment?.onActivityResult(requestCode, resultCode, intent)
        }
    }

    fun updatedAssignment(assignment: Assignment) {
        populateFragments(assignment, false, false)
    }

    private fun populateFragments(assignment: Assignment?, isWithinAnotherCallback: Boolean, isCached: Boolean) {
        if (fragmentPagerAdapter == null) {
            return
        }

        if (assignment?.isLocked == true) {
            // recreate the adapter, because of slidingTabLayout's assumption that viewpager won't change size.
            fragmentPagerAdapter = FragmentPagerDetailAdapter(childFragmentManager, true)
            viewPager.adapter = fragmentPagerAdapter
        }

        if(assignment != null) {
            (0 until NUMBER_OF_TABS)
                    .mapNotNull { fragmentPagerAdapter!!.getRegisteredFragment(it) }
                    .forEach {
                        when (it) {
                            is AssignmentDetailsFragment -> it.setAssignment(assignment, isWithinAnotherCallback, isCached)
                            is SubmissionDetailsFragment -> {
                                it.setAssignmentFragment(WeakReference(this))
                                it.setAssignment(assignment, isWithinAnotherCallback, isCached)
                                it.setSubmissionDetailsFragmentCallback(this)
                            }
                            is RubricFragment -> it.setAssignment(assignment, isWithinAnotherCallback, isCached)
                        }
                    }
        }
    }

    private fun setupTabLayoutColors() {
        val color = ColorKeeper.getOrGenerateColor(canvasContext)
        tabLayout.setBackgroundColor(color)
        tabLayout.setTabTextColors(ContextCompat.getColor(context, R.color.glassWhite), Color.WHITE)
    }

    override fun updateSubmissionDate(submissionDate: Date) {
        assignmentDetailsFragment?.updateSubmissionDate(submissionDate)
    }

    override fun getParamForBookmark(): HashMap<String, String> {
        val params = super.getParamForBookmark()
        params.put(Param.ASSIGNMENT_ID, assignmentId.toString())
        return params
    }


    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        // Propagate userVisibleHint state to pager adapter for PageView tracking
        fragmentPagerAdapter?.setUserVisibleHint(isVisibleToUser)
    }

    internal inner class FragmentPagerDetailAdapter(fm: FragmentManager, private val isLocked: Boolean) : FragmentStatePagerAdapter(fm) {
        // http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager
        private var registeredFragments: SparseArray<WeakReference<Fragment>> = SparseArray()

        // region Modifications for PageView tracking sanity

        private var currentPrimaryItem by ReflectField<Fragment?>("mCurrentPrimaryItem", FragmentStatePagerAdapter::class.java)

        private var isVisible = parentFragment == null

        fun setUserVisibleHint(isVisibleToUser: Boolean) {
            if (!isVisibleToUser) {
                (0 until registeredFragments.size())
                    .mapNotNull { registeredFragments[it].get() }
                    .forEach { it.userVisibleHint = false }
            } else {
                currentPrimaryItem?.setMenuVisibility(true)
                currentPrimaryItem?.userVisibleHint = true
            }
            isVisible = isVisibleToUser
        }

        override fun setPrimaryItem(container: ViewGroup?, position: Int, `object`: Any?) {
            val fragment = `object` as? Fragment
            if (fragment !== currentPrimaryItem) {
                if (currentPrimaryItem != null) {
                    currentPrimaryItem?.setMenuVisibility(false)
                    currentPrimaryItem?.userVisibleHint = false
                }
                if (fragment != null && isVisible) {
                    fragment.setMenuVisibility(true)
                    fragment.userVisibleHint = true
                }
                currentPrimaryItem = fragment
            }
        }

        // endregion

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            registeredFragments.put(position, WeakReference(fragment))
            if (!isVisible) fragment.userVisibleHint = false
            return fragment
        }

        override fun destroyItem(container: ViewGroup?, position: Int, item: Any) {
            registeredFragments.remove(position)
            super.destroyItem(container, position, item)
        }

        fun getRegisteredFragment(position: Int): Fragment? {
            val weakReference = registeredFragments.get(position)
            return weakReference?.get()
        }

        override fun getItem(position: Int): Fragment? {
            val fragment: Fragment?
            val bundle: Bundle

            when (position) {
                ASSIGNMENT_TAB_DETAILS -> {
                    bundle = ParentFragment.createBundle(canvasContext)
                    fragment = ParentFragment.createFragment(AssignmentDetailsFragment::class.java, bundle)
                }
                ASSIGNMENT_TAB_SUBMISSION -> {
                    bundle = SubmissionDetailsFragment.createBundle(canvasContext)
                    fragment = ParentFragment.createFragment(SubmissionDetailsFragment::class.java, bundle)
                }
                ASSIGNMENT_TAB_GRADE -> {
                    bundle = RubricFragment.createBundle(canvasContext)
                    fragment = ParentFragment.createFragment(RubricFragment::class.java, bundle)
                }
                else -> {
                    bundle = ParentFragment.createBundle(canvasContext)
                    fragment = ParentFragment.createFragment(AssignmentDetailsFragment::class.java, bundle)
                }
            }
            return fragment
        }

        override fun getCount(): Int {
            return if (isLocked) 1 else NUMBER_OF_TABS
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                ASSIGNMENT_TAB_DETAILS -> if (isLocked) getString(R.string.assignmentLocked) else getString(AssignmentDetailsFragment.tabTitle)
                ASSIGNMENT_TAB_SUBMISSION -> getString(SubmissionDetailsFragment.getTabTitle())
                ASSIGNMENT_TAB_GRADE -> getString(RubricFragment.getTabTitle())
                else -> getString(AssignmentDetailsFragment.tabTitle)
            }
        }
    }

    private val assignmentCallback = object : StatusCallback<Assignment>() {
        override fun onResponse(response: Response<Assignment>, linkHeaders: LinkHeaders, type: ApiType) {
            if (!apiCheck()) return

            response.body().let {
                assignment = it
                populateFragments(assignment, true, APIHelper.isCachedResponse(response))
                assignmentDetailsFragment?.setAssignmentWithNotification(it, message, true, APIHelper.isCachedResponse(response))
                toolbar.title = title()
            }
        }

        override fun onFail(call: Call<Assignment>?, error: Throwable, response: Response<*>?) {
            //Unable to retrieve the assignment, likely was deleted at some point
            Toast.makeText(context, R.string.assignmentDeletedError, Toast.LENGTH_SHORT).show()
            navigation?.popCurrentFragment()
        }
    }

    override fun handleIntentExtras(extras: Bundle?) {
        super.handleIntentExtras(extras)
        if (extras == null) return

        if (extras.containsKey(Const.TAB_ID)) {
            currentTab = extras.getInt(Const.TAB_ID, 0)
        }

        if (extras.containsKey(Const.ASSIGNMENT)) {
            assignment = extras.getParcelable(Const.ASSIGNMENT)
            assignmentId = assignment!!.id
        } else if (urlParams != null) {
            assignmentId = parseLong(urlParams[Param.ASSIGNMENT_ID], -1)
            if (BaseRouterActivity.SUBMISSIONS_ROUTE == urlParams[Param.SLIDING_TAB_TYPE]) {
                currentTab = ASSIGNMENT_TAB_SUBMISSION
            } else if (BaseRouterActivity.RUBRIC_ROUTE == urlParams[Param.SLIDING_TAB_TYPE]) {
                currentTab = ASSIGNMENT_TAB_GRADE
            }
        } else {
            assignmentId = extras.getLong(Const.ASSIGNMENT_ID, -1)
        }

        if (extras.containsKey(Const.MESSAGE)) {
            message = extras.getString(Const.MESSAGE)
        }
    }

    override fun allowBookmarking(): Boolean {
        //navigation is a course, but isn't in notification list.
        return canvasContext.isCourseOrGroup && navigation?.currentFragment !is NotificationListFragment
    }

    companion object {

        val ASSIGNMENT_TAB_DETAILS = 0
        val ASSIGNMENT_TAB_SUBMISSION = 1
        val ASSIGNMENT_TAB_GRADE = 2
        val NUMBER_OF_TABS = 3

        fun createBundle(course: Course, assignment: Assignment): Bundle {
            val extras = ParentFragment.createBundle(course)
            extras.putParcelable(Const.ASSIGNMENT, assignment)
            extras.putLong(Const.ASSIGNMENT_ID, assignment.id)
            return extras
        }

        fun createBundle(course: Course, assignment: Assignment, tabId: Int): Bundle {
            val extras = ParentFragment.createBundle(course)
            extras.putParcelable(Const.ASSIGNMENT, assignment)
            extras.putLong(Const.ASSIGNMENT_ID, assignment.id)
            extras.putInt(Const.TAB_ID, tabId)

            return extras
        }

        fun createBundle(course: CanvasContext, assignmentId: Long): Bundle {
            val extras = ParentFragment.createBundle(course)
            extras.putLong(Const.ASSIGNMENT_ID, assignmentId)
            return extras
        }

        fun createBundle(context: Context, course: CanvasContext, assignmentId: Long, item: StreamItem?): Bundle {
            val extras = ParentFragment.createBundle(course)
            extras.putLong(Const.ASSIGNMENT_ID, assignmentId)

            if (item != null) {
                extras.putString(Const.MESSAGE, item.getMessage(context))
            }
            return extras
        }
    }
}
