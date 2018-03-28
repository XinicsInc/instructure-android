/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.canvasapi2.models

import com.google.gson.annotations.SerializedName
import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

@JvmSuppressWildcards
@PaperParcel
data class Submission(
        private var id: Long = 0,
        var grade: String? = null,
        var score: Double = 0.0,
        var attempt: Long = 0,
        @SerializedName("submitted_at")
        var submittedAt: Date? = null,
        @SerializedName("submission_comments")
        var submissionComments: List<SubmissionComment> = ArrayList(),
        var commentCreated: Date? = null,
        var mediaContentType: String? = null,
        var mediaCommentUrl: String? = null,
        var mediaCommentDisplay: String? = null,
        @SerializedName("submission_history")
        var submissionHistory: List<Submission?> = ArrayList(),
        var attachments: ArrayList<Attachment> = arrayListOf(),
        var body: String? = null,
        @SerializedName("rubric_assessment")
        var rubricAssessment: HashMap<String, RubricCriterionAssessment> = hashMapOf(),
        @SerializedName("grade_matches_current_submission")
        var isGradeMatchesCurrentSubmission: Boolean = false,
        @SerializedName("workflow_state")
        var workflowState: String? = null,
        @SerializedName("submission_type")
        var submissionType: String? = null,
        @SerializedName("preview_url")
        var previewUrl: String? = null,
        var url: String? = null,
        @SerializedName("late")
        var isLate: Boolean = false,
        @SerializedName("excused")
        var isExcused: Boolean = false,
        @SerializedName("media_comment")
        var mediaComment: MediaComment? = null,
        //Conversation Stuff
        @SerializedName("assignment_id")
        var assignmentId: Long = 0,
        var assignment: Assignment? = null,
        @SerializedName("user_id")
        var userId: Long = 0,
        @SerializedName("grader_id")
        var graderId: Long = 0,
        var user: User? = null,
        //this value could be null. Currently will only be returned when getting the submission for
        //a user when the submission_type is discussion_topic
        @SerializedName("discussion_entries")
        var discussionEntries: ArrayList<DiscussionEntry> = arrayListOf(),
        // Group Info only available when including groups in the Submissions#index endpoint
        var group: Group? = null,
        @SerializedName("points_deducted")
        var pointsDeducted: Double? = null,
        @SerializedName("entered_score")
        var enteredScore: Double = 0.0,
        @SerializedName("entered_grade")
        var enteredGrade: String? = null
        ) : CanvasModel<Submission>(), PaperParcelable {

    override fun getId() = id

    fun setId(id: Long) { this.id = id }

    val isWithoutGradedSubmission: Boolean get() = !isGraded && submissionType == null

    val isGraded: Boolean get() = grade != null

    @Suppress("unused")
    val userIds: List<Long> get() = submissionComments.map { it.authorId }

    /* Submissions will have dummy submissions if they grade an assignment with no actual submissions. We want to see if any are not dummy submissions */
    @Suppress("unused")
    fun hasRealSubmission() = submissionHistory.any { it?.submissionType != null}

    override fun getComparisonDate() = submittedAt

    override fun getComparisonString() = submissionType

    override fun describeContents() = 0

    companion object {
        @Suppress("unresolved_reference")
        @JvmField val CREATOR = PaperParcelSubmission.CREATOR
    }

}
