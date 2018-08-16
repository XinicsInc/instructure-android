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

package com.instructure.teacher.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Toast
import com.instructure.canvasapi2.managers.GroupManager
import com.instructure.canvasapi2.managers.SectionManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Group
import com.instructure.canvasapi2.models.Section
import com.instructure.canvasapi2.utils.weave.inParallel
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.dismissExisting
import com.instructure.teacher.R
import com.instructure.teacher.adapters.PeopleFilterAdapter
import kotlinx.coroutines.experimental.Job
import kotlin.properties.Delegates

class PeopleListFilterDialog : AppCompatDialogFragment() {
    init {
        retainInstance = true
    }
    private var recyclerView: RecyclerView? = null
    private var finishedCallback: (canvasContexts: ArrayList<CanvasContext>) -> Unit by Delegates.notNull()
    private var canvasContext: CanvasContext? = null
    private var canvasContextMap: HashMap<CanvasContext, Boolean> = HashMap()
    private var canvasContextIdList: ArrayList<Long> = ArrayList()
    private var shouldIncludeGroups = true
    private var mApiCalls: Job? = null

    companion object {

        @JvmStatic
        fun getInstance(manager: FragmentManager, canvasContextIdList: ArrayList<Long>, canvasContext: CanvasContext, shouldIncludeGroups: Boolean, callback: (canvasContexts: ArrayList<CanvasContext>) -> Unit) : PeopleListFilterDialog {
            manager.dismissExisting<PeopleListFilterDialog>()
            val dialog = PeopleListFilterDialog()
            val args = Bundle()
            args.putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            args.putBoolean(Const.GROUPS, shouldIncludeGroups)
            dialog.canvasContextIdList = canvasContextIdList
            dialog.arguments = args
            dialog.finishedCallback = callback
            return dialog
        }
    }


    fun updateCanvasContexts(sections: ArrayList<Section>, groups: ArrayList<Group>) {
        // the selected contexts and the previously selected contexts need to be preserved on rotation
        val combinedContextList: ArrayList<Long> = canvasContextIdList
        combinedContextList.addAll(canvasContextMap.filter { it.value }.keys.map { it.id })
        recyclerView?.adapter = PeopleFilterAdapter(getCanvasContextList(context, sections, groups), combinedContextList, { canvasContext: CanvasContext, isChecked: Boolean ->
            canvasContextMap.put(canvasContext, isChecked)
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(ContextThemeWrapper(activity, 0), R.layout.dialog_canvas_context_list, null)

        canvasContext = arguments.getParcelable(Const.CANVAS_CONTEXT)
        shouldIncludeGroups = arguments.getBoolean(Const.GROUPS)

        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView?.layoutManager = LinearLayoutManager(context)


        val dialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle(activity.getString(R.string.filterBy))
                .setView(view)
                .setPositiveButton(activity.getString(android.R.string.ok), { _, _ ->
                    // get the list of checked Canvas Contexts from the map
                    finishedCallback(canvasContextMap.filter { it.value }.keys.toMutableList() as ArrayList<CanvasContext>)
                })
                .setNegativeButton(activity.getString(R.string.cancel), null)
                .create()

        dialog.setOnShowListener {
            loadData(false)
            dialog.getButton(AppCompatDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
            dialog.getButton(AppCompatDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
        }


        return dialog
    }

    fun loadData(forceNetwork: Boolean) {
        mApiCalls = weave {

            try {
                var groups: ArrayList<Group> = ArrayList()
                var sections: ArrayList<Section> = ArrayList()
                inParallel {

                    // Get Sections
                    await<List<Section>>({ SectionManager.getAllSectionsForCourse(canvasContext?.id ?: 0, it, forceNetwork) }) {
                        sections = it as ArrayList<Section>
                    }

                    if(shouldIncludeGroups) {
                        // Get groups
                        await<List<Group>>({ GroupManager.getAllGroupsForCourse(canvasContext?.id ?: 0, it, forceNetwork) }) {
                            groups = it as ArrayList<Group>
                        }
                    }
                }

                updateCanvasContexts(sections, groups)
            } catch (ignore: Exception) {
                if (activity != null) {
                    Toast.makeText(activity, R.string.error_occurred, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getCanvasContextList(context: Context, sections: List<Section>, groups: List<Group>): ArrayList<CanvasContext> {
        val canvasContexts = ArrayList<CanvasContext>()

        if(sections.isNotEmpty()) {
            val sectionSeparator = Course()
            sectionSeparator.name = context.getString(R.string.sections)
            sectionSeparator.id = -1
            canvasContexts.add(sectionSeparator)

            canvasContexts.addAll(sections)
        }

        if(groups.isNotEmpty()) {
            val groupSeparator = Course()
            groupSeparator.name = context.getString(R.string.assignee_type_groups)
            groupSeparator.id = -1
            canvasContexts.add(groupSeparator)

            canvasContexts.addAll(groups)
        }

        return canvasContexts
    }

    override fun onDestroyView() {
        // Fix for rotation bug
        mApiCalls?.cancel()
        dialog?.let { if (retainInstance) it.setDismissMessage(null) }
        super.onDestroyView()
    }
}
