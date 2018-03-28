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
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.holders.QuizViewHolder;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.canvasapi2.models.Quiz;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.pandautils.utils.ColorKeeper;

import java.util.Date;

public class QuizBinder extends BaseBinder{

    public static void bind(
            final QuizViewHolder holder,
            final Quiz item,
            final AdapterToFragmentCallback<Quiz> adapterToFragmentCallback,
            final Context context,
            final int courseColor) {

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterToFragmentCallback.onRowClicked(item, holder.getAdapterPosition(), true);
            }
        });

        holder.title.setText(item.getTitle());

        String description = getHtmlAsText(item.getDescription());
        if(!TextUtils.isEmpty(description)) {
            holder.description.setText(description);
            setVisible(holder.description);
        } else {
            holder.description.setText("");
            setGone(holder.description);
        }

        Drawable drawable = ColorKeeper.getColoredDrawable(context, R.drawable.vd_quiz, courseColor);
        holder.icon.setImageDrawable(drawable);

        setupStatusAndDate(context, holder, item, courseColor);
        setupPointsAndQuestions(context, holder, item);
    }

    private static void setupStatusAndDate(final Context context, final QuizViewHolder holder, final Quiz item, final int courseColor) {
        Date dueDate = item.getDueAt();

        boolean hasDate, hasStatus;

        if (dueDate != null) {
            holder.date.setText(DateHelper.createPrefixedDateTimeString(context, R.string.toDoDue, dueDate));
            hasDate = true;
        } else {
            holder.date.setText("");
            hasDate = false;
        }

        Date lockDate = item.getLockAt();
        Date today = new Date();
        if((lockDate != null && today.after(lockDate)) || (item.isRequireLockdownBrowserForResults())) {
            holder.status.setText(R.string.closed);
            holder.status.setTextColor(courseColor);
            hasStatus = true;
        } else {
            holder.status.setText("");
            hasStatus = false;
        }

        if(hasDate && hasStatus) {
            holder.dateContainer.setVisibility(View.VISIBLE);
            holder.bulletStatusAndDate.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.VISIBLE);
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.bulletStatusAndDate.setVisibility(View.GONE);
            holder.status.setVisibility(hasStatus ? View.VISIBLE : View.GONE);
            holder.date.setVisibility(hasDate ? View.VISIBLE : View.GONE);

            if(!hasDate && !hasStatus) {
                holder.dateContainer.setVisibility(View.GONE);
            } else {
                holder.dateContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private static void setupPointsAndQuestions(final Context context, final QuizViewHolder holder, final Quiz item) {

        boolean hasPoints;

        String points = item.getPointsPossible();
        if (!TextUtils.isEmpty(points)) {
            setGrade(null, Double.parseDouble(points), holder.points, context);
            hasPoints = true;
        } else {
            holder.points.setText("");
            hasPoints = false;
        }

        final int questionCount = item.getQuestionCount();
        final String numberOfQuestions = context.getResources().getQuantityString(R.plurals.question_count, questionCount, questionCount);
        holder.questions.setText(numberOfQuestions);

        if(hasPoints) {
            holder.bulletPointsAndQuestions.setVisibility(View.VISIBLE);
            holder.points.setVisibility(View.VISIBLE);
        } else {
            holder.bulletPointsAndQuestions.setVisibility(View.GONE);
            holder.points.setVisibility(View.GONE);
        }
    }
}
