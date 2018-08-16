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

import com.instructure.canvasapi2.apis.FeaturesAPI
import com.instructure.canvasapi2.managers.*
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.pandautils.utils.AssignmentUtils2
import com.instructure.teacher.events.ToDoListUpdatedEvent
import com.instructure.teacher.presenters.AssignmentSubmissionListPresenter.Companion.makeGroupSubmissions
import com.instructure.teacher.utils.TeacherPrefs
import com.instructure.teacher.utils.getState
import com.instructure.teacher.viewinterface.ToDoView
import instructure.androidblueprint.SyncPresenter
import org.greenrobot.eventbus.EventBus

class ToDoPresenter : SyncPresenter<ToDo, ToDoView>(ToDo::class.java) {

    var apiCall: WeaveJob? = null
    var routeCalls: WeaveJob? = null

    override fun loadData(forceNetwork: Boolean) {
        if (data.size() > 0 && !forceNetwork) return
        viewCallback?.onRefreshStarted()

        apiCall = tryWeave {
            viewCallback?.onRefreshStarted()

            var courses: HashMap<Long, Course> = HashMap()
            var groups: HashMap<Long, Group> = HashMap()
            inParallel {

                // Get Courses
                await<List<Course>>({ CourseManager.getCourses(forceNetwork, it) }) {

                    it.forEach { courses.put(it.id, it) }
                }


                // Get groups
                await<List<Group>>({ GroupManager.getAllGroups(it, forceNetwork) }) {
                    it.forEach { groups.put(it.id, it) }
                }

            }
            awaitPaginated<List<ToDo>> {
                onRequest { callback ->
                    ToDoManager.getUserTodos(callback, forceNetwork)
                }
                onResponse { response ->
                    // set the context info for each to do
                    response.forEach{ ToDo.setContextInfo(it, courses, groups) }
                    data.addOrUpdate(response)

                    // We want the count of the assignments that need grading. If there are more than 100 we will just show 99+
                    val todoCount = response.sumBy { it.needsGradingCount }

                    EventBus.getDefault().post(ToDoListUpdatedEvent(todoCount))
                    viewCallback?.onRefreshFinished()
                    viewCallback?.checkIfEmpty()
                }
                onError { }
            }

        } catch {
            it.printStackTrace()
        }
    }

    fun goToUngradedSubmissions(assignment: Assignment, courseId: Long) {
        routeCalls = tryWeave {
            viewCallback?.onRefreshStarted()

            val unfilteredSubmissions: List<GradeableStudentSubmission>
            // get the course
            val course = awaitApi<Course> { CourseManager.getCourse(courseId, it, true) }
            val (gradeableStudents, enrollments, submissions) = awaitApis<List<GradeableStudent>, List<Enrollment>, List<Submission>>(
                    { AssignmentManager.getAllGradeableStudentsForAssignment(assignment.courseId, assignment.id, true, it) },
                    { EnrollmentManager.getAllEnrollmentsForCourse(assignment.courseId, null, true, it) },
                    { AssignmentManager.getAllSubmissionsForAssignment(assignment.courseId, assignment.id, true, it) }
            )
            val enrollmentMap = enrollments.associateBy { it.user.id }
            val students = gradeableStudents.distinctBy { it.id }.map { enrollmentMap[it.id]?.user }.filterNotNull()
            if (assignment.groupCategoryId > 0 && !assignment.isGradeGroupsIndividually) {
                val groups = awaitApi<List<Group>> { CourseManager.getGroupsForCourse(assignment.courseId, it, false) }
                        .filter { it.groupCategoryId == assignment.groupCategoryId }
                unfilteredSubmissions = makeGroupSubmissions(students, groups, submissions)
            } else {
                val submissionMap = submissions.associateBy { it.userId }
                unfilteredSubmissions = students.map {
                    GradeableStudentSubmission(StudentAssignee(it), submissionMap[it.id])
                }
            }

            // see if anonymous grading is set by the institution
            val features = awaitApi<List<String>> { FeaturesManager.getEnabledFeaturesForCourse(assignment.courseId, true, it) }
            TeacherPrefs.enforceGradeAnonymously = (features.contains(FeaturesAPI.ANONYMOUS_GRADING))


            // filter the submissions to just the ones that need grading
            val filteredSubmissions = unfilteredSubmissions.filter { it.submission?.let { assignment.getState(it) == AssignmentUtils2.ASSIGNMENT_STATE_SUBMITTED ||  assignment.getState(it) == AssignmentUtils2.ASSIGNMENT_STATE_SUBMITTED_LATE || !it.isGradeMatchesCurrentSubmission } ?: false }

            viewCallback?.onRefreshFinished()
            viewCallback?.onRouteSuccessfully(course, assignment, filteredSubmissions)

        } catch {
            viewCallback?.onRefreshFinished()
            viewCallback?.onRouteFailed()
            it.printStackTrace()
        }
    }

    override fun onDestroyed() {
        apiCall?.cancel()
        routeCalls?.cancel()
        super.onDestroyed()
    }

    override fun refresh(forceNetwork: Boolean) {
        apiCall?.cancel()
        clearData()
        loadData(forceNetwork)
    }

    fun nextPage() = apiCall?.next()

    public override fun areContentsTheSame(oldItem: ToDo, newItem: ToDo): Boolean {
        return if (containsNull(oldItem.scheduleItem, newItem.scheduleItem) || oldItem.assignment.id != newItem.assignment.id) {
            false
        } else oldItem.htmlUrl == newItem.htmlUrl
    }

    public override fun areItemsTheSame(item1: ToDo, item2: ToDo) = item1.id == item2.id

    // We don't want to sort the items locally, but we do need id comparison for item updates
    override fun compare(item1: ToDo, item2: ToDo) = if (item1.id == item2.id) 0 else -1

    private fun containsNull(oldItem: Any?, newItem: Any?) = oldItem == null || newItem == null
}
