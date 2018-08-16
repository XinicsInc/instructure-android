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
package com.instructure.teacher.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.support.v7.widget.AppCompatTextView
import android.text.Html
import android.text.format.Formatter
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.Assignment.SUBMISSION_TYPE
import com.instructure.canvasapi2.models.MediaComment
import com.instructure.canvasapi2.models.Submission
import com.instructure.pandautils.utils.DP
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.onClick
import com.instructure.pandautils.utils.setGone
import com.instructure.teacher.R
import com.instructure.teacher.utils.getColorCompat
import com.instructure.teacher.utils.iconRes
import com.instructure.canvasapi2.utils.prettyPrint
import kotlinx.android.synthetic.main.comment_submission_attachment_view.view.*
import org.greenrobot.eventbus.EventBus

@SuppressLint("ViewConstructor")
class CommentSubmissionView(context: Context, val submission: Submission) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        val type = submission.submissionType?.let { Assignment.getSubmissionTypeFromAPIString(it) }
        val hasSubmission = submission.workflowState != "unsubmitted" && type != null
        if (hasSubmission) {
            if (type == SUBMISSION_TYPE.ONLINE_UPLOAD) {
                setupAttachmentLabel()
                setupAttachments()
            } else {
                setupSubmissionAsAttachment(type!!)
            }
        }
    }

    private fun setupSubmissionAsAttachment(type: SUBMISSION_TYPE) {
        val view = LayoutInflater.from(context).inflate(R.layout.comment_submission_attachment_view, this, false)
        view.iconImageView.setColorFilter(ThemePrefs.brandColor)

        val (icon: Int, title: String, subtitle: String?) = when (type) {
            SUBMISSION_TYPE.ONLINE_TEXT_ENTRY -> {
                Triple(R.drawable.vd_document, context.getString(R.string.speedGraderTextSubmission), quotedFromHtml(submission.body))
            }
            SUBMISSION_TYPE.EXTERNAL_TOOL -> {
                Triple(R.drawable.vd_lti, context.getString(R.string.speedGraderExternalToolSubmission), submission.url)
            }
            SUBMISSION_TYPE.DISCUSSION_TOPIC -> {
                Triple(R.drawable.vd_discussion, context.getString(R.string.speedGraderDiscussionSubmission), quotedFromHtml(submission.discussionEntries.firstOrNull()?.message))
            }
            SUBMISSION_TYPE.ONLINE_QUIZ -> {
                Triple(R.drawable.vd_quiz, context.getString(R.string.speedGraderQuizSubmission), context.getString(R.string.speedgraderCommentQuizAttempt, submission.attempt))
            }
            SUBMISSION_TYPE.MEDIA_RECORDING -> {
                val media = submission.mediaComment ?: throw IllegalStateException("Media comment is null for media submission. WHY!?")
                val subtitle = when (media.mediaType!!) {
                    MediaComment.MediaType.AUDIO -> context.getString(R.string.submissionTypeAudio)
                    MediaComment.MediaType.VIDEO -> context.getString(R.string.submissionTypeVideo)
                }
                Triple(R.drawable.vd_media, context.getString(R.string.speedGraderMediaFile), subtitle)
            }
            SUBMISSION_TYPE.ONLINE_URL -> {
                Triple(R.drawable.vd_link, context.getString(R.string.speedGraderUrlSubmission), submission.url)
            }
            else -> Triple(R.drawable.vd_utils_attachment, type.prettyPrint(context), "")
        }

        view.iconImageView.setImageResource(icon)
        view.titleTextView.text = title
        if (subtitle.isNullOrBlank()) {
            view.subtitleTextView.setGone()
        } else {
            view.subtitleTextView.text = subtitle
        }

        view.onClick { EventBus.getDefault().post(SubmissionSelectedEvent(submission)) }
        addView(view)
    }

    @Suppress("DEPRECATION")
    private fun quotedFromHtml(html: String?): String? {
        if (html == null) return null
        return "\"" + Html.fromHtml(html) + "\""
    }

    private fun setupAttachmentLabel() {
        val titleView = AppCompatTextView(context)
        titleView.typeface = Typeface.create("sans-serif-medium", Typeface.ITALIC)
        addView(titleView)
        titleView.text = context.getString(R.string.speedgraderCommentSubmittedFiles)
        titleView.setTextColor(context.getColorCompat(R.color.defaultTextGray))
    }

    private fun setupAttachments() {
        for (attachment in submission.attachments) {
            val view = LayoutInflater.from(context).inflate(R.layout.comment_submission_attachment_view, this, false)
            view.iconImageView.setColorFilter(ThemePrefs.accentColor)
            view.iconImageView.setImageResource(attachment.iconRes)
            view.titleTextView.text = attachment.displayName
            view.subtitleTextView.text = Formatter.formatFileSize(context, attachment.size)
            view.onClick {
                EventBus.getDefault().post(SubmissionSelectedEvent(submission))
                EventBus.getDefault().post(SubmissionFileSelectedEvent(submission.id, attachment))
            }
            (view.layoutParams as LinearLayout.LayoutParams).topMargin = context.DP(4).toInt()
            addView(view)
        }
    }
}
