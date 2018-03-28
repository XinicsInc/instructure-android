/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.canvasapi2.models;

import android.os.Parcel;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.instructure.canvasapi2.utils.APIHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class DiscussionEntry extends CanvasModel<DiscussionEntry> {

    // TODO: Prepend underscore to local-only fields
    private long id;                      // Entry id.
    private boolean unread = false;
    @SerializedName("updated_at")
    private String updatedAt;
    @SerializedName("created_at")
    private String createdAt;
    private DiscussionEntry parent;       // Parent of the entry;
    private DiscussionParticipant author;
    private String description;           // HTML formatted string used for an edge case. Converting header to entry
    @SerializedName("user_id")
    private long userId;                  // Id of the user that posted it.
    @SerializedName("parent_id")
    private long parentId = -1;           // Parent id. -1 if there isn't one.
    private String message;               // HTML message.
    private boolean deleted;              // Whether the author deleted the message. If true, the message will be null.
    private int totalChildren = 0;
    private int unreadChildren = 0;
    private List<DiscussionEntry> replies = new ArrayList<>();
    private List<RemoteFile> attachments = new ArrayList<>();
    @SerializedName("rating_count")
    private int ratingCount;
    @SerializedName("rating_sum")
    private int ratingSum;
    @SerializedName("user_name")
    private String userName;
    @SerializedName("editor_id")
    private long editorId;
    private boolean _hasRated;

    @Override
    public long getId() {
        return id;
    }

    @Nullable
    @Override
    public Date getComparisonDate() {
        return APIHelper.stringToDate(updatedAt);
    }

    @Nullable
    @Override
    public String getComparisonString() {
        return message;
    }

    public void init(DiscussionTopic topic, DiscussionEntry parentEntry) {
        parent = parentEntry;

        HashMap<Long, DiscussionParticipant> participantHashMap = topic.getParticipantsMap();
        DiscussionParticipant discussionParticipant = participantHashMap.get(getUserId());
        if(getUserId() == 0 && getEditorId() != 0) {
            discussionParticipant = participantHashMap.get(getEditorId());
        }
        if(discussionParticipant != null){
            author = discussionParticipant;
        }

        _hasRated = topic.hasRated(id);

        //Get whether or not the topic is unread;
        unread = topic.getUnreadEntriesMap().containsKey(this.getId());

        for(DiscussionEntry reply : replies){
            reply.init(topic,this);

            //Handle total and unread children.
            unreadChildren += reply.getUnreadChildren();
            if (reply.isUnread())
                unreadChildren++;

            totalChildren++;
            totalChildren += reply.getTotalChildren();
        }
    }

    public int getDepth() {
        int depth = 0;
        DiscussionEntry temp = this;

        while (temp.getParent() != null) {
            depth++;
            temp = temp.getParent();
        }

        return depth;
    }

    public void addReply (DiscussionEntry entry){
        if(replies == null) {
            replies = new ArrayList<>();
        }
        replies.add(entry);
    }

    public void addInnerReply(DiscussionEntry parent, DiscussionEntry toAdd) {
        if(!getReplies().isEmpty()) {
            for(DiscussionEntry reply : getReplies()) {
                if(reply.id == parent.id) {
                    reply.addReply(toAdd);
                    break;
                }
            }
        }
    }

    //region Getters

    public long getEditorId() {
        return editorId;
    }

    public boolean isUnread() {
        return unread;
    }

    public Date getUpdatedAt() {
        return APIHelper.stringToDate(updatedAt);
    }

    public Date getCreatedAt() {
        return APIHelper.stringToDate(createdAt);
    }

    public DiscussionEntry getParent() {
        return parent;
    }

    public DiscussionParticipant getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public long getUserId() {
        return userId;
    }

    public long getParentId() {
        return parentId;
    }

    public String getMessage(String localizedDeletedString) {
        if (message == null || message.equals("null")) {
            if (deleted)
                return localizedDeletedString;
            else
                return "";
        }
        return message;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getTotalChildren() {
        return totalChildren;
    }

    public int getUnreadChildren() {
        return unreadChildren;
    }

    public List<DiscussionEntry> getReplies() {
        return replies;
    }

    public List<RemoteFile> getAttachments() {
        return attachments;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int getRatingSum() {
        return ratingSum;
    }

    public String getMessage() {
        return message;
    }

    public String getUserName() {
        return userName;
    }

    public boolean hasRated() {
        return _hasRated;
    }

    //endregion

    //region Setters

    public void setEditorId(long editorId) {
        this.editorId = editorId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = APIHelper.dateToString(updatedAt);
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = APIHelper.dateToString(createdAt);
    }

    public void setParent(DiscussionEntry parent) {
        this.parent = parent;
    }

    public void setAuthor(DiscussionParticipant author) {
        this.author = author;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setTotalChildren(int totalChildren) {
        this.totalChildren = totalChildren;
    }

    public void setUnreadChildren(int unreadChildren) {
        this.unreadChildren = unreadChildren;
    }

    public void setReplies(List<DiscussionEntry> replies) {
        this.replies = replies;
    }

    public void setAttachments(List<RemoteFile> attachments) {
        this.attachments = attachments;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public void setRatingSum(int ratingSum) {
        this.ratingSum = ratingSum;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setHasRated(boolean hasRated) {
        this._hasRated = hasRated;
    }

    //endregion

    public DiscussionEntry() {
    }

    //region Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeByte(this.unread ? (byte) 1 : (byte) 0);
        dest.writeString(this.updatedAt);
        dest.writeString(this.createdAt);
        //can't have a circular reference with parcelable, so it needs to be serializable
        //Will cause a StackOverflowError
        dest.writeSerializable(this.parent);
        dest.writeParcelable(this.author, flags);
        dest.writeString(this.description);
        dest.writeLong(this.userId);
        dest.writeLong(this.parentId);
        dest.writeString(this.message);
        dest.writeByte(this.deleted ? (byte) 1 : (byte) 0);
        dest.writeInt(this.totalChildren);
        dest.writeInt(this.unreadChildren);
        //can't have a circular reference with parcelable, so it needs to be serializable
        //Will cause a StackOverflowError
        dest.writeSerializable((Serializable)this.replies);
        dest.writeTypedList(this.attachments);
        dest.writeInt(this.ratingCount);
        dest.writeInt(this.ratingSum);
        dest.writeString(this.userName);
        dest.writeLong(this.editorId);
        dest.writeInt(this._hasRated ? (byte) 1 : (byte) 0);
    }

    protected DiscussionEntry(Parcel in) {
        this.id = in.readLong();
        this.unread = in.readByte() != 0;
        this.updatedAt = in.readString();
        this.createdAt = in.readString();
        this.parent = (DiscussionEntry)in.readSerializable();
        this.author = in.readParcelable(DiscussionParticipant.class.getClassLoader());
        this.description = in.readString();
        this.userId = in.readLong();
        this.parentId = in.readLong();
        this.message = in.readString();
        this.deleted = in.readByte() != 0;
        this.totalChildren = in.readInt();
        this.unreadChildren = in.readInt();
        this.replies = (List<DiscussionEntry>)in.readSerializable();
        this.attachments = in.createTypedArrayList(RemoteFile.CREATOR);
        this.ratingCount = in.readInt();
        this.ratingSum = in.readInt();
        this.userName = in.readString();
        this.editorId = in.readLong();
        this._hasRated = in.readByte() != 0;
    }

    public static final Creator<DiscussionEntry> CREATOR = new Creator<DiscussionEntry>() {
        @Override
        public DiscussionEntry createFromParcel(Parcel source) {
            return new DiscussionEntry(source);
        }

        @Override
        public DiscussionEntry[] newArray(int size) {
            return new DiscussionEntry[size];
        }
    };

    //endregion
}
