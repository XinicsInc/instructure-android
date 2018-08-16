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
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.GradesListRecyclerAdapter;
import com.instructure.candroid.dialog.WhatIfDialogStyled;
import com.instructure.candroid.holders.GradeViewHolder;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;

public class GradeBinder extends BaseBinder {

    public static void bind(
            final GradeViewHolder holder,
            final Context context,
            final int courseColor,
            final Assignment assignment,
            final Course course,
            final boolean isEdit,
            final WhatIfDialogStyled.WhatIfDialogCallback dialogStyled,
            final AdapterToFragmentCallback<Assignment> adapterToFragmentCallback,
            final GradesListRecyclerAdapter.SetSelectedItemCallback selectedItemCallback) {

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterToFragmentCallback.onRowClicked(assignment, holder.getAdapterPosition(), true);
                selectedItemCallback.setSelected(holder.getAdapterPosition());
            }
        });

        holder.title.setText(assignment.getName());

        final int drawable = getAssignmentIcon(assignment);
        holder.icon.setImageDrawable(ColorKeeper.getColoredDrawable(context, drawable, courseColor));

        holder.pendingReviewIcon.setVisibility(View.GONE);

        if(assignment.isMuted()){
            //mute that score
            holder.points.setVisibility(View.GONE);
        } else {
            Submission submission = assignment.getSubmission();
            if (submission != null && Const.PENDING_REVIEW.equals(submission.getWorkflowState())) {
                holder.points.setVisibility(View.GONE);
                holder.pendingReviewIcon.setVisibility(View.VISIBLE);
                holder.pendingReviewIcon.setColorFilter(courseColor);
            } else {
                holder.points.setVisibility(View.VISIBLE);
                setupGradeText(context, holder.points, assignment, submission, courseColor);
            }
        }



        //configures whatIf editing boxes and listener for dialog
        if (isEdit) {
            holder.edit.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.vd_edit, Color.BLACK));
            holder.edit.setVisibility(View.VISIBLE);
            holder.edit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WhatIfDialogStyled.show(
                            (FragmentActivity)context,
                            assignment.getPointsPossible(),
                            dialogStyled,
                            assignment,
                            ColorKeeper.getOrGenerateColor(course), holder.getAdapterPosition());
                }
            });
        } else {
            holder.edit.setVisibility(View.GONE);
        }

        if (assignment.getDueAt() != null) {
            holder.date.setText(DateHelper.getDayMonthDateString(context, assignment.getDueAt()));
        } else {
            holder.date.setText("");
        }
    }
}
