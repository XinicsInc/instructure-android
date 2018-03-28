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
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.NotificationListRecyclerAdapter;
import com.instructure.candroid.holders.NotificationViewHolder;
import com.instructure.candroid.interfaces.NotificationAdapterToFragmentCallback;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.StreamItem;
import com.instructure.pandautils.utils.ColorKeeper;

public class NotificationBinder extends BaseBinder {

    public static void bind(
            final Context context,
            final NotificationViewHolder holder,
            final StreamItem item,
            final NotificationListRecyclerAdapter.NotificationCheckboxCallback checkboxCallback,
            final NotificationAdapterToFragmentCallback<StreamItem> adapterToFragmentCallback) {

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkboxCallback.isEditMode()){
                    checkboxCallback.onCheckChanged(item, !item.isChecked(), holder.getAdapterPosition());
                } else {
                    adapterToFragmentCallback.onRowClicked(item, holder.getAdapterPosition(), true);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                checkboxCallback.onCheckChanged(item, !item.isChecked(), holder.getAdapterPosition());
                return true;
            }
        });

        holder.title.setText(item.getTitle(context));

        //Course Name
        String courseName = "";
        if (item.getContextType() == CanvasContext.Type.COURSE && item.getCanvasContext() != null) {
            courseName = item.getCanvasContext().getSecondaryName();
        } else if(item.getContextType() == CanvasContext.Type.GROUP && item.getCanvasContext() != null) {
            courseName = item.getCanvasContext().getName();
        } else {
            courseName = "";
        }
        holder.course.setText(courseName);
        holder.course.setTextColor(ColorKeeper.getOrGenerateColor(item.getCanvasContext()));

        //Description
        if (!TextUtils.isEmpty(item.getMessage(context))) {
            holder.description.setText(getHtmlAsText(item.getMessage(context)));
            setVisible(holder.description);
        } else {
            holder.description.setText("");
            setGone(holder.description);
        }

        if(item.isChecked()) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.lightgray));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.canvasBackgroundWhite));
        }

        //Icon
        int drawableResId = 0;
        switch (item.getType()) {
            case DISCUSSION_TOPIC:
                drawableResId = R.drawable.vd_discussion;
                holder.icon.setContentDescription(context.getString(R.string.discussionIcon));
                break;
            case ANNOUNCEMENT:
                drawableResId = R.drawable.vd_announcement;
                holder.icon.setContentDescription(context.getString(R.string.announcementIcon));
                break;
            case SUBMISSION:
                drawableResId = R.drawable.vd_assignment;
                holder.icon.setContentDescription(context.getString(R.string.assignmentIcon));

                //need to prepend "Grade" in the message if there is a valid score
                if (item.getScore() != -1.0) {
                    //if the submission has a grade (like a letter or percentage) display it
                    if (item.getGrade() != null
                            && !item.getGrade().equals("")
                            && !item.getGrade().equals("null")) {
                        holder.description.setText(context.getResources().getString(R.string.grade) + ": " + item.getGrade());
                    } else {
                        holder.description.setText(context.getResources().getString(R.string.grade) + holder.description.getText());
                    }
                }
                break;
            case CONVERSATION:
                drawableResId = R.drawable.vd_inbox;
                holder.icon.setContentDescription(context.getString(R.string.conversationIcon));
                break;
            case MESSAGE:
                if (item.getContextType() == CanvasContext.Type.COURSE) {
                    drawableResId = R.drawable.vd_assignment;
                    holder.icon.setContentDescription(context.getString(R.string.assignmentIcon));
                } else if (item.getNotificationCategory().toLowerCase().contains("assignment graded")) {
                    drawableResId = R.drawable.vd_grades;
                    holder.icon.setContentDescription(context.getString(R.string.gradesIcon));
                } else {
                    drawableResId = R.drawable.vd_profile;
                    holder.icon.setContentDescription(context.getString(R.string.defaultIcon));
                }
                break;
            case CONFERENCE:
                drawableResId = R.drawable.vd_conferences;
                holder.icon.setContentDescription(context.getString(R.string.icon));
                break;
            case COLLABORATION:
                drawableResId = R.drawable.vd_collaborations;
                holder.icon.setContentDescription(context.getString(R.string.icon));
                break;
            case COLLECTION_ITEM:
            default:
                drawableResId = R.drawable.vd_peer_review;
                break;
        }

        if (item.getCanvasContext() != null) {
            int courseColor = ColorKeeper.getOrGenerateColor(item.getCanvasContext());
            Drawable drawable = ColorKeeper.getColoredDrawable(context, drawableResId, courseColor);
            holder.icon.setImageDrawable(drawable);
        }

        //Read/Unread
        if (item.isReadState()) {
            holder.title.setTypeface(null, Typeface.NORMAL);
        } else {
            holder.title.setTypeface(null, Typeface.BOLD);
        }
    }
}
