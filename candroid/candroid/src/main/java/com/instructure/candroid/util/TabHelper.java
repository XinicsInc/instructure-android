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

package com.instructure.candroid.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.instructure.candroid.R;
import com.instructure.candroid.fragment.AnnouncementListFragment;
import com.instructure.candroid.fragment.AssignmentListFragment;
import com.instructure.candroid.fragment.DiscussionListFragment;
import com.instructure.candroid.fragment.FileListFragment;
import com.instructure.candroid.fragment.GradesListFragment;
import com.instructure.candroid.fragment.LTIWebViewFragment;
import com.instructure.candroid.fragment.ModuleListFragment;
import com.instructure.candroid.fragment.NotificationListFragment;
import com.instructure.candroid.fragment.PageListFragment;
import com.instructure.candroid.fragment.ParentFragment;
import com.instructure.candroid.fragment.PeopleListFragment;
import com.instructure.candroid.fragment.QuizListFragment;
import com.instructure.candroid.fragment.ScheduleListFragment;
import com.instructure.candroid.fragment.CourseSettingsFragment;
import com.instructure.candroid.fragment.UnSupportedTabFragment;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.ContextKeeper;

public class TabHelper {

    public static Tab getTabForType(Context context, String tabId){

        if(TextUtils.isEmpty(tabId)) {
            return Tab.newInstance(Tab.HOME_ID, context.getString(R.string.home));
        } else {
            if(Tab.SYLLABUS_ID.equals(tabId)) {
                return Tab.newInstance(Tab.SYLLABUS_ID, context.getString(R.string.syllabus));
            } else if(Tab.ASSIGNMENTS_ID.equals(tabId)) {
                return Tab.newInstance(Tab.ASSIGNMENTS_ID, context.getString(R.string.assignments));
            } else if(Tab.DISCUSSIONS_ID.equals(tabId)) {
                return Tab.newInstance(Tab.DISCUSSIONS_ID, context.getString(R.string.discussion));
            } else if(Tab.PAGES_ID.equals(tabId)) {
                return Tab.newInstance(Tab.PAGES_ID, context.getString(R.string.pages));
            } else if(Tab.PEOPLE_ID.equals(tabId)) {
                return Tab.newInstance(Tab.PEOPLE_ID, context.getString(R.string.coursePeople));
            } else if(Tab.QUIZZES_ID.equals(tabId)) {
                return Tab.newInstance(Tab.QUIZZES_ID, context.getString(R.string.quizzes));
            } else if(Tab.FILES_ID.equals(tabId)) {
                return Tab.newInstance(Tab.FILES_ID, context.getString(R.string.files));
            } else if(Tab.ANNOUNCEMENTS_ID.equals(tabId)) {
                return Tab.newInstance(Tab.ANNOUNCEMENTS_ID, context.getString(R.string.announcements));
            } else if(Tab.MODULES_ID.equals(tabId)) {
                return Tab.newInstance(Tab.MODULES_ID, context.getString(R.string.modules));
            } else if(Tab.GRADES_ID.equals(tabId)) {
                return Tab.newInstance(Tab.GRADES_ID, context.getString(R.string.grades));
            } else if(Tab.COLLABORATIONS_ID.equals(tabId)) {
                return Tab.newInstance(Tab.COLLABORATIONS_ID, context.getString(R.string.collaborations));
            } else if(Tab.CONFERENCES_ID.equals(tabId)) {
                return Tab.newInstance(Tab.CONFERENCES_ID, context.getString(R.string.conferences));
            } else if(Tab.OUTCOMES_ID.equals(tabId)) {
                return Tab.newInstance(Tab.OUTCOMES_ID, context.getString(R.string.outcomes));
            } else if(Tab.NOTIFICATIONS_ID.equals(tabId)) {
                return Tab.newInstance(Tab.NOTIFICATIONS_ID, context.getString(R.string.notifications));
            } else if(Tab.HOME_ID.equals(tabId)) {
                return Tab.newInstance(Tab.HOME_ID, context.getString(R.string.home));
            } else if(Tab.CHAT_ID.equals(tabId)) {
                return Tab.newInstance(Tab.CHAT_ID, context.getString(R.string.chat));
            } else if(Tab.SETTINGS_ID.equals(tabId)) {
                return Tab.newInstance(Tab.SETTINGS_ID, context.getString(R.string.settings));
            } else if(Tab.TYPE_EXTERNAL.equals(tabId)) {
                return Tab.newInstance(tabId, context.getString(R.string.link));
            } else {
                return Tab.newInstance(Tab.HOME_ID, context.getString(R.string.home));
            }
        }
    }

    @Nullable
    public static String getHomePageDisplayString(@NonNull CanvasContext canvasContext, @NonNull Tab tab) {
        switch (canvasContext.getHomePageID()) {
            case Tab.NOTIFICATIONS_ID: return ContextKeeper.appContext.getString(R.string.homePageIdForNotifications);
            case Tab.PAGES_ID: return "";
            case Tab.MODULES_ID: return ContextKeeper.appContext.getString(R.string.homePageIdForModules);
            case Tab.ASSIGNMENTS_ID: return ContextKeeper.appContext.getString(R.string.homePageIdForAssignments);
            case Tab.SYLLABUS_ID: return ContextKeeper.appContext.getString(R.string.homePageIdForSyllabus);
            default: return null;
        }
    }

    public static boolean isHomeTabAPage(@NonNull CanvasContext canvasContext) {
        return Tab.PAGES_ID.equals(canvasContext.getHomePageID());
    }

    /**
     * Check if the tab is the home tab. This will allow us to display "Home"
     * in the actionbar instead of the actual tab name
     * @param tab Tab that we are checking to see if it is the home tab
     * @param canvasContext Used to get the home tab id for the course/group
     * @return True if the tab is the home page, false otherwise
     */
    public static boolean isHomeTab(@NonNull Tab tab, @NonNull CanvasContext canvasContext) {
        return isHomeTab(tab.getTabId(), canvasContext);
    }

    public static boolean isHomeTab(@NonNull String tabId, @NonNull CanvasContext canvasContext) {
        return canvasContext.getHomePageID().equals(tabId) || "home".equalsIgnoreCase(tabId);
    }

    public static boolean isHomeTab(@NonNull Tab tab) {
        return "home".equalsIgnoreCase(tab.getTabId());
    }

    public static ParentFragment getFragmentByTab(Tab tab, CanvasContext canvasContext) {
        if(canvasContext == null) {
            return null;
        }

        if(tab == null) {
            return getFragmentByTabId(Tab.newInstance(Tab.HOME_ID, ""), canvasContext);
        }

        return getFragmentByTabId(tab, canvasContext);
    }

    public static ParentFragment getFragmentByTabId(Tab tab, CanvasContext canvasContext) {

        if(tab == null) {
            return null;
        }
        String tabId = tab.getTabId();
        final Bundle standardTabBundle = ParentFragment.createBundle(canvasContext, tab);
        if(TextUtils.isEmpty(tabId)) {
            tabId = canvasContext.getHomePageID();
        }

        if(tabId.equalsIgnoreCase(canvasContext.getHomePageID()) || "home".equalsIgnoreCase(tabId)) {
            tabId = canvasContext.getHomePageID();
        }

        ParentFragment fragment = null;

        if(tabId.equalsIgnoreCase(Tab.MODULES_ID)) {
            fragment = ParentFragment.createFragment(ModuleListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.DISCUSSIONS_ID)) {
            fragment = ParentFragment.createFragment(DiscussionListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.PAGES_ID)) {
            fragment = ParentFragment.createFragment(PageListFragment.class, PageListFragment.createBundle(canvasContext, false, tab));
        } else if(tabId.equalsIgnoreCase(Tab.PEOPLE_ID)) {
            fragment = ParentFragment.createFragment(PeopleListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.FILES_ID)) {
            fragment = ParentFragment.createFragment(FileListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.SYLLABUS_ID)) {
            fragment = ParentFragment.createFragment(ScheduleListFragment.class, ScheduleListFragment.createBundle(canvasContext, true, tab));
        } else if(tabId.equalsIgnoreCase(Tab.QUIZZES_ID)) {
            fragment = ParentFragment.createFragment(QuizListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.OUTCOMES_ID)) {
            fragment = UnSupportedTabFragment.createFragment(UnSupportedTabFragment.class, UnSupportedTabFragment.createBundle(canvasContext, tab.getTabId(), R.string.outcomes));
        } else if(tabId.equalsIgnoreCase(Tab.CONFERENCES_ID)) {
            fragment = UnSupportedTabFragment.createFragment(UnSupportedTabFragment.class, UnSupportedTabFragment.createBundle(canvasContext, tab.getTabId(), R.string.conferences));
        } else if(tabId.equalsIgnoreCase(Tab.ANNOUNCEMENTS_ID)) {
            fragment = ParentFragment.createFragment(AnnouncementListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.ASSIGNMENTS_ID)) {
            fragment = ParentFragment.createFragment(AssignmentListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.GRADES_ID)) {
            fragment = ParentFragment.createFragment(GradesListFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.COLLABORATIONS_ID)) {
            fragment = UnSupportedTabFragment.createFragment(UnSupportedTabFragment.class, UnSupportedTabFragment.createBundle(canvasContext, tab.getTabId(), R.string.collaborations));
        } else if(tabId.equalsIgnoreCase(Tab.SETTINGS_ID)) {
            fragment = ParentFragment.createFragment(CourseSettingsFragment.class, standardTabBundle);
        } else if(tabId.equalsIgnoreCase(Tab.NOTIFICATIONS_ID)) {
            fragment = ParentFragment.createFragment(NotificationListFragment.class, standardTabBundle);
        }
        //we just care if it's external, some external tabs (Attendance) have an id after "external"
        else if(tabId.contains(Tab.TYPE_EXTERNAL)) {
            //Assumes we are not routing from a link, if so use LTIWebViewRoutingFragment.class
            fragment = ParentFragment.createFragment(LTIWebViewFragment.class, LTIWebViewFragment.Companion.createBundle(canvasContext, tab));
        }

        return fragment;
    }
}
