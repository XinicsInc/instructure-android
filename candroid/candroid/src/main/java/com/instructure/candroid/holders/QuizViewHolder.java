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

package com.instructure.candroid.holders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instructure.candroid.R;

public class QuizViewHolder extends RecyclerView.ViewHolder {

    public TextView title, description, status, questions, date, points, bulletPointsAndQuestions, bulletStatusAndDate;
    public ImageView icon;
    public View dateContainer, pointsContainer;

    public QuizViewHolder(View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        description = itemView.findViewById(R.id.description);
        status = itemView.findViewById(R.id.status);
        questions = itemView.findViewById(R.id.questions);
        date = itemView.findViewById(R.id.date);
        points = itemView.findViewById(R.id.points);
        icon = itemView.findViewById(R.id.icon);
        bulletPointsAndQuestions = itemView.findViewById(R.id.bulletPointsAndQuestions);
        bulletStatusAndDate = itemView.findViewById(R.id.bulletStatusAndDate);
        dateContainer = itemView.findViewById(R.id.dateContainer);
        pointsContainer = itemView.findViewById(R.id.pointsContainer);
    }

    public static int holderResId(){
        return R.layout.viewholder_quiz;
    }
}
