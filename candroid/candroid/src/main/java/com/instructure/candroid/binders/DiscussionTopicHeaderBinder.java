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
import android.text.TextUtils;
import android.view.View;
import com.instructure.candroid.R;
import com.instructure.candroid.holders.DiscussionTopicHeaderViewHolder;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.canvasapi2.models.DiscussionTopicHeader;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.ColorUtils;

import java.util.Calendar;
import java.util.Date;

@Deprecated
public class DiscussionTopicHeaderBinder extends BaseBinder {

    public static void bind(
            final DiscussionTopicHeaderViewHolder holder,
            final DiscussionTopicHeader item,
            final Context context,
            final int courseColor,
            final boolean isDiscussions,
            final AdapterToFragmentCallback<DiscussionTopicHeader> adapterToFragmentCallback){

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterToFragmentCallback.onRowClicked(item, holder.getAdapterPosition(), true);
            }
        });

        holder.title.setText(item.getTitle());

        if(TextUtils.isEmpty(item.getMessage())) {
            setGone(holder.description);
        } else {
            holder.description.setText(getHtmlAsText(item.getMessage()));
            setVisible(holder.description);
        }

        if(item.isPinned() || (item.isLockedForUser())) {
            if(item.isLockedForUser()) {
                Drawable pinIcon = ColorUtils.colorIt(
                        context.getResources().getColor(R.color.canvasTextMedium),
                        context.getResources().getDrawable(R.drawable.vd_lock));
                holder.pin.setImageDrawable(pinIcon);
            } else {
                Drawable pinIcon = ColorUtils.colorIt(
                        context.getResources().getColor(R.color.canvasTextMedium),
                        context.getResources().getDrawable(R.drawable.vd_pin));
                holder.pin.setImageDrawable(pinIcon);
            }
            setVisible(holder.pin);
        } else {
            setGone(holder.pin);
        }

        //only show the box if there are unread discussions
        if(item.getUnreadCount() == 0 && item.getStatus() == DiscussionTopicHeader.ReadState.READ) {
            holder.title.setTypeface(Typeface.DEFAULT);
        } else if(item.getStatus() == DiscussionTopicHeader.ReadState.UNREAD) {
            //if there are no replies but the item is unread, we want to indicate that by making the title bold
            holder.title.setTypeface(Typeface.DEFAULT_BOLD);
        } else if(!item.isPublished()) {
            //if the item isn't published, there is a chance that there are unread teacher replies that will make the draft label look
            //bad because it is under the unread count. A teacher can't unpublish a discussion with student replies
        } else {
            holder.title.setTypeface(Typeface.DEFAULT_BOLD);
        }

        if(item.isPublished()) {
            if (item.getLastReplyAt() != null) {
                holder.lastPost.setTypeface(Typeface.DEFAULT);
                holder.lastPost.setTextColor(context.getResources().getColor(R.color.canvasTextMedium));
                setVisible(holder.lastPost);
                Date lastReply = item.getLastReplyAt();

                Calendar day = Calendar.getInstance();
                day.setTime(lastReply);
                if(day.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR)) {
                    holder.lastPost.setText(DateHelper.createPrefixedDateString(context, R.string.lastDiscussionPost, lastReply));
                } else {
                    holder.lastPost.setText(DateHelper.createPrefixedShortDateString(context, R.string.lastDiscussionPost, lastReply));
                }
            } else {
                setGone(holder.lastPost);
            }

            //Does a check if it's locked for the user and sets an explanation if one exists.
            if(item.isLockedForUser() && !TextUtils.isEmpty(item.getLockExplanation())) {
                holder.lastPost.setText(item.getLockExplanation());
                setVisible(holder.lastPost);
            }

        } else {
            setVisible(holder.lastPost);
            holder.lastPost.setTypeface(Typeface.DEFAULT_BOLD);
            holder.lastPost.setTextColor(courseColor);
            holder.lastPost.setText(R.string.draft);
        }

        if(isDiscussions) {
            if (item.getUnreadCount() > 0) {
                Drawable drawable = ColorKeeper.getColoredDrawable(context, R.drawable.vd_discussion_fill, courseColor);
                holder.icon.setImageDrawable(drawable);
            } else {
                Drawable drawable = ColorKeeper.getColoredDrawable(context, R.drawable.vd_discussion, courseColor);
                holder.icon.setImageDrawable(drawable);
            }
        } else {
            Drawable drawable = ColorKeeper.getColoredDrawable(context, R.drawable.vd_announcement, courseColor);
            holder.icon.setImageDrawable(drawable);
        }
    }
}
