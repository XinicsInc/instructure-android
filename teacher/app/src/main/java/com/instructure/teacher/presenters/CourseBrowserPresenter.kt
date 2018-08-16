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
package com.instructure.teacher.presenters

import com.instructure.canvasapi2.apis.AttendanceAPI
import com.instructure.canvasapi2.managers.LaunchDefinitionsManager
import com.instructure.canvasapi2.managers.TabManager
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.LaunchDefinition
import com.instructure.canvasapi2.models.Tab
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.teacher.viewinterface.CourseBrowserView
import instructure.androidblueprint.SyncPresenter
import kotlinx.coroutines.experimental.Job

class CourseBrowserPresenter(val course: Course, val filter: (Tab, Int) -> Boolean) : SyncPresenter<Tab, CourseBrowserView>(Tab::class.java) {

    var mApiCalls: Job? = null

    override fun loadData(forceNetwork: Boolean) {
        if(!forceNetwork && data.size() > 0)  return

        onRefreshStarted()

        mApiCalls = tryWeave {
            val tabs = awaitApi<List<Tab>> { TabManager.getTabs(course, it, forceNetwork) }.filter { !(it.isExternal && it.isHidden) } //we don't want to list external tools that are hidden
            val launchDefinitions = awaitApi<List<LaunchDefinition>> {
                LaunchDefinitionsManager.getLaunchDefinitionsForCourse(course.id, it, forceNetwork)
            }

            var attendanceId: Int = 0

            launchDefinitions.forEach {
                val ltiDefinitionUrl = it.placements?.courseNavigation?.url
                if (ltiDefinitionUrl != null && (
                        ltiDefinitionUrl.contains(AttendanceAPI.BASE_DOMAIN) ||
                                ltiDefinitionUrl.contains(AttendanceAPI.BASE_TEST_DOMAIN))) {
                    //Has rollcall (Attendance) installed, show tool
                    attendanceId = it.definitionId
                }
            }

            data.addOrUpdate(tabs.filter {
                filter(it, attendanceId)
            })

            viewCallback?.let {
                it.onRefreshFinished()
                it.checkIfEmpty()
            }
        } catch {
            it.cause?.printStackTrace()
            viewCallback?.let {
                it.onRefreshFinished()
                it.checkIfEmpty()
            }
        }
    }

    override fun refresh(forceNetwork: Boolean) {
        onRefreshStarted()
        mApiCalls?.cancel()
        clearData()
        loadData(forceNetwork)
    }

    override fun compare(item1: Tab, item2: Tab) = item1.position.compareTo(item2.position)
    override fun areItemsTheSame(item1: Tab, item2: Tab) = item1.id == item2.id

    /**
     * Get shortened course name
     * E.g. BIO 101
     *
     * @return String representing the shortened name of the course.
     */
    fun getShortCourseName(): String {
        return course.courseCode
    }

    override fun onDestroyed() {
        super.onDestroyed()
        mApiCalls?.cancel()
    }
}
