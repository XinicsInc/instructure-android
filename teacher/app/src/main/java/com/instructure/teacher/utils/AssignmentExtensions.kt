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
@file:JvmName("AssignmentUtils")

package com.instructure.teacher.utils

import android.content.Context
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.AssignmentDueDate
import com.instructure.canvasapi2.models.AssignmentOverride
import com.instructure.canvasapi2.models.Submission
import com.instructure.canvasapi2.models.post_models.AssignmentPostBody
import com.instructure.canvasapi2.models.post_models.OverrideBody
import com.instructure.canvasapi2.type.SubmissionType
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.NumberHelper
import com.instructure.pandautils.utils.AssignmentUtils2
import com.instructure.pandautils.utils.AssignmentUtils2.*
import com.instructure.teacher.R
import com.instructure.teacher.models.CoreDates
import com.instructure.teacher.models.DueDateGroup
import java.util.*

fun Assignment.getAssignmentIcon() = when {
    Assignment.SUBMISSION_TYPE.ONLINE_QUIZ in submissionTypes -> R.drawable.vd_quiz
    Assignment.SUBMISSION_TYPE.DISCUSSION_TOPIC in submissionTypes -> R.drawable.vd_discussion
    else -> R.drawable.vd_assignment
}

fun List<SubmissionType>?.getAssignmentIcon() = when {
    this == null -> R.drawable.vd_assignment
    SubmissionType.online_quiz in this -> R.drawable.vd_quiz
    SubmissionType.discussion_topic in this -> R.drawable.vd_discussion
    else -> R.drawable.vd_assignment
}
//region Grouped due dates

var AssignmentPostBody.coreDates: CoreDates
    get() = CoreDates(
            APIHelper.stringToDate(dueAt),
            APIHelper.stringToDate(lockAt),
            APIHelper.stringToDate(unlockAt)
    )
    set(dates) {
        dueAt = dates.dueDate.toApiString()
        lockAt = dates.lockDate.toApiString()
        unlockAt = dates.unlockDate.toApiString()
    }

val Assignment.coreDates: CoreDates
    get() = CoreDates(dueAt, lockAt, unlockAt)

var AssignmentDueDate.coreDates: CoreDates
    get() = CoreDates(dueAt, lockAt, unlockAt)
    set(dates) {
        setDueAt(dates.dueDate.toApiString())
        setLockAt(dates.lockDate.toApiString())
        setUnlockAt(dates.unlockDate.toApiString())
    }

var OverrideBody.coreDates: CoreDates
    get() = CoreDates(dueAt, lockAt, unlockAt)
    set(dates) {
        dueAt = dates.dueDate
        lockAt = dates.lockDate
        unlockAt = dates.unlockDate
    }

typealias EditDateGroups = List<DueDateGroup>

val Assignment.groupedDueDates: EditDateGroups
    get() {
        val dates = ArrayList(allDates)
        if (!isOnlyVisibleToOverrides && dates.none { it.isBase }) {
            dates += AssignmentDueDate().apply {
                isBase = true
                coreDates = this@groupedDueDates.coreDates
            }
        }
        return dates.groupBy { it.coreDates }
            .map { (date, simpleDates) ->
                val overrides = simpleDates.filter { it.id > 0 }.map { simpleDate -> overrides?.firstOrNull { it.id == simpleDate.id } ?: AssignmentOverride() }
                DueDateGroup(
                        sectionIds = overrides.map { it.courseSectionId }.filter { it != 0L },
                        groupIds = overrides.map { it.groupId }.filter { it != 0L },
                        studentIds = overrides.flatMap { it.studentIds?.asList() ?: emptyList() },
                        isEveryone = simpleDates.any { it.isBase },
                        coreDates = date
                )
            }
    }

fun AssignmentPostBody.setGroupedDueDates(dates: EditDateGroups) {
    val newOverrides: List<OverrideBody> = dates.flatMap { (_, sections, groups, students, coreDate) ->
        val mappedOverrides = arrayListOf<OverrideBody>()
        mappedOverrides += groups.map {
            OverrideBody().apply {
                groupId = it
                coreDates = coreDate
            }
        }
        mappedOverrides += sections.map {
            OverrideBody().apply {
                courseSectionId = it
                coreDates = coreDate
            }
        }
        if (students.isNotEmpty()) {
            mappedOverrides += OverrideBody().apply {
                studentIds = students.toLongArray()
                coreDates = coreDate
            }
        }
        mappedOverrides
    }

    assignmentOverrides = newOverrides

    val baseDate = dates.firstOrNull { it.isEveryone }
    if (baseDate == null) {
        isOnlyVisibleToOverrides = newOverrides.isNotEmpty()
    } else {
        isOnlyVisibleToOverrides = false
        coreDates = baseDate.coreDates
    }
}
//endregion

fun Assignment.getGradeText(submission: Submission?, context: Context, includePointsPossible: Boolean = true, includeLatePenalty: Boolean = false): String {
    //If the submission doesn't exist, so we return an empty string
    if(submission == null) return ""

    //cover the first edge case: excused assignment
    if(submission.isExcused) {
        return context.getString(R.string.excused)
    }

    //cover the second edge case: NOT_GRADED type and no grade
    if(Assignment.getGradingTypeFromAPIString(this.gradingType) == Assignment.GRADING_TYPE.NOT_GRADED) {
        return context.getString(R.string.not_graded)
    }

    //first lets see if the assignment is graded
    if(submission.grade != null && submission.grade != "null") {
        return when(Assignment.getGradingTypeFromAPIString(this.gradingType)) {
            Assignment.GRADING_TYPE.POINTS ->
                if(includeLatePenalty) {
                    getPointsPossibleWithNoParenthesis(submission.enteredScore, this.pointsPossible)
                } else {
                    getPointsPossibleWithNoParenthesis(submission.score, this.pointsPossible)
                }
            //edge case, NOT_GRADED type with grade, it COULD happen
            Assignment.GRADING_TYPE.NOT_GRADED ->
                context.getString(R.string.not_graded)
            else ->{
                var grade = submission.grade
                if (this.gradingType == Assignment.PERCENT_TYPE) {
                    try {
                        val value: Double = if(includeLatePenalty) submission.enteredGrade?.removeSuffix("%")?.toDouble() as Double else submission.grade?.removeSuffix("%")?.toDouble() as Double
                        grade = NumberHelper.doubleToPercentage(value, 2)
                    } catch (e: NumberFormatException) { }
                }
                when(submission.grade) {
                    "complete" ->
                        grade = context.getString(R.string.complete_grade)
                    "incomplete" ->
                        grade = context.getString(R.string.incomplete_grade)
                }
                if (includePointsPossible) {
                    if(includeLatePenalty) {
                        context.getString(R.string.grade_value_format, grade, getPointsPossibleWithParenthesis(submission.enteredScore, this.pointsPossible))
                    } else {
                        context.getString(R.string.grade_value_format, grade, getPointsPossibleWithParenthesis(submission.score, this.pointsPossible))
                    }
                } else {
                    grade.orEmpty()
                }
            }

        }
    } else {
        //return empty string for "empty" state
        return ""
    }
}

fun getGradeText(
    context: Context,
    gradingStatus: String?,
    gradingType: String,
    grade: String?,
    enteredGrade: String?,
    score: Double?,
    enteredScore: Double?,
    pointsPossible: Double,
    includePointsPossible: Boolean = true,
    includeLatePenalty: Boolean = false
): String {
    if (gradingStatus == null) return ""

    // Cover the first edge case: excused assignment
    if (gradingStatus == "excused") return context.getString(R.string.excused)


    // Cover the second edge case: NOT_GRADED type and no grade
    if (Assignment.getGradingTypeFromAPIString(gradingType) == Assignment.GRADING_TYPE.NOT_GRADED) {
        return context.getString(R.string.not_graded)
    }

    // First let's see if the assignment is graded
    if (gradingStatus == "graded" && score != null && enteredScore != null) {
        return when (Assignment.getGradingTypeFromAPIString(gradingType)) {
            Assignment.GRADING_TYPE.POINTS ->
                if (includeLatePenalty) {
                    getPointsPossibleWithNoParenthesis(enteredScore, pointsPossible)
                } else {
                    getPointsPossibleWithNoParenthesis(score, pointsPossible)
                }
        // Edge case, NOT_GRADED type with grade, it COULD happen
            Assignment.GRADING_TYPE.NOT_GRADED -> context.getString(R.string.not_graded)
            else -> {
                var formattedGrade = grade
                if (gradingType == Assignment.PERCENT_TYPE) {
                    try {
                        val value: Double = if (includeLatePenalty) {
                            enteredGrade?.removeSuffix("%")?.toDouble() as Double
                        } else {
                            grade?.removeSuffix( "%")?.toDouble() as Double
                        }
                        formattedGrade = NumberHelper.doubleToPercentage(value, 2)
                    } catch (ignored: NumberFormatException) { }
                }
                when (grade) {
                    "complete" ->
                        formattedGrade = context.getString(R.string.complete_grade)
                    "incomplete" ->
                        formattedGrade = context.getString(R.string.incomplete_grade)
                }
                if (includePointsPossible) {
                    if (includeLatePenalty) {
                        context.getString(
                            R.string.grade_value_format,
                            formattedGrade,
                            getPointsPossibleWithParenthesis(enteredScore, pointsPossible)
                        )
                    } else {
                        context.getString(
                            R.string.grade_value_format,
                            formattedGrade,
                            getPointsPossibleWithParenthesis(score, pointsPossible)
                        )
                    }
                } else {
                    grade.orEmpty()
                }
            }

        }
    } else {
        // Return empty string for "empty" state
        return ""
    }
}

private fun getPointsPossibleWithNoParenthesis(points: Double, pointsPossible: Double): String {
    return NumberHelper.formatDecimal(points, 2, true) + "/" + NumberHelper.formatDecimal(pointsPossible, 2, true)
}

fun getPointsPossibleWithParenthesis(points: Double, pointsPossible: Double): String {
    return "(" + NumberHelper.formatDecimal(points, 2, true) + "/" + NumberHelper.formatDecimal(pointsPossible, 2, true) + ")"
}

fun Assignment?.getState(submission: Submission?) = AssignmentUtils2.getAssignmentState(this, submission)

/**
 *
 * @return Pair(stringRes: Int, colorRes: Int)
 */
fun Assignment.getResForSubmission(submission: Submission?): Pair<Int, Int> {
    when(getAssignmentState(this, submission)) {
        ASSIGNMENT_STATE_MISSING -> {
            //if they haven't turned it in but there is no due date, we just want to show it as "Not Submitted"
            if(this.dueAt == null) {
                return Pair(R.string.submission_status_not_submitted, R.color.defaultTextGray)
            } else {
                return Pair(R.string.submission_status_missing, R.color.submission_status_color_missing)
            }
        }

        ASSIGNMENT_STATE_GRADED -> {
            if (submission != null && (submission.attempt > 0 || (this.submissionTypes != null && this.submissionTypes.contains(Assignment.SUBMISSION_TYPE.ON_PAPER)))) {
                // User has made attempts, so it has been submitted, or there is a submission and it was on paper
                return Pair(R.string.submission_status_submitted, R.color.submission_status_color_submitted)
            } else if (this.dueAt == null) {
                // No Due date + no submission + graded == Not Submitted
                return Pair(R.string.submission_status_not_submitted, R.color.defaultTextGray)
            } else if ((this.dueAt as Date).time >= Calendar.getInstance().timeInMillis){
                // Not past due date + no submission + grade == Not submitted yet
                return Pair(R.string.submission_status_not_submitted, R.color.defaultTextGray)
            } else {
                // Past due + no submission + grade == Missing
                return Pair(R.string.submission_status_missing, R.color.submission_status_color_missing)
            }
        }

        ASSIGNMENT_STATE_SUBMITTED_LATE, ASSIGNMENT_STATE_GRADED_LATE ->
            return Pair(R.string.submission_status_late, R.color.submission_status_color_late)

        ASSIGNMENT_STATE_SUBMITTED ->
            return Pair(R.string.submission_status_submitted, R.color.submission_status_color_submitted)

        ASSIGNMENT_STATE_DUE ->
            return Pair(R.string.submission_status_not_submitted, R.color.defaultTextGray)

        else -> return Pair(-1, -1)
    }
}

fun getResForSubmission(submissionStatus: String?): Pair<Int, Int> {
    return when(submissionStatus) {
        "missing" -> Pair(R.string.submission_status_missing, R.color.submission_status_color_missing)
        "late" -> Pair(R.string.submission_status_late, R.color.submission_status_color_late)
        "submitted" -> Pair(R.string.submission_status_submitted, R.color.submission_status_color_submitted)
        "unsubmitted" -> Pair(R.string.submission_status_not_submitted, R.color.defaultTextGray)
        else -> Pair(-1, -1)
    }
}
