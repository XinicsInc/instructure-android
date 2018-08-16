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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.instructure.canvasapi2.utils.ApiPrefs;

import java.util.Date;


public class Tab extends CanvasModel<Tab> implements Parcelable {

    public static final String TYPE_INTERNAL = "internal";
    public static final String TYPE_EXTERNAL = "external";

    public static final String TYPE_EXTERNAL_PREFIX = "context_external_tool_";

    // id constants (these should never change in the API)
    public static final String SYLLABUS_ID = "syllabus";
    public static final String AGENDA_ID = "agenda";
    public static final String ASSIGNMENTS_ID = "assignments";
    public static final String DISCUSSIONS_ID = "discussions";
    public static final String PAGES_ID = "pages";
    public static final String PEOPLE_ID = "people";
    public static final String QUIZZES_ID = "quizzes";
    public static final String FILES_ID = "files";
    public static final String ANNOUNCEMENTS_ID = "announcements";
    public static final String MODULES_ID = "modules";
    public static final String GRADES_ID = "grades";
    public static final String COLLABORATIONS_ID = "collaborations";
    public static final String CONFERENCES_ID = "conferences";
    public static final String OUTCOMES_ID = "outcomes";
    public static final String NOTIFICATIONS_ID = "notifications";
    public static final String HOME_ID = "home";
    public static final String CHAT_ID = "chat";
    public static final String SETTINGS_ID = "settings";

    // API Variables
    private String id;
    private String label;
    private String type;
    @SerializedName("html_url")
    private String htmlUrl;                // internal url
    @SerializedName("full_url")
    private String fullUrl;                // external url
    private String visibility = "none";     // possible values are: public, members, admins, and none
    private boolean hidden;                 // only included when true
    private int position = 0;
    @SerializedName("url")
    private String LTI_url;

    @Override
    public long getId() {
        return position;
    }

    @Override
    public String getComparisonString() {
        return null;
    }

    @Override
    public Date getComparisonDate() {
        return null;
    }

    public String getTabId() {
        return id;
    }
    public String getLabel() {
        return label;
    }
    public String getType() {
        return type;
    }
    public boolean isExternal() {
        return type.equals(TYPE_EXTERNAL);
    }

    public String getUrl() {
        String temp_html_url = htmlUrl;

        //Domain strips off trailing slashes.
        if(!temp_html_url.startsWith("/")){
            temp_html_url = "/" + temp_html_url;
        }

        return ApiPrefs.getDomain() + temp_html_url;
    }

    public String getExternalUrl() {
        return fullUrl;
    }

    public String getLTIUrl() {
        return LTI_url;
    }

    public String getVisibility() {
        return visibility;
    }

    public boolean isPublic() {
        return "public".equals(visibility);
    }

    public boolean isMembers() {
        return "members".equals(visibility);
    }

    public boolean isAdmin() {
        return "admin".equals(visibility);
    }

    public boolean isNone() {
        return "none".equals(visibility);
    }

    public int getPosition() {
        return position;
    }

    public boolean isHidden() {
        return hidden;
    }

    private Tab() {}

    public static Tab newInstance(String id, String label) {
        Tab result = new Tab();
        result.id = id;
        result.label = label;
        result.type = TYPE_INTERNAL;
        return result;
    }

    @Override
    public String toString(){
        if(this.getTabId() == null || this.getLabel() == null){
            return "";
        }
        return this.getTabId()+":"+this.getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tab tab = (Tab) o;

        return id.equals(tab.id) && label.equals(tab.label);
    }

    //region Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.label);
        dest.writeString(this.type);
        dest.writeString(this.htmlUrl);
        dest.writeString(this.fullUrl);
        dest.writeString(this.visibility);
        dest.writeByte(hidden ? (byte) 1 : (byte) 0);
        dest.writeInt(this.position);
        dest.writeString(this.LTI_url);
    }

    protected Tab(Parcel in) {
        this.id = in.readString();
        this.label = in.readString();
        this.type = in.readString();
        this.htmlUrl = in.readString();
        this.fullUrl = in.readString();
        this.visibility = in.readString();
        this.hidden = in.readByte() != 0;
        this.position = in.readInt();
        this.LTI_url = in.readString();
    }

    public static final Creator<Tab> CREATOR = new Creator<Tab>() {
        @Override
        public Tab createFromParcel(Parcel source) {
            return new Tab(source);
        }

        @Override
        public Tab[] newArray(int size) {
            return new Tab[size];
        }
    };

    //endregion
}
