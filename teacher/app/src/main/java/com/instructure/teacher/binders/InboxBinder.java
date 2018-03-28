/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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
 */

package com.instructure.teacher.binders;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.instructure.canvasapi2.models.BasicUser;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Conversation;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.pandautils.utils.ColorUtils;
import com.instructure.pandautils.utils.ProfileUtils;
import com.instructure.pandautils.utils.ThemePrefs;
import com.instructure.teacher.R;
import com.instructure.teacher.adapters.StudentContextFragment;
import com.instructure.teacher.holders.InboxViewHolder;
import com.instructure.teacher.interfaces.AdapterToFragmentCallback;
import com.instructure.interactions.router.Route;
import com.instructure.teacher.router.RouteMatcher;

import java.util.Date;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

public class InboxBinder extends BaseBinder {

    public static void bind(final Context context, final Conversation conversation, final InboxViewHolder holder, final AdapterToFragmentCallback<Conversation> callback) {
        long myUserId = ApiPrefs.getUser().getId();

        setVisible(holder.userName);

        String messageTitle = getConversationTitle(context, myUserId, conversation);

        ProfileUtils.loadAvatarsForConversation(holder.avatar1, conversation, new Function2<BasicUser, CanvasContext, Unit>() {
            @Override
            public Unit invoke(BasicUser user, CanvasContext canvasContext) {
                Bundle bundle = StudentContextFragment.makeBundle(user.getId(), canvasContext.getId(), false);
                RouteMatcher.route(context, new Route(StudentContextFragment.class, null, bundle));
                return null;
            }
        });

        holder.userName.setText(messageTitle);
        holder.message.setText(conversation.getLastMessagePreview());

        if (conversation.hasAttachments() || conversation.hasMedia()) {
            holder.attachment.setImageDrawable(ColorUtils.colorIt(context.getResources().getColor(R.color.canvasTextMedium), holder.attachment.getDrawable()));
            holder.attachment.setVisibility(View.VISIBLE);
        } else {
            holder.attachment.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(conversation.getLastAuthoredMessageAt())) {
            holder.date.setText(getParsedDate(context, conversation.getLastAuthoredMessageAt()));
        } else {
            holder.date.setText(getParsedDate(context, conversation.getLastMessageAt()));
        }

        if(!TextUtils.isEmpty(conversation.getSubject())){
            setVisible(holder.subject);
            holder.subject.setText(conversation.getSubject());
            holder.message.setMaxLines(1);
        } else {
            setGone(holder.subject);
            holder.message.setMaxLines(2);
        }

        if (conversation.getWorkflowState() == Conversation.WorkflowState.UNREAD) {
            setVisible(holder.unreadMark);
            holder.unreadMark.setImageDrawable(ColorUtils.colorIt(ThemePrefs.getAccentColor(), holder.unreadMark.getDrawable()));
        } else {
            setGone(holder.unreadMark);
        }

        if(conversation.isStarred()) {
            holder.star.setImageDrawable(ColorUtils.colorIt(ThemePrefs.getBrandColor(), holder.star.getDrawable()));
            setVisible(holder.star);
        } else {
            setGone(holder.star);
        }

        ifHasTextSetVisibleElseGone(holder.message);
        ifHasTextSetVisibleElseGone(holder.date);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onRowClicked(conversation, holder.getAdapterPosition());
            }
        });
    }

    private static String getParsedDate(Context context, String messageDate){
        Date date = APIHelper.stringToDate(messageDate);
        return DateHelper.dateToDayMonthYearString(context, date);
    }

    public static String getConversationTitle(Context context, long myUserId, Conversation conversation) {

        if(conversation.isMonologue(myUserId)) {
            return context.getString(R.string.monologue);
        }

        List<BasicUser> users = conversation.getParticipants();
        if(users == null || users.size() == 0) {
            return "";
        } else {
            if(users.size() == 1) {
                return users.get(0).getName();
            } else if(users.size() == 2) {
                return users.get(0).getName() + ", " + users.get(1).getName();
            } else {
                return String.format(context.getString(R.string.conversation_message_title), users.get(0).getName(), Integer.toString(users.size() - 1));
            }
        }
    }
    
}
