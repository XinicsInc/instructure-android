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

package com.instructure.candroid.holders

import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.interfaces.CourseAdapterToFragmentCallback
import com.instructure.canvasapi2.managers.EnrollmentManager
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Enrollment
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.viewholder_course_invite_card.view.*
import kotlinx.coroutines.experimental.delay

class CourseInvitationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun holderResId(): Int = R.layout.viewholder_course_invite_card
    }

    fun bind(
            enrollment: Enrollment,
            course: Course,
            callback: CourseAdapterToFragmentCallback
    ) = with(itemView) {
        val section = course.sections.find { it.id == enrollment.courseSectionId }

        inviteTitle.text = context.getString(R.string.courseInviteTitle)
        DrawableCompat.setTint(DrawableCompat.wrap(background), ContextCompat.getColor(context, R.color.notificationTintInvite))
        inviteDetails.setVisible()
        buttonContainer.setVisible()
        inviteProgressBar.setGone()
        inviteDetails.text = listOfNotNull(course.name, section?.name).distinct().joinToString(", ")

        fun handleInvitation(accepted: Boolean) {
            buttonContainer.setInvisible()
            inviteProgressBar.setVisible()
            tryWeave {
                awaitApi<Void> { EnrollmentManager.handleInvite(enrollment.courseId, enrollment.id, accepted, it) }
                inviteDetails.setGone()
                buttonContainer.setGone()
                inviteProgressBar.setGone()
                inviteTitle.text = getContext().getText(if (accepted) R.string.inviteAccepted else R.string.inviteDeclined)
                announceForAccessibility(inviteTitle.text)
                delay(2000)
                callback.onHandleCourseInvitation(course, accepted)
            } catch {
                toast(R.string.errorOccurred)
                inviteDetails.setVisible()
                buttonContainer.setVisible()
                inviteProgressBar.setGone()
            }
        }

        acceptButton.onClick { handleInvitation(true) }
        declineButton.onClick { handleInvitation(false) }
    }

}
