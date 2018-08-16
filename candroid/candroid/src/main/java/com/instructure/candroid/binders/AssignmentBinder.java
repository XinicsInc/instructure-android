/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.binders;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.holders.AssignmentViewHolder;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.pandautils.utils.ColorKeeper;

public class AssignmentBinder extends BaseBinder {

    public static void bind(
            Context context,
            final AssignmentViewHolder holder,
            final Assignment assignment,
            final int courseColor,
            final AdapterToFragmentCallback adapterToFragmentCallback) {

        holder.title.setText(assignment.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterToFragmentCallback.onRowClicked(assignment, holder.getAdapterPosition(), true);
            }
        });

        long courseId = assignment.getCourseId();
        int color = ColorKeeper.getOrGenerateColor(CanvasContext.makeContextId(CanvasContext.Type.COURSE, courseId));

        Submission submission = assignment.getSubmission();

        if(assignment.isMuted()){
            //mute that score
            holder.points.setVisibility(View.GONE);
        } else {
            holder.points.setVisibility(View.VISIBLE);
            setupGradeText(context, holder.points, assignment, submission, courseColor);
        }


        final int drawable = getAssignmentIcon(assignment);
        holder.icon.setImageDrawable(ColorKeeper.getColoredDrawable(context, drawable, color));

        if (assignment.getDueAt() != null) {
            holder.date.setText(DateHelper.createPrefixedDateTimeString(context, R.string.toDoDue, assignment.getDueAt()));
        } else {
            holder.date.setText(context.getResources().getString(R.string.toDoNoDueDate));
        }

        String description; //set description to assignment description or excused
        if(submission != null && submission.isExcused()){
            description = context.getString(R.string.excusedAssignment);
            holder.description.setTypeface(null, Typeface.BOLD);
        } else {
            description = getHtmlAsText(assignment.getDescription());
            holder.description.setTypeface(null, Typeface.NORMAL);
        }

        setCleanText(holder.description, description);
        if(TextUtils.isEmpty(description)) {
            setGone(holder.description);
        } else {
            setVisible(holder.description);
        }

    }
}
