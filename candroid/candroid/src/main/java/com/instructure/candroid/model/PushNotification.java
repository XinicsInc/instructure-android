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

package com.instructure.candroid.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instructure.candroid.BuildConfig;
import com.instructure.candroid.util.StudentPrefs;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.ApiPrefs;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PushNotification implements Parcelable {

    /**
     * Basic idea is that we STORE push notification info based on the incoming PUSH data.
     * But we RETRIEVE push notification info based on the current users data.
     *
     * If we store it correctly then it can be retrieved correctly.
     *
     * The HTML url has the domain as part of the url and the users id, the users id also has the shard so we
     * have to strip off the shard so we have the actual user id. With the domain and user id we can make
     * a key to store push notifications with.
     */

    private static final String PUSH_NOTIFICATIONS_KEY = "pushNotificationsKey";

    public static final String HTML_URL = "html_url";
    public static final String FROM = "from";
    public static final String ALERT = "alert";
    public static final String COLLAPSE_KEY = "collapse_key";
    public static final String USER_ID = "user_id";

    public String html_url = "";
    public String from = "";
    public String alert = "";
    public String collapse_key = "";
    public String user_id = "";

    public PushNotification() {

    }

    public PushNotification(String html_url, String from, String alert, String collapse_key, String user_id) {
        this.html_url = html_url;
        this.from = from;
        this.alert = alert;
        this.collapse_key = collapse_key;
        this.user_id = user_id;
    }

    public static boolean store(Context context, PushNotification push) {
        List<PushNotification> pushes = getStoredPushes(context);
        pushes.add(push);
        final String json = new Gson().toJson(pushes);
        final String key = getPushStoreKey(push);
        if(!TextUtils.isEmpty(key)) {
            StudentPrefs.INSTANCE.putString(key, json);
            return true;
        }
        return false;
    }

    public static void clearPushHistory(Context context) {
        String key = getPushStoreKey(context);
        if (key != null) StudentPrefs.INSTANCE.remove(key);
    }

    public static List<PushNotification> getStoredPushes(Context context) {
        String key = getPushStoreKey(context);
        if(!TextUtils.isEmpty(key)) {
            String json = StudentPrefs.INSTANCE.getString(key, "");
            if (!TextUtils.isEmpty(json)) {
                Type type = new TypeToken<List<PushNotification>>() {
                }.getType();
                List<PushNotification> pushes = new Gson().fromJson(json, type);
                if (pushes == null) {
                    pushes = new ArrayList<>();
                }
                return pushes;
            }
        }
        return new ArrayList<>();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.html_url);
        dest.writeString(this.from);
        dest.writeString(this.alert);
        dest.writeString(this.collapse_key);
        dest.writeString(this.user_id);
    }

    private PushNotification(Parcel in) {
        this.html_url = in.readString();
        this.from = in.readString();
        this.alert = in.readString();
        this.collapse_key = in.readString();
        this.user_id = in.readString();
    }

    public static final Parcelable.Creator<PushNotification> CREATOR = new Parcelable.Creator<PushNotification>() {
        public PushNotification createFromParcel(Parcel source) {
            return new PushNotification(source);
        }

        public PushNotification[] newArray(int size) {
            return new PushNotification[size];
        }
    };

    private static String getPushStorePrefix(Context context) {
        final User user = ApiPrefs.getUser();
        String domain = ApiPrefs.getDomain();

        if(user == null || TextUtils.isEmpty(domain)) {
            return null;
        }

        if(BuildConfig.IS_DEBUG) {
            domain = domain.replaceFirst(".beta", "");
        }

        return user.getId() + "___" + domain + "___";
    }

    private static String getPushStorePrefix(PushNotification push) {
        String userId = getUserIdFromPush(push);
        String domain;
        try {
            URL url = new URL(push.html_url);
            domain = url.getHost();
        } catch (MalformedURLException e) {
            domain = "";
        }

        if(TextUtils.isEmpty(userId) || TextUtils.isEmpty(domain)) {
            return null;
        }
        return userId + "___" + domain + "___";
    }


    private static String getPushStoreKey(Context context) {
        String prefix = getPushStorePrefix(context);
        if(prefix == null) {
            return null;
        }
        return prefix + PUSH_NOTIFICATIONS_KEY;
    }

    private static String getPushStoreKey(PushNotification push) {
        String prefix = getPushStorePrefix(push);
        if(prefix == null) {
            return null;
        }
        return prefix + PUSH_NOTIFICATIONS_KEY;
    }

    /**
     * To get the user id when the shard is added we have to mod the number by 10 trillion.
     * @param push The notification which should always have a valid user id with the shard.
     * @return the users id after removal of the shard id
     */
    private static String getUserIdFromPush(PushNotification push) {

        try {
            final long temp = 1000000000000L;
            final long userId = Long.parseLong(push.user_id);
            final long remainder = userId % temp;
            return Long.toString(remainder);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Removes the shard from a user id. It is expcted that the user id passed in has a mixed in. 
     * @param userIdWithShard
     * @return
     */
    public static String getUserIdFromPush(String userIdWithShard) {

        try {
            final long temp = 1000000000000L;
            final long userId = Long.parseLong(userIdWithShard);
            final long remainder = userId % temp;
            return Long.toString(remainder);
        } catch (Exception e) {
            return "";
        }
    }
}

