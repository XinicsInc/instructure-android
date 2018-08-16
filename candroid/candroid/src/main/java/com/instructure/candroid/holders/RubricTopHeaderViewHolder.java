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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.instructure.candroid.R;

public class RubricTopHeaderViewHolder extends RecyclerView.ViewHolder {
    public TextView pointsText;
    public TextView gradeText;
    public TextView mutedText;
    public TextView finalGrade;
    public TextView latePenalty;
    public RelativeLayout latePolicy;

    public RubricTopHeaderViewHolder(View itemView) {
        super(itemView);
        pointsText = itemView.findViewById(R.id.currentPoints);
        gradeText = itemView.findViewById(R.id.currentGrade);
        mutedText = itemView.findViewById(R.id.mutedText);
        finalGrade = itemView.findViewById(R.id.finalGrade);
        latePenalty = itemView.findViewById(R.id.latePenalty);
        latePolicy = itemView.findViewById(R.id.latePolicy);
    }

    public static int holderResId() {
        return R.layout.viewholder_rubric_top_header;
    }
}
