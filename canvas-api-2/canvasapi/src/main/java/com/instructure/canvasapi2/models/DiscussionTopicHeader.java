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
import com.instructure.canvasapi2.utils.DateHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class DiscussionTopicHeader extends CanvasModel<DiscussionTopicHeader> {

    public enum ReadState { READ, UNREAD }
    public enum DiscussionType { UNKNOWN, SIDE_COMMENT, THREADED }

    private long id;                //Discussion Topic Id;
    @SerializedName("discussion_type")
    private String discussionType; //Type of discussion (side_comment or threaded).
    private String title;           //Discussion title
    private String message;         //HTML content
    @SerializedName("html_url")
    private String htmlUrl;         //URL to the topic on canvas.

    //Only one of the following two will be filled out. the other will be null.
    //If posted_at isn't null, it represents when the discussion WAS posted.
    //If delayed_post_at isn't null, it represents when the discussion WILL be posted.
    @SerializedName("posted_at")
    private Date postedAt;
    @SerializedName("delayed_post_at")
    private Date delayedPostAt;
    @SerializedName("last_reply_at")
    private Date lastReplyAt;           // Last response to the thread.
    @SerializedName("require_initial_post")
    private boolean requireInitialPost;   // Whether or not users are required to post before they can respond to comments.
    @SerializedName("discussion_subentry_count")
    private int discussionSubentryCount;  // The count of entries in the topic.
    @SerializedName("read_state")
    private String readState;              // Whether or not the topic has been read yet.
    @SerializedName("unread_count")
    private int unreadCount;               // Number of unread messages.
    private int position;                   // If topic is pinned it'll have a position
    @SerializedName("assignment_id")
    private long assignmentId;             // The unique identifier of the assignment if the topic is for grading, otherwise null.
    private boolean locked;                 // Whether or not the discussion is 'closed for comments'.
    @SerializedName("locked_for_user")
    private boolean lockedForUser;        // whether or not this is locked for students to see.
    @SerializedName("lock_explanation")
    private String lockExplanation;        // (Optional) An explanation of why this is locked for the user. Present when locked_for_user is true.
    private boolean pinned;                 // whether or not the discussion has been "pinned" by an instructor
    private DiscussionParticipant author;   // The user that started the thread.
    @SerializedName("podcast_url")
    private String podcastUrl;             // If the topic is a podcast topic this is the feed url for the current user.
    @SerializedName("group_category_id")
    private String groupCategoryId;

    // If true, this topic is an announcement. This requires announcement-posting permissions.
    @SerializedName("is_announcement")
    private boolean announcement;

    // If the topic is for grading and a group assignment this will
    // point to the original topic in the course.
    //String maybe?
    @SerializedName("root_topic_id")
    private long rootTopicId;

    // A list of topic_ids for the group discussions the user is a part of.
    @SerializedName("topic_children")
    private List<Long> topicChildren = new ArrayList<>();

    //List of file attachments
    private List<RemoteFile> attachments = new ArrayList<>();

    public boolean unauthorized;
    private DiscussionTopicPermission permissions;
    private Assignment assignment;
    @SerializedName("lock_info")
    private LockInfo lockInfo;
    private boolean published;              //Whether this discussion topic is published (true) or draft state (false)
    @SerializedName("allow_rating")
    private boolean allowRating;           //Whether or not users can rate entries in this topic.
    @SerializedName("only_graders_can_rate")
    private boolean onlyGradersCanRate;  //Whether or not grade permissions are required to rate entries.
    @SerializedName("sort_by_rating")
    private boolean sortByRating;         //Whether or not entries should be sorted by rating.

    // Context code. This is only set when getting DiscussionTopicHeaders from the announcements API
    @SerializedName("context_code")
    private String contextCode;

    private boolean subscribed;

    @SerializedName("lock_at")
    private Date lockAt;

    @SerializedName("user_can_see_posts")
    private boolean userCanSeePosts = true;

    @Override
    public long getId() {
        return id;
    }

    @Override
    @Nullable
    public Date getComparisonDate() {
        if(lastReplyAt != null) return lastReplyAt;
        else return postedAt;
    }

    @Override
    public String getComparisonString() {
        return title;
    }

    public DiscussionEntry convertToDiscussionEntry(String localizedGradedDiscussion, String localizedPointsPossible) {
        DiscussionEntry discussionEntry = new DiscussionEntry();
        discussionEntry.setMessage(this.message);
        discussionEntry.setParent(null);
        discussionEntry.setParentId(-1);
        discussionEntry.setReplies(new ArrayList<DiscussionEntry>());

        String description = "";
        if(assignment != null) {
            description = localizedGradedDiscussion;
            if(assignment.getPointsPossible() > 0)
                description += "<br>"+Double.toString(assignment.getPointsPossible()) + " " + localizedPointsPossible;
        }
        discussionEntry.setDescription(description);

        discussionEntry.setMessage(this.getMessage());

        if(this.getLastReplyAt() != null) {
            discussionEntry.setUpdatedAt(this.getLastReplyAt());
        } else if(this.getPostedAt() != null) {
            discussionEntry.setUpdatedAt(this.getPostedAt());
        } else {
            discussionEntry.setUpdatedAt(this.getDelayedPostAt());
        }

        discussionEntry.setAuthor(author);

        discussionEntry.setAttachments(this.getAttachments());

        discussionEntry.setUnread(this.getStatus() == ReadState.UNREAD);

        return discussionEntry;
    }

    //region Getters

    public DiscussionType getType() {
        if("side_comment".equals(this.discussionType)){
            return DiscussionType.SIDE_COMMENT;
        } else if ("threaded".equals(this.discussionType)){
            return DiscussionType.THREADED;
        }
        return DiscussionType.UNKNOWN;
    }

    public String getDiscussionType() {
        return discussionType;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    @Nullable
    public Date getPostedAt() {
        return postedAt;
    }

    @Nullable
    public Date getDelayedPostAt() {
        return delayedPostAt;
    }

    @Nullable
    public Date getLastReplyAt() {
        return lastReplyAt;
    }

    public boolean isRequireInitialPost() {
        return requireInitialPost;
    }

    public int getDiscussionSubentryCount() {
        return discussionSubentryCount;
    }

    public String getReadState() {
        return readState;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public int getPosition() {
        return position;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isLockedForUser() {
        return lockedForUser;
    }

    public boolean isAnnouncement() {
        return announcement;
    }

    public String getLockExplanation() {
        return lockExplanation;
    }

    public boolean isPinned() {
        return pinned;
    }

    public DiscussionParticipant getAuthor() {
        return author;
    }

    public String getPodcastUrl() {
        return podcastUrl;
    }

    public String getGroupCategoryId() {
        return groupCategoryId;
    }

    public long getRootTopicId() {
        return rootTopicId;
    }

    public List<Long> getTopicChildren() {
        return topicChildren;
    }

    public List<RemoteFile> getAttachments() {
        return attachments;
    }

    public boolean isUnauthorized() {
        return unauthorized;
    }

    public DiscussionTopicPermission getPermissions() {
        return permissions;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public LockInfo getLockInfo() {
        return lockInfo;
    }

    public boolean isPublished() {
        return published;
    }

    public boolean isAllowRating() {
        return allowRating;
    }

    public boolean isOnlyGradersCanRate() {
        return onlyGradersCanRate;
    }

    public boolean isSortByRating() {
        return sortByRating;
    }

    public boolean isSubscribed() { return subscribed; }

    public ReadState getStatus() {
        if("read".equals(readState)) {
            return ReadState.READ;
        } else if ("unread".equals(readState)) {
            return ReadState.UNREAD;
        } else {
            return ReadState.UNREAD;
        }
    }

    public String getContextCode() {
        return contextCode;
    }

    public Date getLockAt() {
        return lockAt;
    }

    public void setUserCanSeePosts(boolean userCanSeePosts) {
        this.userCanSeePosts = userCanSeePosts;
    }

    //endregion

    //region Setters

    public void setId(long id) {
        this.id = id;
    }

    public void setDiscussionType(String discussionType) {
        this.discussionType = discussionType;
    }

    public void setType(DiscussionType type) {
        if(type == DiscussionType.SIDE_COMMENT) {
            this.discussionType = "side_comment";
        } else if(type == DiscussionType.THREADED) {
            this.discussionType = "threaded";
        } else {
            this.discussionType = "unknown";
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public void setPostedAt(String postedAt) {
        this.postedAt = DateHelper.stringToDate(postedAt);
    }

    public void setDelayedPostAt(String delayedPostAt) {
        this.delayedPostAt = DateHelper.stringToDate(delayedPostAt);
    }

    public void setDelayedPostAtDate(Date delayedPostAt) {
        this.delayedPostAt = delayedPostAt;
    }

    public void setLastReplyAt(String lastReplyAt) {
        this.lastReplyAt = DateHelper.stringToDate(lastReplyAt);
    }

    public void setRequireInitialPost(boolean requireInitialPost) {
        this.requireInitialPost = requireInitialPost;
    }

    public void setDiscussionSubentryCount(int discussionSubentryCount) {
        discussionSubentryCount = discussionSubentryCount;
    }

    public void incrementDiscussionSubentryCount() {
        discussionSubentryCount += 1;
    }

    public void decrementDiscussionSubentryCount() {
        discussionSubentryCount -= 1;
        if(discussionSubentryCount < 0) { discussionSubentryCount = 0; }
    }

    public void setReadState(String readState) {
        this.readState = readState;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setLockedForUser(boolean lockedForUser) {
        this.lockedForUser = lockedForUser;
    }

    public void setLockExplanation(String lockExplanation) {
        this.lockExplanation = lockExplanation;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void setAuthor(DiscussionParticipant author) {
        this.author = author;
    }

    public void setPodcastUrl(String podcastUrl) {
        this.podcastUrl = podcastUrl;
    }

    public void setGroupCategoryId(String groupCategoryId) {
        this.groupCategoryId = groupCategoryId;
    }

    public void setAnnouncement(boolean isAnnouncement) {
        this.announcement = isAnnouncement;
    }

    public void setRootTopicId(long rootTopicId) {
        this.rootTopicId = rootTopicId;
    }

    public void setTopicChildren(List<Long> topicChildren) {
        this.topicChildren = topicChildren;
    }

    public void setAttachments(List<RemoteFile> attachments) {
        this.attachments = attachments;
    }

    public void setUnauthorized(boolean unauthorized) {
        this.unauthorized = unauthorized;
    }

    public void setPermissions(DiscussionTopicPermission permissions) {
        this.permissions = permissions;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public void setLockInfo(LockInfo lockInfo) {
        this.lockInfo = lockInfo;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setAllowRating(boolean allowRating) {
        this.allowRating = allowRating;
    }

    public void setOnlyGradersCanRate(boolean onlyGradersCanRate) {
        this.onlyGradersCanRate = onlyGradersCanRate;
    }

    public void setSortByRating(boolean sortByRating) {
        this.sortByRating = sortByRating;
    }

    public void setStatus(ReadState status) {
        if (status == ReadState.READ){
            this.readState = "read";
        } else {
            this.readState = "unread";
        }
    }

    public void setContextCode(String contextCode) {
        this.contextCode = contextCode;
    }

    public void setSubscribed(boolean subscribed) { this.subscribed = subscribed; }


    public void setLockAt(String lockAt) {
        this.lockAt = DateHelper.stringToDate(lockAt);
    }

    public void setLockAtDate(Date lockAt) {
        this.lockAt = lockAt;
    }

    public boolean getUserCanSeePosts() {
        return userCanSeePosts;
    }

    //endregion

    //region Parcelable

    public DiscussionTopicHeader() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.discussionType);
        dest.writeString(this.title);
        dest.writeString(this.message);
        dest.writeString(this.htmlUrl);
        dest.writeLong(this.postedAt != null ? this.postedAt.getTime() : -1);
        dest.writeLong(this.delayedPostAt != null ? this.delayedPostAt.getTime() : -1);
        dest.writeLong(this.lastReplyAt != null ? this.lastReplyAt.getTime() : -1);
        dest.writeByte(this.requireInitialPost ? (byte) 1 : (byte) 0);
        dest.writeInt(this.discussionSubentryCount);
        dest.writeString(this.readState);
        dest.writeInt(this.unreadCount);
        dest.writeInt(this.position);
        dest.writeLong(this.assignmentId);
        dest.writeByte(this.locked ? (byte) 1 : (byte) 0);
        dest.writeByte(this.lockedForUser ? (byte) 1 : (byte) 0);
        dest.writeString(this.lockExplanation);
        dest.writeByte(this.pinned ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.author, flags);
        dest.writeString(this.podcastUrl);
        dest.writeString(this.groupCategoryId);
        dest.writeByte(this.announcement ? (byte) 1 : (byte) 0);
        dest.writeLong(this.rootTopicId);
        dest.writeList(this.topicChildren);
        dest.writeTypedList(this.attachments);
        dest.writeByte(this.unauthorized ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.permissions, flags);
        dest.writeParcelable(this.assignment, flags);
        dest.writeParcelable(this.lockInfo, flags);
        dest.writeByte(this.published ? (byte) 1 : (byte) 0);
        dest.writeByte(this.allowRating ? (byte) 1 : (byte) 0);
        dest.writeByte(this.onlyGradersCanRate ? (byte) 1 : (byte) 0);
        dest.writeByte(this.sortByRating ? (byte) 1 : (byte) 0);
        dest.writeString(this.contextCode);
        dest.writeByte(this.subscribed ? (byte) 1 : (byte) 0);
        dest.writeLong(this.lockAt != null ? this.lockAt.getTime() : -1);
        dest.writeByte(this.userCanSeePosts ? (byte) 1 : (byte) 0);
    }

    protected DiscussionTopicHeader(Parcel in) {
        this.id = in.readLong();
        this.discussionType = in.readString();
        this.title = in.readString();
        this.message = in.readString();
        this.htmlUrl = in.readString();
        long tmpPostedAt = in.readLong();
        this.postedAt = tmpPostedAt == -1 ? null : new Date(tmpPostedAt);
        long tmpDelayedPostAt = in.readLong();
        this.delayedPostAt = tmpDelayedPostAt == -1 ? null : new Date(tmpDelayedPostAt);
        long tmpLastReplyAt = in.readLong();
        this.lastReplyAt = tmpLastReplyAt == -1 ? null : new Date(tmpLastReplyAt);
        this.requireInitialPost = in.readByte() != 0;
        this.discussionSubentryCount = in.readInt();
        this.readState = in.readString();
        this.unreadCount = in.readInt();
        this.position = in.readInt();
        this.assignmentId = in.readLong();
        this.locked = in.readByte() != 0;
        this.lockedForUser = in.readByte() != 0;
        this.lockExplanation = in.readString();
        this.pinned = in.readByte() != 0;
        this.author = in.readParcelable(DiscussionParticipant.class.getClassLoader());
        this.podcastUrl = in.readString();
        this.groupCategoryId = in.readString();
        this.announcement = in.readByte() != 0;
        this.rootTopicId = in.readLong();
        this.topicChildren = new ArrayList<Long>();
        in.readList(this.topicChildren, Long.class.getClassLoader());
        this.attachments = in.createTypedArrayList(RemoteFile.CREATOR);
        this.unauthorized = in.readByte() != 0;
        this.permissions = in.readParcelable(DiscussionTopicPermission.class.getClassLoader());
        this.assignment = in.readParcelable(Assignment.class.getClassLoader());
        this.lockInfo = in.readParcelable(LockInfo.class.getClassLoader());
        this.published = in.readByte() != 0;
        this.allowRating = in.readByte() != 0;
        this.onlyGradersCanRate = in.readByte() != 0;
        this.sortByRating = in.readByte() != 0;
        this.contextCode = in.readString();
        this.subscribed = in.readByte() != 0;
        long tmpLockAt = in.readLong();
        this.lockAt = tmpLockAt == -1 ? null : new Date(tmpLockAt);
        this.userCanSeePosts = in.readByte() != 0;
    }

    public static final Creator<DiscussionTopicHeader> CREATOR = new Creator<DiscussionTopicHeader>() {
        @Override
        public DiscussionTopicHeader createFromParcel(Parcel source) {
            return new DiscussionTopicHeader(source);
        }

        @Override
        public DiscussionTopicHeader[] newArray(int size) {
            return new DiscussionTopicHeader[size];
        }
    };

    //endregion
}
