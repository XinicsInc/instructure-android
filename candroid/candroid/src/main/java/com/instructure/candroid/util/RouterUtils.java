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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.instructure.candroid.R;
import com.instructure.candroid.activity.BaseRouterActivity;
import com.instructure.candroid.activity.NavigationActivity;
import com.instructure.candroid.fragment.AnnouncementListFragment;
import com.instructure.candroid.fragment.AssignmentFragment;
import com.instructure.candroid.fragment.AssignmentListFragment;
import com.instructure.candroid.fragment.BasicQuizViewFragment;
import com.instructure.candroid.fragment.CalendarListViewFragment;
import com.instructure.candroid.fragment.DashboardFragment;
import com.instructure.candroid.fragment.CourseModuleProgressionFragment;
import com.instructure.candroid.fragment.DiscussionDetailsFragment;
import com.instructure.candroid.fragment.DiscussionListFragment;
import com.instructure.candroid.fragment.FileListFragment;
import com.instructure.candroid.fragment.GradesListFragment;
import com.instructure.candroid.fragment.InboxFragment;
import com.instructure.candroid.fragment.InternalWebviewFragment;
import com.instructure.candroid.fragment.InboxConversationFragment;
import com.instructure.candroid.fragment.ModuleListFragment;
import com.instructure.candroid.fragment.NotificationListFragment;
import com.instructure.candroid.fragment.PageDetailsFragment;
import com.instructure.candroid.fragment.PageListFragment;
import com.instructure.candroid.fragment.PeopleDetailsFragment;
import com.instructure.candroid.fragment.PeopleListFragment;
import com.instructure.candroid.fragment.QuizListFragment;
import com.instructure.candroid.fragment.ScheduleListFragment;
import com.instructure.candroid.fragment.CourseSettingsFragment;
import com.instructure.candroid.fragment.UnSupportedFeatureFragment;
import com.instructure.candroid.fragment.UnSupportedTabFragment;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.Logger;
import com.instructure.pandautils.utils.Const;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class should be used for external and internal linking. The links send here will route the user
 * to the correct activity/fragment if we handle that type of link.
 */
public class RouterUtils {

    public enum ROUTE_TYPE {INTERNALLY_ROUTED, FILE_DOWNLOAD, LTI, NOT_INTERNALLY_ROUTED, NOTIFICATION_PREFERENCES};

    private static final List<Route> sRoutes = new ArrayList<>();

    ///////////////////
    // NOTE: Place routes that have are specify a non-param url before a param url
    //
    // Example:   /:course_id/assignments/syllabus (should come before the route with assignments/:assignment_id so it is matched first)
    //            /:course_id/assignments/:assignment_id (will match /syllabus as the :assignment_id param, and /syllabus route would not be matched if it came after)
    //
    ///////////////////

    private static String COURSE_OR_GROUP_REGEX = "/(?:courses|groups)";
    private static String courseOrGroup(String route) {
        return COURSE_OR_GROUP_REGEX + route;
    }

    static {
        sRoutes.add(new Route("/", DashboardFragment.class));
        // region Conversations
        sRoutes.add(new Route("/conversations", InboxFragment.class));
        sRoutes.add(new Route("/conversations/:conversation_id", InboxFragment.class, InboxConversationFragment.class));
        sRoutes.add(new Route("/conversations/:conversation_id", NotificationListFragment.class, InboxConversationFragment.class));
        sRoutes.add(new Route("/login.*", ROUTE_TYPE.NOT_INTERNALLY_ROUTED));
        // endregion

        //////////////////////////
        // Courses
        //////////////////////////
        sRoutes.add(new Route(courseOrGroup("/"), DashboardFragment.class));
        sRoutes.add(new Route(courseOrGroup("/:course_id"), null, Tab.HOME_ID));

        // region Modules

        /*
        Modules with query params
        !!!!!!!!!!!!
        !  CAUTION: Order matters, these are purposely placed above the pages, quizzes, disscussions, assignments, and files so they are matched if query params exist and routed to Modules
        !!!!!!!!!!!!
        */
        sRoutes.add(new Route(courseOrGroup("/:course_id/modules"), ModuleListFragment.class, Tab.MODULES_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/modules/items/:module_item_id"), ModuleListFragment.class, Tab.MODULES_ID)); // Just route to modules list. API does not have a way to fetch a module item without knowing the module id (even though web canvas can do it)
        sRoutes.add(new Route(courseOrGroup("/:course_id/modules/:module_id"), ModuleListFragment.class, Tab.MODULES_ID));

        sRoutes.add(new Route(courseOrGroup("/:course_id/pages/:page_id"), ModuleListFragment.class, CourseModuleProgressionFragment.class, Tab.MODULES_ID, Arrays.asList("module_item_id")));
        sRoutes.add(new Route(courseOrGroup("/:course_id/quizzes/:quiz_id"), ModuleListFragment.class, CourseModuleProgressionFragment.class, Tab.MODULES_ID, Arrays.asList("module_item_id")));
        sRoutes.add(new Route(courseOrGroup("/:course_id/discussion_topics/:message_id"), ModuleListFragment.class, CourseModuleProgressionFragment.class, Tab.MODULES_ID, Arrays.asList("module_item_id")));
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id"), ModuleListFragment.class, CourseModuleProgressionFragment.class, Tab.MODULES_ID, Arrays.asList("module_item_id")));
        sRoutes.add(new Route(courseOrGroup("/:course_id/files/:file_id"), ModuleListFragment.class, CourseModuleProgressionFragment.class, Tab.MODULES_ID, Arrays.asList("module_item_id"))); // TODO TEST
        // endregion

        // Notifications
        sRoutes.add(new Route(courseOrGroup("/:course_id/notifications"), NotificationListFragment.class, Tab.NOTIFICATIONS_ID));

        // Grades
        sRoutes.add(new Route(courseOrGroup("/:course_id/grades"), GradesListFragment.class, Tab.GRADES_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/grades/:assignment_id"), GradesListFragment.class, AssignmentFragment.class, Tab.GRADES_ID));

        // People
        sRoutes.add(new Route(courseOrGroup("/:course_id/users"), PeopleListFragment.class, Tab.PEOPLE_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/users/:user_id"), PeopleListFragment.class, PeopleDetailsFragment.class, Tab.PEOPLE_ID));

        // Files
        sRoutes.add(new Route(courseOrGroup("/:course_id/files"), FileListFragment.class, Tab.FILES_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/files/:file_id/download"), ROUTE_TYPE.FILE_DOWNLOAD)); // trigger webview's download listener
        sRoutes.add(new Route(courseOrGroup("/:course_id/files/:file_id"), ROUTE_TYPE.FILE_DOWNLOAD));

        // Discussions
        sRoutes.add(new Route(courseOrGroup("/:course_id/discussion_topics"), DiscussionListFragment.class, Tab.DISCUSSIONS_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/discussion_topics/:message_id"), DiscussionListFragment.class, DiscussionDetailsFragment.class, Tab.DISCUSSIONS_ID));

        // Pages
        sRoutes.add(new Route(courseOrGroup("/:course_id/pages"), PageListFragment.class, Tab.PAGES_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/pages/:page_id"), PageListFragment.class, PageDetailsFragment.class, Tab.PAGES_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/wiki"), PageListFragment.class, Tab.PAGES_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/wiki/:page_id"), PageListFragment.class, PageDetailsFragment.class, Tab.PAGES_ID));

        // Announcements
        sRoutes.add(new Route(courseOrGroup("/:course_id/announcements"), AnnouncementListFragment.class, Tab.ANNOUNCEMENTS_ID));
        // :message_id because it shares with discussions
        sRoutes.add(new Route(courseOrGroup("/:course_id/announcements/:message_id"), AnnouncementListFragment.class, DiscussionDetailsFragment.class, Tab.ANNOUNCEMENTS_ID));
        // Announcements from the notifications tab
        sRoutes.add(new Route(courseOrGroup("/:course_id/announcements/:message_id"), NotificationListFragment.class, DiscussionDetailsFragment.class, Tab.ANNOUNCEMENTS_ID));

        // Quiz
        sRoutes.add(new Route(courseOrGroup("/:course_id/quizzes"), QuizListFragment.class, Tab.QUIZZES_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/quizzes/:quiz_id"), QuizListFragment.class, BasicQuizViewFragment.class, Tab.QUIZZES_ID));


        // Calendar
        sRoutes.add(new Route("/calendar", CalendarListViewFragment.class));
        sRoutes.add(new Route(courseOrGroup("/:course_id/calendar_events/:event_id"), CalendarListViewFragment.class));

        // Syllabus
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/syllabus"), ScheduleListFragment.class, ScheduleListFragment.class, Tab.SYLLABUS_ID));

        // Assignments
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments"), AssignmentListFragment.class, Tab.ASSIGNMENTS_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id"), AssignmentListFragment.class, AssignmentFragment.class, Tab.ASSIGNMENTS_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id"), ScheduleListFragment.class, AssignmentFragment.class, Tab.ASSIGNMENTS_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id"), NotificationListFragment.class, AssignmentFragment.class, Tab.ASSIGNMENTS_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id"), CalendarListViewFragment.class, AssignmentFragment.class, Tab.ASSIGNMENTS_ID));


        // Submissions
        // :sliding_tab_type can be /rubric or /submissions (used to navigate to the nested fragment)
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id/:sliding_tab_type"), AssignmentListFragment.class, AssignmentFragment.class, Tab.ASSIGNMENTS_ID));
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id/:sliding_tab_type/:submission_id"), AssignmentListFragment.class, AssignmentFragment.class, Tab.ASSIGNMENTS_ID));

        // Settings
        sRoutes.add(new Route(courseOrGroup("/:course_id/settings"), CourseSettingsFragment.class, Tab.SETTINGS_ID));

        // Unsupported
        // NOTE: An Exception to how the router usually works (Not recommended for urls that are meant to be internally routed)
        //  The .* will catch anything and route to UnsupportedFragment. If the users decides to press "open in browser" from the UnsupportedFragment, then InternalWebviewFragment is setup to handle the unsupportedFeature
        sRoutes.add(new Route(courseOrGroup("/:course_id/collaborations.*"), UnSupportedTabFragment.class, Tab.COLLABORATIONS_ID));
        //sRoutes.add(new Route(courseOrGroup("/:course_id/conferences.*"), UnSupportedTabFragment.class, Tab.CONFERENCES_ID)); //No longer supported. Left here so you read this and know.
        sRoutes.add(new Route(courseOrGroup("/:course_id/outcomes.*"), UnSupportedTabFragment.class, Tab.OUTCOMES_ID));

        sRoutes.add(new Route("/files", FileListFragment.class));
        sRoutes.add(new Route("/files/:file_id", FileListFragment.class)); // TODO TEST
        sRoutes.add(new Route("/files/:file_id/download", ROUTE_TYPE.FILE_DOWNLOAD)); // trigger webview's download listener

        //Notification Preferences
        sRoutes.add(new Route("/profile/communication", ROUTE_TYPE.NOTIFICATION_PREFERENCES));

        //Course Module Progression
        sRoutes.add(new Route(courseOrGroup("/:course_id/:module_type_slash_id"), ModuleListFragment.class, CourseModuleProgressionFragment.class, Tab.MODULES_ID));

        //LTI
        sRoutes.add(new Route(courseOrGroup("/:course_id/external_tools/:external_id"), ROUTE_TYPE.LTI));

        //Single Detail Pages (Typically routing from To-dos (may not be handling every use case)
        sRoutes.add(new Route(courseOrGroup("/:course_id/assignments/:assignment_id"), AssignmentFragment.class, null, Tab.ASSIGNMENTS_ID));

        // Catch all (when nothing has matched, these take over)
        // Note: Catch all only happens with supported domains such as instructure.com
        sRoutes.add(new Route(courseOrGroup("/:course_id/.*"), UnSupportedFeatureFragment.class)); // course_id fetches the course context
        sRoutes.add(new Route(".*", UnSupportedFeatureFragment.class));
    }


    /**
     * Gets the Route, null if route cannot be handled internally
     * @param url
     * @return Route if application can handle link internally; null otherwise
     */
    public static Route getInternalRoute(String url, String domain) {
        UrlValidity urlValidity = new UrlValidity(url, domain);

        if (!urlValidity.isHostForLoggedInUser() || !urlValidity.isValid()) {
            return null;
        }

        Route route = null;
        for (Route r : sRoutes) {
            if (r.apply(url)) {
                if (ROUTE_TYPE.NOT_INTERNALLY_ROUTED == r.getRouteType()) {
                    return null; // returning null allows for routes that are not supported to skip the unsupported fragment and are usually just opened in a webview
                }
                route = r;
                break; // Do NOT remove break (matches the first route it comes across, more specific routes must come first)
            }
        }
        return route;
    }

    public static Route getInternalRoute(Class<? extends FragmentInteractions> masterCls, Class<? extends FragmentInteractions> detailCls) {
        Route route = null;
        for (Route r : sRoutes) {
            if (r.apply(masterCls, detailCls)) {
                route = r;
                break; // Do NOT remove break (matches the first route it comes across, more specific routes must come first)
            }
        }
        return route;
    }

    public static String createUrl(CanvasContext.Type type, Class<? extends FragmentInteractions> masterCls, HashMap<String, String> replacementParams) {
        return createUrl(type, masterCls, null, replacementParams, null);
    }

    public static String createUrl(CanvasContext.Type type, Class<? extends FragmentInteractions> masterCls, Class<? extends FragmentInteractions> detailCls, HashMap<String, String> replacementParams, HashMap<String, String> queryParams) {
        if(replacementParams == null || replacementParams.isEmpty()) {
            return null;
        }

        String domain = ApiPrefs.getFullDomain();
        Route urlRoute = getInternalRoute(masterCls, detailCls);
        if (urlRoute != null) {
            String path = urlRoute.createUrl(replacementParams);
            if (path.contains(COURSE_OR_GROUP_REGEX)) {
                Pattern pattern = Pattern.compile(COURSE_OR_GROUP_REGEX, Pattern.LITERAL);
                Matcher matcher = pattern.matcher(path);
                switch (type) {
                    case COURSE:
                        path = matcher.replaceAll("/courses");
                        break;
                    case GROUP:
                        path = matcher.replaceAll("/groups");
                        break;
                    //TODO: handel USER type
                }
            }
            return createQueryParamString((domain + path), queryParams);
        }
        return null;
    }

    /**
     * Returns true if url can be routed to a fragment, false otherwise
     * @param activity
     * @param url
     * @param routeIfPossible
     * @return
     */
    public static boolean canRouteInternally(Activity activity, String url, String domain, boolean routeIfPossible) {
        boolean canRoute = getInternalRoute(url, domain) != null;

        if (canRoute && activity != null && routeIfPossible) {
            routeUrl(activity, url, true);
        }
        return canRoute;
    }

    /**
     * Determines if a url can be loaded with a fragment in the app.
     * Example: /courses/1234/assignments/5678 would be routed to AssignmentListFragment (master) and AssignmentFragment (detail)
     *
     * Routing is determined by the following:
     *      1. Url has the logged in user's domain
     *      2. Url has a matching route which specifies which fragment to load
     *      3. Url is from a third party domain such as youtube.com or wikipedia.com
     *
     * if 1 && 2
     *      Routes to fragment
     * else if 1 && not 2
     *      Routes to unsupported fragment with an option to open the url with web canvas
     * else  (3)
     *      open internal webview fragment
     *
     * @param activity
     * @param url
     * @param animate
     * @return
     */
    public static void routeUrl(final Activity activity, String url, boolean animate) {
        if (activity == null) {
            return;
        }

        UrlValidity urlValidity = new UrlValidity(url, ApiPrefs.getDomain());

        if (!urlValidity.isValid()) {
            routeToLandingPage(activity, true);
        }

        boolean isHostForLoggedInUser = urlValidity.isHostForLoggedInUser();

        if (isHostForLoggedInUser) {
            Application application = activity.getApplication();
            if(application instanceof AnalyticsEventHandling) {
                ((AnalyticsEventHandling)application).trackScreen("AppRouter");
            }

            if (activity instanceof BaseRouterActivity) {
                final Route route = getInternalRoute(url, ApiPrefs.getDomain());
                if (route != null) ((BaseRouterActivity)activity).handleRoute(route);
            } else {
                Intent intent = new Intent(activity, NavigationActivity.Companion.getStartActivityClass());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(Const.PARSE, true);
                intent.putExtra(Const.URL, url);
                activity.startActivity(intent);
                if(animate) activity.overridePendingTransition(R.anim.fade_in_quick, R.anim.fade_out_quick);
            }
        } else {
            openInInternalWebViewFragment(activity, url);
        }
    }

    private static String createQueryParamString(String url, HashMap<String, String> queryParams) {
        if(queryParams != null && !queryParams.isEmpty()) {
            Uri.Builder uri = Uri.parse(url).buildUpon();
            for (HashMap.Entry<String, String> entry : queryParams.entrySet()) {
                uri.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            return uri.build().toString();
        }
        return url;
    }

    private static class UrlValidity {
        private boolean mIsHostForLoggedInUser = false;
        private boolean mIsValid = false;
        private Uri mUri;

        UrlValidity(String url, String userDomain) {
            mUri =  Uri.parse(url);
            if (mUri != null) {
                mIsValid = true;
            }

            String host = mUri.getHost();
            mIsHostForLoggedInUser = isLoggedInUserHost(host, userDomain);
        }

        private boolean isLoggedInUserHost(String host, String userDomain) {
            // Assumes user is already signed in (InterwebsToApplication does a signin check)
            return ((userDomain != null && userDomain.equals(host)));
        }

        // region Getter && Setters
        boolean isHostForLoggedInUser() {
            return mIsHostForLoggedInUser;
        }

        boolean isValid() {
            return mIsValid;
        }

        public Uri getUri() {
            return mUri;
        }

        // endregion
    }

    private static void routeToLandingPage(Context context, boolean clearBackStack) {
        Logger.d("routeToLandingPage()");
        Intent intent = new Intent(context, NavigationActivity.Companion.getStartActivityClass());
        if(clearBackStack) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        else intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    private static void openInInternalWebViewFragment(Context context, String url) {
        Logger.d("couldNotParseUrl()");

        Bundle bundle = InternalWebviewFragment.Companion.createBundle(url, null, false, null);

        Intent intent = new Intent(context, NavigationActivity.Companion.getStartActivityClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }



    /**
     * A Route defines urls with params to be matched and parsed.
     * The params are denoted by a ':' then the name of the param. A paramHash is then created based off how the route is specified.
     *
     * Example: If the route is /courses/:course_id/assignments/:this_is_the_name_i_choose and the url /courses/1234/assignments/5678 is applied.
     * The paramsHash will contain "course_id" -> "1234", "this_is_the_name_i_choose" -> "5678"
     */
    public static class Route {
        // region Route specific (never changes after route is created)
        private String mRoute;
        private Pattern mRoutePattern;
        private ArrayList<String> mParamNames = new ArrayList<>();
        private List<String> mQueryParamNames; // used to validate a route that requires certain query params
        private @Nullable Class<? extends FragmentInteractions> mMasterCls;
        private @Nullable Class<? extends FragmentInteractions> mDetailCls;
        private String mTabId;
        private ROUTE_TYPE mRouteType = ROUTE_TYPE.INTERNALLY_ROUTED;
        // endregion

        // region Refers to the url that was applied to the route
        private HashMap<String, String> mParamsHash;
        private HashMap<String, String> mQueryParamsHash;
        private String mUrl;
        private Uri mUri;
        // endregion

        // region Constructors

        public Route(String route, @Nullable Class<? extends FragmentInteractions> masterCls, @Nullable Class<? extends FragmentInteractions> detailCls) {
            this(route);
            mMasterCls = masterCls;
            mDetailCls = detailCls;
        }

        public Route(String route, @Nullable Class<? extends FragmentInteractions> masterCls, @Nullable Class<? extends FragmentInteractions> detailCls, String tabId) {
            this(route, masterCls, tabId);
            mDetailCls = detailCls;
        }

        public Route(String route, @Nullable Class<? extends FragmentInteractions> masterCls, @Nullable Class<? extends FragmentInteractions> detailCls, String tabId, List<String> queryParamNames) {
            this(route, masterCls, tabId);
            mDetailCls = detailCls;
            mQueryParamNames = queryParamNames;
        }

        public <Type extends FragmentInteractions> Route(String route, @Nullable Class<Type> cls, String tabId) {
            this(route);
            mMasterCls = cls;
            mTabId = tabId;
        }

        public <Type extends FragmentInteractions> Route(String route, @Nullable Class<Type> cls) {
            this(route);
            mMasterCls = cls;
        }

        public Route(String route, ROUTE_TYPE route_type, List<String> queryParamNames) {
            this(route);
            mRouteType = route_type;
            mQueryParamNames = queryParamNames;
        }

        /**
         * Used for routes that should be treated as an external links
         * @param route
         * @param route_type
         */
        public Route(String route, ROUTE_TYPE route_type) {
            this(route);
            mRouteType = route_type;
        }

        /**
         * All constructors eventually get here
         * @param route
         */
        public Route(String route) {
            mRoute = route;

            Matcher matcher = Pattern.compile("/:([^/]*)").matcher(route); // match anything but a slash after a colon and create a group for the name of the param

            // Get the names of the params
            while (matcher.find()) {
                mParamNames.add(matcher.group(1));
            }

            Matcher paramValueMatcher = Pattern.compile("/:[^/]*").matcher(route); // match a slash, colon and then anything but a slash. Matched value is replaced so the param value can be parsed
            if (paramValueMatcher.find()) {
                String paramValueRegex = paramValueMatcher.replaceAll("/([^/]*)"); // Create a group where the param was, so the value can be located
                paramValueRegex = addLineMatchingAndOptionalEndSlash(paramValueRegex);
                mRoutePattern = Pattern.compile(paramValueRegex);
            } else { // does not contain params, just look for exact match
                mRoutePattern = Pattern.compile(addLineMatchingAndOptionalEndSlash(mRoute));
            }
        }
        // endregion

        /**
         * When a route is a match, the paramsHash, queryParamsHash, and Uri are set.
         * @param url
         * @return true is route is a match, false otherwise
         */
        public boolean apply(String url) {
            if (url == null) {
                return false;
            }
            Uri parsedUri = Uri.parse(url);
            String path = parsedUri.getPath();
            boolean isMatch = mRoutePattern.matcher(path).find();
            if (isMatch) {
                if (ROUTE_TYPE.NOT_INTERNALLY_ROUTED.equals(mRouteType)) {
                    return true; // recognized as a match so the unsupported fragment doesn't match it, then getInternalRoute will handle it
                }

                mUri = parsedUri;
                mParamsHash = createParamsHash(path);
                mQueryParamsHash = createQueryParamsHash();

                if (mQueryParamNames != null) {
                    return checkQueryParamNamesExist(mQueryParamNames, mQueryParamsHash.keySet());
                }

                mUrl = url;
            }
            return isMatch;
        }

        public boolean apply(Class<? extends FragmentInteractions> masterCls, Class<? extends FragmentInteractions> detailCls) {
            return (!ROUTE_TYPE.NOT_INTERNALLY_ROUTED.equals(mRouteType) && masterCls == mMasterCls && detailCls == mDetailCls);
        }

        public String createUrl(HashMap<String, String> replacementParams) {
            if(replacementParams == null) {
                return null;
            }

            String url = mRoute;
            for (String key : replacementParams.keySet()) {
                String keyParamIndicator = key;
                if (!keyParamIndicator.startsWith(":")) {
                    keyParamIndicator = ":" + key;
                }
                url = url.replaceAll(keyParamIndicator, replacementParams.get(key));
            }

            return url;
        }

        private boolean checkQueryParamNamesExist(List<String> expectedQueryParams, Set<String> actualQueryParams) {
            for (String expectedKey : expectedQueryParams) {
                if (!actualQueryParams.contains(expectedKey)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Adds '^' and '$' to regex for line matching
         * Also makes ending slash and api/v1 optional
         * @param regex
         * @return
         */
        private String addLineMatchingAndOptionalEndSlash(String regex) {
            if (regex.endsWith("/")) {
                regex = String.format("^(?:/api/v1)?%s?$", regex);
            } else {
                regex = String.format("^(?:/api/v1)?%s/?$", regex);
            }
            return regex;
        }

        /**
         * A param hash contains the key and values for the route
         * Example: If the route is /courses/:course_id and the url /courses/1234 is applied. The paramsHash will contain "course_id" -> "1234"
         * @param url
         * @return
         */
        private HashMap<String, String> createParamsHash(String url) {
            // TODO make this more bullet proof
            HashMap<String, String> params = new HashMap<>();
            Matcher matcher = mRoutePattern.matcher(url);
            ArrayList<String> paramValues = new ArrayList<>();
            if (matcher.find()) {
                for (int i = 0; i < matcher.groupCount(); i++) {
                    try {
                        // index 0 is the original string that was matched. Just get the group values
                        paramValues.add(matcher.group(i + 1));
                    } catch (Exception e) {
                        //do nothing
                    }
                }
            }
            for (int i = 0; i < mParamNames.size(); i++) {
                if (i < paramValues.size()) {
                    params.put(mParamNames.get(i), paramValues.get(i));
                }
            }

            return params;
        }

        /**
         * Query params for the url.
         * Example: The url /courses/1234?hello=world would have a Query params hash containing "hello" -> "world"
         * @return
         */
        private HashMap<String, String> createQueryParamsHash() {
            HashMap<String, String> queryParams = new HashMap<>();
            if (getUri() != null) {
                for (String param : getUri().getQueryParameterNames()) {
                    queryParams.put(param, getUri().getQueryParameter(param));
                }
            }
            return queryParams;
        }

        public CanvasContext.Type getContextType() {
            Matcher coursesMatcher = Pattern.compile("^/courses/?").matcher(mUri.getPath());
            if(coursesMatcher.find()) {
                return CanvasContext.Type.COURSE;
            }

            Matcher groupsMatcher = Pattern.compile("^/groups/?").matcher(mUri.getPath());
            if(groupsMatcher.find()) {
                return CanvasContext.Type.GROUP;
            }

            Matcher usersMatcher = Pattern.compile("^/users/?").matcher(mUri.getPath());
            if(usersMatcher.find()) {
                return CanvasContext.Type.USER;
            }

            return CanvasContext.Type.UNKNOWN;
        }

        // region Getter & Setters

        public ROUTE_TYPE getRouteType() {
            return mRouteType;
        }

        public Class<? extends FragmentInteractions> getMasterCls() {
            return mMasterCls;
        }

        public Class<? extends FragmentInteractions> getDetailCls() {
            return mDetailCls;
        }

        public String getTabId() {
            return mTabId;
        }

        public HashMap<String, String> getParamsHash() {
            return mParamsHash;
        }

        public String getFragmentIdentifier() {
            return getUri().getFragment();
        }

        public String getQueryString() {
            return getUri().getQuery();
        }

        public HashMap<String, String> getQueryParamsHash() {
            return mQueryParamsHash;
        }

        public String getUrl() {
            return mUrl;
        }

        public Uri getUri() {
            return mUri;
        }

        // endregion
    }

    /**
     * Gets a course id from a url, if url is invalid or could not be parsed a 0 will return.
     * @param url
     * @return
     */
    public static long getCourseIdFromURL(String url) {
        if(TextUtils.isEmpty(url)) {
            return 0L;
        }

        try {
            HashMap<String, String> params = new HashMap<>();
            for (Route r : sRoutes) {
                if (r.apply(url)) {
                    params = r.getParamsHash();
                    break;
                }
            }
            return Long.parseLong(params.get(Param.COURSE_ID));
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Gets a course id from a url, if url is invalid or could not be parsed a 0 will return.
     * @param urlString a valid url in the form of a string
     * @return a CanvasContext context_id (group_12345, course_12345)
     */
    public static String getContextIdFromURL(String urlString) {
        if(TextUtils.isEmpty(urlString)) return "";

        try {
            HashMap<String, String> params = new HashMap<>();
            Route route = null;
            for (Route r : sRoutes) {
                if (r.apply(urlString)) {
                    route = r;
                    params = r.getParamsHash();
                    break;
                }
            }
            return route != null ? CanvasContext.makeContextId(route.getContextType(), Long.parseLong(params.get(Param.COURSE_ID))) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isGroup(Uri uri) {
        Pattern pattern = Pattern.compile("^/group/?");
        Matcher matcher = pattern.matcher(uri.getPath());
        return matcher.find();
    }
}
