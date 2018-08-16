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

import android.net.Uri;
import android.os.Bundle;

import com.instructure.candroid.fragment.AssignmentFragment;
import com.instructure.candroid.fragment.DiscussionDetailsFragment;
import com.instructure.candroid.fragment.FileDetailsFragment;
import com.instructure.candroid.fragment.InternalWebviewFragment;
import com.instructure.candroid.fragment.MasteryPathLockedFragment;
import com.instructure.candroid.fragment.MasteryPathSelectionFragment;
import com.instructure.candroid.fragment.ModuleQuizDecider;
import com.instructure.candroid.fragment.PageDetailsFragment;
import com.instructure.candroid.fragment.ParentFragment;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.ModuleItem;
import com.instructure.canvasapi2.models.ModuleObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

public class ModuleUtility {

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Creation
    ///////////////////////////////////////////////////////////////////////////

    public static ParentFragment getFragment(ModuleItem moduleItem, final Course course, ModuleObject moduleObject) {
        if(moduleItem.getType() == null) {
            return null;
        }

        //deal with files
        if(moduleItem.getType().equals("File")) {
            String url = moduleItem.getUrl();
            //we just want the end of the api, not the domain

            Bundle bundle;
            url = removeDomain(url);

            if(moduleObject == null){
                bundle = FileDetailsFragment.createBundle(course, url);
            }else{
                long itemId = moduleItem.getId();
                long moduleId = moduleObject.getId();
                bundle = FileDetailsFragment.createBundle(course, moduleId, itemId, url);
            }

            //the fragment will handle getting the file information from the url
            return ParentFragment.createFragment(FileDetailsFragment.class, bundle);
        }

        //deal with pages
        if(moduleItem.getType().equals("Page")) {
            Bundle bundle = getPageBundle(moduleItem, course);

            return ParentFragment.createFragment(PageDetailsFragment.class, bundle);
        }

        //deal with assignments
        if(moduleItem.getType().equals("Assignment")) {
            Bundle bundle = getAssignmentBundle(moduleItem, course);
            return ParentFragment.createFragment(AssignmentFragment.class, bundle);
        }

        //deal with external urls
        if((moduleItem.getType().equals("ExternalUrl") || moduleItem.getType().equals("ExternalTool"))) {

            Uri uri =  Uri.parse(moduleItem.getHtml_url()).buildUpon().appendQueryParameter("display", "borderless").build();

            Bundle bundle = InternalWebviewFragment.Companion.createBundle(course, uri.toString(), moduleItem.getTitle(), true, true, true);
            return ParentFragment.createFragment(InternalWebviewFragment.class, bundle);
        }

        //don't do anything with headers, they're just dividers so we don't show them here.
        if((moduleItem.getType().equals("SubHeader"))) {
            return null;
        }

        //Quizzes
        if(moduleItem.getType().equals("Quiz")) {
            String apiURL = moduleItem.getUrl();
            apiURL = removeDomain(apiURL);

            Bundle bundle = ModuleQuizDecider.createBundle(course, moduleItem.getHtml_url(), apiURL);
            return ParentFragment.createFragment(ModuleQuizDecider.class, bundle);
        }

        //Discussions
        if(moduleItem.getType().equals("Discussion")) {
            Bundle bundle = getDiscussionBundle(moduleItem, course);
            return ParentFragment.createFragment(DiscussionDetailsFragment.class, bundle);
        }

        if(moduleItem.getType().equals("Locked")) {
            Bundle bundle = getLockedBundle(moduleItem, course);

            return ParentFragment.createFragment(MasteryPathLockedFragment.class, bundle);
        }

        if(moduleItem.getType().equals("ChooseAssignmentGroup")) {
            Bundle bundle = MasteryPathSelectionFragment.createBundle(course, moduleItem.getMasteryPaths(), moduleObject.getId(), moduleItem.getMasteryPathsItemId());

            return ParentFragment.createFragment(MasteryPathSelectionFragment.class, bundle);
        }
        //return null if there is a type we don't handle yet
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Bundle Creation
    ///////////////////////////////////////////////////////////////////////////

    private static Bundle getPageBundle(ModuleItem moduleItem, Course course) {
        //get the pageName from the url
        String url = moduleItem.getUrl();
        String pageName = null;
        if(url != null) {
            if(url.contains("wiki")) {
                int index = url.indexOf("wiki/");
                if(index != -1) {
                    index += 5;
                    pageName = getPageName(url, index);
                }
            }
            else if(url.contains("pages")) {
                int index = url.indexOf("pages/");
                if(index != -1) {
                    index += 6;
                    pageName = getPageName(url, index);
                }
            }

        }
        return PageDetailsFragment.Companion.createBundle(pageName, course);
    }

    private static Bundle getAssignmentBundle(ModuleItem moduleItem, Course course) {
        //get the assignment id from the url
        String url = moduleItem.getUrl();
        long assignmentId = 0;
        if(url.contains("assignments")) {
            int index = url.indexOf("assignments/");
            if(index != -1) {
                index += 12;
                assignmentId = getIdFromUrl(url, index);
            }
        }
        return AssignmentFragment.Companion.createBundle(course, assignmentId);
    }

    private static Bundle getDiscussionBundle(ModuleItem moduleItem, Course course) {
        //get the topic id from the url
        String url = moduleItem.getUrl();
        long topicId = 0;
        if(url.contains("discussion_topics")) {
            int index = url.indexOf("discussion_topics/");
            if(index != -1) {
                index += 18;
                topicId = getIdFromUrl(url, index);
            }
        }
        return DiscussionDetailsFragment.makeBundle(course, topicId, false);
    }

    private static Bundle getLockedBundle(ModuleItem moduleItem, Course course) {
        return MasteryPathLockedFragment.createBundle(course, moduleItem.getTitle());
    }
    ///////////////////////////////////////////////////////////////////////////
    // Helpers
    ///////////////////////////////////////////////////////////////////////////

    public static boolean isGroupLocked(ModuleObject module) {
        //currently the state for the group says "locked" until the user visits the modules online, so we need
        //a different way to determine if it's locked
        boolean isLocked = false;
        Date curDate = new Date();
        boolean isDatePassed = false;
        //if unlock date is set and we're still before the unlock date
        if((module.getUnlock_at() != null && curDate.before(module.getUnlock_at()))) {
            isDatePassed = true;
        }
        long[] ids = module.getPrerequisite_ids();
        //is the unlock date is in the past or the state == locked AND there are prerequisites
        if(ids != null && (isDatePassed || (ids.length > 0 && ids[0] != 0)) && module.getState() != null &&
                module.getState().equals(ModuleObject.STATE.locked.toString())) {
            isLocked = true;

        }
        return isLocked;
    }

    private static String removeDomain(String url) {
        //strip off the domain and protocol
        int index = 0;
        String prefix = "/api/v1/";
        index = url.indexOf(prefix);
        if(index != -1) {
            url = url.substring(index + prefix.length());
        }
        return url;
    }

    private static long getIdFromUrl(String url, int index) {
        long assignmentId;
        int endIndex = url.indexOf("/", index);
        if(endIndex != -1) {
            assignmentId = Long.parseLong(url.substring(index, endIndex));
        } else {
            assignmentId = Long.parseLong(url.substring(index));
        }
        return assignmentId;
    }

    private static String getPageName(String url, int index) {
        String pageName;
        int endIndex = url.indexOf("/", index);
        if(endIndex != -1) {
            pageName = url.substring(index, endIndex);
        } else {
            pageName = url.substring(index);
        }

        //decode the page name in case there are special characters in the name (like {}<>|`)
        try {
            pageName = URLDecoder.decode(pageName, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            LoggingUtility.LogConsole(e.getMessage());
        }

        return pageName;
    }
}
