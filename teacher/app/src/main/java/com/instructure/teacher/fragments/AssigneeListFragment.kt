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
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.instructure.canvasapi2.models.CanvasComparable
import com.instructure.canvasapi2.models.Group
import com.instructure.canvasapi2.models.Section
import com.instructure.canvasapi2.models.User
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandautils.fragments.BaseExpandableSyncFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.adapters.AssigneeListAdapter
import com.instructure.teacher.factory.AssigneeListPresenterFactory
import com.instructure.teacher.holders.AssigneeViewHolder
import com.instructure.teacher.models.AssigneeCategory
import com.instructure.teacher.models.DueDateGroup
import com.instructure.teacher.presenters.AssigneeListPresenter
import com.instructure.teacher.utils.EditDateGroups
import com.instructure.teacher.utils.RecyclerViewUtils
import com.instructure.teacher.utils.setupCloseButton
import com.instructure.teacher.utils.setupMenu
import com.instructure.teacher.view.EmptyPandaView
import com.instructure.teacher.viewinterface.AssigneeListView
import kotlinx.android.synthetic.main.fragment_assignee_list.*
import java.util.*

class AssigneeListFragment : BaseExpandableSyncFragment<
        AssigneeCategory,
        CanvasComparable<*>,
        AssigneeListView,
        AssigneeListPresenter,
        AssigneeViewHolder,
        AssigneeListAdapter>(), AssigneeListView {

    private var mDateGroups: EditDateGroups by SerializableListArg(emptyList())
    private var mTargetIdx: Int by IntArg()
    private var sections by ParcelableArrayListArg<Section>(arrayListOf())
    private var groups by ParcelableArrayListArg<Group>(arrayListOf())
    private var students by ParcelableArrayListArg<User>(arrayListOf())

    private val assigneeRecyclerView by bind<RecyclerView>(R.id.recyclerView)
    private val swipeRefreshLayout by bind<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
    private val emptyPandaView by bind<EmptyPandaView>(R.id.emptyPandaView)
    private val saveButton: TextView? get() = view?.findViewById<TextView>(R.id.menuSave)

    override fun layoutResId() = R.layout.fragment_assignee_list
    override fun getList() = presenter.data
    override fun getRecyclerView() = assigneeRecyclerView
    override fun withPagination() = false
    override fun perPageCount() = ApiPrefs.perPageCount
    override fun getPresenterFactory() = AssigneeListPresenterFactory(mDateGroups, mTargetIdx, sections, groups, students)
    override fun onCreateView(view: View?) {}

    private fun performSave() {
        presenter.save()
        activity.onBackPressed()
    }

    private fun <T : SpannableStringBuilder> T.appendColored(text: CharSequence, color: Int): T = apply {
        val start = length
        append(text)
        setSpan(ForegroundColorSpan(color), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    override fun updateSelectedAssignees(assigneeNames: ArrayList<String>, displayEveryone: Boolean, displayAsEveryoneElse: Boolean) {
        if (displayEveryone) assigneeNames.add(0, getString(if (displayAsEveryoneElse) R.string.everyone_else else R.string.everyone))
        val span = SpannableStringBuilder()
        val nameColor = ContextCompat.getColor(context, R.color.colorAccent)
        val separatorColor = ContextCompat.getColor(context, R.color.defaultTextGray)
        for (name in assigneeNames) {
            span.appendColored(name, nameColor)
            if (name !== assigneeNames.last()) span.appendColored(", ", separatorColor)
        }
        selectedAssigneesTextView.setVisible(assigneeNames.isNotEmpty()).text = span
    }

    override fun onPresenterPrepared(presenter: AssigneeListPresenter?) {
        RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.recyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))
        addSwipeToRefresh(swipeRefreshLayout)
    }

    override fun onReadySetGo(presenter: AssigneeListPresenter) {
        assigneeRecyclerView.adapter = adapter
        presenter.loadData(false)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    private fun setupToolbar() {
        toolbar.setupCloseButton(this)
        toolbar.title = getString(R.string.page_title_add_assignees)
        toolbar.setupMenu(R.menu.menu_save_generic) { performSave() }
        ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
        saveButton?.setTextColor(ThemePrefs.buttonColor)
    }

    override fun onRefreshStarted() {
        emptyPandaView.setLoading()
    }

    override fun onRefreshFinished() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun checkIfEmpty() {
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, assigneeRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    override fun getAdapter(): AssigneeListAdapter {
        if (mAdapter == null) {
            mAdapter = AssigneeListAdapter(context, presenter)
        }
        return mAdapter
    }

    override fun notifyItemChanged(position: Int) {
        adapter.notifyItemChanged(position)
    }

    companion object {
        @JvmStatic val DATE_GROUPS = "dateGroups"
        @JvmStatic val TARGET_INDEX = "targetIndex"
        @JvmStatic val SECTIONS = "sections"
        @JvmStatic val GROUPS = "groups"
        @JvmStatic val STUDENTS = "students"

        @JvmStatic
        fun newInstance(args: Bundle) = AssigneeListFragment().apply {
            this.mDateGroups = args.getSerializable(DATE_GROUPS) as List<DueDateGroup>
            this.mTargetIdx = args.getInt(TARGET_INDEX)
            this.sections = args.getParcelableArrayList(SECTIONS)
            this.groups = args.getParcelableArrayList(GROUPS)
            this.students = args.getParcelableArrayList(STUDENTS)
        }

        @JvmStatic
        fun makeBundle(dateGroups: EditDateGroups, targetIdx: Int, sections: List<Section>, groups: List<Group>, students: List<User>): Bundle {
            val args = Bundle()
            args.putSerializable(AssigneeListFragment.DATE_GROUPS, ArrayList(dateGroups))
            args.putInt(AssigneeListFragment.TARGET_INDEX, targetIdx)
            args.putParcelableArrayList(AssigneeListFragment.SECTIONS, ArrayList(sections.toList()))
            args.putParcelableArrayList(AssigneeListFragment.GROUPS, ArrayList(groups.toList()))
            args.putParcelableArrayList(AssigneeListFragment.STUDENTS, ArrayList(students.toList()))
            return args
        }
    }
}
