/*
 * Copyright (C) 2016 - present  Instructure, Inc.
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

package com.instructure.parentapp.binders;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.instructure.canvasapi2.models.Alert;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Utils;
import com.instructure.parentapp.BuildConfig;
import com.instructure.parentapp.R;
import com.instructure.parentapp.adapter.AlertListRecyclerAdapter;
import com.instructure.parentapp.holders.AlertViewHolder;
import com.instructure.parentapp.interfaces.AdapterToFragmentBadgeCallback;

public class AlertBinder extends BaseBinder {

    public static void bind(
            Context context,
            final Alert item,
            final AlertViewHolder holder,
            final AdapterToFragmentBadgeCallback<Alert> adapterToFragmentCallback,
            final AlertListRecyclerAdapter.ItemDismissedInterface itemDismissedInterface){

        holder.title.setText(item.getTitle());
        Utils.testSafeContentDescription(holder.title,
                String.format(context.getString(R.string.alert_title_content_desc), holder.getAdapterPosition()),
                holder.title.getText().toString(),
                BuildConfig.IS_TESTING);

        if(item.getCourse() != null) {
            final String courseName = item.getCourse().getName();
            if(TextUtils.isEmpty(courseName)) {
                holder.description.setText(courseName);
                holder.description.setVisibility(View.VISIBLE);
            } else {
                holder.description.setVisibility(View.GONE);
            }
        } else {
            holder.description.setVisibility(View.GONE);
        }

        if(item.getActionDate() != null){
            String headerText = DateHelper.getDateNoYearWeekDayAndTime(context, item.getActionDate());
            holder.date.setText(headerText);
            holder.date.setVisibility(View.VISIBLE);
            Utils.testSafeContentDescription(holder.date,
                    String.format(context.getString(R.string.date_text_content_desc), holder.getAdapterPosition()),
                    headerText,
                    BuildConfig.IS_TESTING);
        } else {
            holder.date.setText("");
            holder.date.setVisibility(View.GONE);
        }

        holder.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemDismissedInterface.itemDismissed(item, holder);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterToFragmentCallback.onRowClicked(item, holder.getAdapterPosition(), false);
            }
        });

        switch(item.getAlertType()) {
            case COURSE_ANNOUNCEMENT:
            case INSTITUTION_ANNOUNCEMENT:
                if (item.isMarkedRead()) {
                    holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_cv_announcment_light));
                } else {
                    holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_cv_announcment_white));
                }
                holder.iconWrapper.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.light_blue_circle_background));
                break;
            case ASSIGNMENT_GRADE_HIGH:
            case COURSE_GRADE_HIGH:
                if (item.isMarkedRead()) {
                    holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_cv_star_light));
                } else {
                    holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_cv_star_white));
                }
                holder.iconWrapper.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.circle_background));
                break;
            case COURSE_GRADE_LOW:
            case ASSIGNMENT_GRADE_LOW:
            case ASSIGNMENT_MISSING:
                if (item.isMarkedRead()) {
                    holder.icon.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.ic_warning_white_18dp, context.getResources().getColor(R.color.gray)));
                } else {
                    holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_warning_white_18dp));
                }
                holder.iconWrapper.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.red_circle_background));
                break;
        }

        if(item.isMarkedRead()){
            holder.iconWrapper.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.read_circle_background));
        }
   }
}
