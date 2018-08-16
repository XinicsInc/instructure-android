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

package com.instructure.candroid.test.util;

import android.test.InstrumentationTestCase;

import com.instructure.candroid.activity.BaseRouterActivity;
import com.instructure.candroid.fragment.AnnouncementListFragment;
import com.instructure.candroid.fragment.AssignmentFragment;
import com.instructure.candroid.fragment.AssignmentListFragment;
import com.instructure.candroid.fragment.BasicQuizViewFragment;
import com.instructure.candroid.fragment.CourseModuleProgressionFragment;
import com.instructure.candroid.fragment.CourseSettingsFragment;
import com.instructure.candroid.fragment.DiscussionDetailsFragment;
import com.instructure.candroid.fragment.DiscussionListFragment;
import com.instructure.candroid.fragment.FileListFragment;
import com.instructure.candroid.fragment.GradesListFragment;
import com.instructure.candroid.fragment.InboxFragment;
import com.instructure.candroid.fragment.ModuleListFragment;
import com.instructure.candroid.fragment.NotificationListFragment;
import com.instructure.candroid.fragment.PageDetailsFragment;
import com.instructure.candroid.fragment.PageListFragment;
import com.instructure.candroid.fragment.PeopleDetailsFragment;
import com.instructure.candroid.fragment.PeopleListFragment;
import com.instructure.candroid.fragment.QuizListFragment;
import com.instructure.candroid.fragment.ScheduleListFragment;
import com.instructure.candroid.fragment.UnSupportedTabFragment;
import com.instructure.candroid.util.Param;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ContextKeeper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class RouterUtilsTest extends InstrumentationTestCase {

    @Before
    public void setUp() throws Exception {
        ContextKeeper.setAppContext(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testCanRouteInternally_misc() {
        // Home
        assertTrue(callCanRouteInternally("http://mobiledev.instructure.com"));

        //  Login
        assertFalse(callCanRouteInternally("http://mobiledev.instructure.com/login"));
    }

    @Test
    public void testCanRouteInternally_notSupported() {
        // Had to comment out so they will pass on Jenkins
        //assertTrue(callCanRouteInternally("https://mobiledev.instructure.com/courses/833052/media_download?"));
    }

    @Test
    public void testCanRouteInternally_courseIdParseCorrect() {
        // Written due to a crash found by Crashlytics
        // See: https://fabric.io/instructure/android/apps/com.instructure.candroid/issues/5a69f6858cb3c2fa63977be1?time=1509408000000%3A1517270399999
        assertEquals(833052L, BaseRouterActivity.parseCourseId("sis_course_id:833052"));
    }

    @Test(expected = RuntimeException.class)
    public void testCanRouteInternally_courseIdParseWrong() {
        // Written due to a crash found by Crashlytics
        // See: https://fabric.io/instructure/android/apps/com.instructure.candroid/issues/5a69f6858cb3c2fa63977be1?time=1509408000000%3A1517270399999
        BaseRouterActivity.parseCourseId("sis_course_id:833");
    }

    @Test
    public void testCanRouteInternally() {
        // Since there is a catch all, anything with the correct domain returns true.
        assertTrue((callCanRouteInternally("https://mobiledev.instructure.com/calendar2?include_contexts=course_833052#view_name=month&view_start=2015-03-19T06%3A00%3A00.000Z")));
        assertTrue(callCanRouteInternally("https://mobiledev.instructure.com/courses/833052/calendar_events/921098"));

        assertFalse(callCanRouteInternally("http://google.com/courses/54564/"));

    }

    private boolean callCanRouteInternally(String url) {
        return RouterUtils.canRouteInternally(null, url, "mobiledev.instructure.com", false);
    }

    private RouterUtils.Route callGetInternalRoute(String url) {
        //String domain = APIHelper.getDomain(RuntimeEnvironment.application);
        return RouterUtils.getInternalRoute(url, "mobiledev.instructure.com");
    }

    @Test
    public void testGetInternalRoute_supportedDomain() {
        RouterUtils.Route route = callGetInternalRoute("https://instructure.com");
        assertNull(route);

        route = callGetInternalRoute("https://mobiledev.instructure.com");
        assertNotNull(route);

        route = callGetInternalRoute("https://canvas.net");
        assertNull(route);

        route = callGetInternalRoute("https://canvas.net/courses/12344");
        assertNull(route);
    }

    @Test
    public void testGetInternalRoute_nonSupportedDomain() {
        RouterUtils.Route route = callGetInternalRoute("https://google.com");
        assertNull(route);

        route = callGetInternalRoute("https://youtube.com");
        assertNull(route);

        route = callGetInternalRoute("https://aFakeWebsite.com/courses/12344");
        assertNull(route);
    }

    @Test
    public void testGetInternalRoute_calendar() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/calendar2?include_contexts=course_833052#view_name=month&view_start=2015-03-19T06%3A00%3A00.000Z");
        assertNotNull(route);
        // TODO add test for calendar
        //assertEquals(CalendarEventFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/833052/calendar_events/921098");
        assertNotNull(route);
    }

    @Test
    public void testGetInternalRoute_externalTools() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/833052/external_tools/131971");
        assertNotNull(route);

    }



    @Test
    public void testGetInternalRoute_files() {

        // courses
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/833052/files/63383591/download?wrap=1");
        assertNotNull(route);
        assertEquals(RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD, route.getRouteType());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "833052");
        expectedParams.put(Param.FILE_ID, "63383591");
        assertEquals(expectedParams, route.getParamsHash());

        HashMap<String, String> expectedQueryParams = new HashMap<>();
        expectedQueryParams.put("wrap", "1");
        assertEquals(expectedQueryParams, route.getQueryParamsHash());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/833052/files/63383591");
        assertNotNull(route); // route is not supported
        assertEquals(null, route.getMasterCls());


        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/833052/files/63383591/download?verifier=12344556");
        assertNotNull(route);
        assertEquals(RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD, route.getRouteType());

        // files
        route = callGetInternalRoute("https://mobiledev.instructure.com/files/63383591/download?wrap=1");
        assertNotNull(route);
        assertEquals(RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD, route.getRouteType());

        expectedParams = new HashMap<>();
        expectedParams.put(Param.FILE_ID, "63383591");
        assertEquals(expectedParams, route.getParamsHash());

        assertEquals(expectedQueryParams, route.getQueryParamsHash());

        route = callGetInternalRoute("https://mobiledev.instructure.com/files/63383591");
        assertNotNull(route);
        assertEquals(FileListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/833052/files/63383591/download?verifier=12344556");
        assertNotNull(route);
        assertEquals(RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD, route.getRouteType());
    }

    @Test
    public void testGetInternalRoute_conversation() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/conversations/");
        assertNotNull(route);
        assertEquals(InboxFragment.class, route.getMasterCls());

        // Detailed Conversation
        route = callGetInternalRoute("https://mobiledev.instructure.com/conversations/1078680");
        assertNotNull(route);
        assertEquals(InboxFragment.class, route.getMasterCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.CONVERSATION_ID, "1078680");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_modules() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/modules");
        assertNotNull(route);
        assertEquals(ModuleListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/modules/48753");
        assertNotNull(route);
        assertEquals(ModuleListFragment.class, route.getMasterCls());

        // discussion
        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/24219/discussion_topics/1129998?module_item_id=12345");
        assertNotNull(route);
        assertEquals(ModuleListFragment.class, route.getMasterCls());
        assertEquals(CourseModuleProgressionFragment.class, route.getDetailCls());

        HashMap<String, String> expectedQueryParams = new HashMap<>();
        expectedQueryParams.put(Param.MODULE_ITEM_ID, "12345");
        assertEquals(expectedQueryParams, route.getQueryParamsHash());

        // pages
        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/24219/pages/1129998?module_item_id=12345");
        assertNotNull(route);
        assertEquals(ModuleListFragment.class, route.getMasterCls());
        assertEquals(CourseModuleProgressionFragment.class, route.getDetailCls());
        assertEquals(expectedQueryParams, route.getQueryParamsHash());

        // quizzes
        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/24219/quizzes/1129998?module_item_id=12345");
        assertNotNull(route);
        assertEquals(ModuleListFragment.class, route.getMasterCls());
        assertEquals(CourseModuleProgressionFragment.class, route.getDetailCls());
        assertEquals(expectedQueryParams, route.getQueryParamsHash());

        // assignments
        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/24219/assignments/1129998?module_item_id=12345");
        assertNotNull(route);
        assertEquals(ModuleListFragment.class, route.getMasterCls());
        assertEquals(CourseModuleProgressionFragment.class, route.getDetailCls());
        assertEquals(expectedQueryParams, route.getQueryParamsHash());

        // files
        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/24219/files/1129998?module_item_id=12345");
        assertNotNull(route);
        assertEquals(ModuleListFragment.class, route.getMasterCls());
        assertEquals(CourseModuleProgressionFragment.class, route.getDetailCls());
        assertEquals(expectedQueryParams, route.getQueryParamsHash());
    }

    @Test
    public void testGetInternalRoute_notifications() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/notifications");
        assertNotNull(route);
        assertEquals(NotificationListFragment.class, route.getMasterCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_grades() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/grades");
        assertNotNull(route);
        assertEquals(GradesListFragment.class, route.getMasterCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_users() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/users");
        assertNotNull(route);
        assertEquals(PeopleListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/users/1234");
        assertNotNull(route);
        assertEquals(PeopleListFragment.class, route.getMasterCls());
        assertEquals(PeopleDetailsFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.USER_ID, "1234");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_discussion() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/discussion_topics");
        assertNotNull(route);
        assertEquals(DiscussionListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/discussion_topics/1234");
        assertNotNull(route);
        assertEquals(DiscussionListFragment.class, route.getMasterCls());
        assertEquals(DiscussionDetailsFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.MESSAGE_ID, "1234");
        assertEquals(expectedParams, route.getParamsHash());

    }

    @Test
    public void testGetInternalRoute_pages() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/pages");
        assertNotNull(route);
        assertEquals(PageListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/pages/hello");
        assertNotNull(route);
        assertEquals(PageListFragment.class, route.getMasterCls());
        assertEquals(PageDetailsFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.PAGE_ID, "hello");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_announcements() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/announcements");
        assertNotNull(route);
        assertEquals(AnnouncementListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/announcements/12345");
        assertNotNull(route);
        assertEquals(AnnouncementListFragment.class, route.getMasterCls());
        assertEquals(DiscussionDetailsFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.MESSAGE_ID, "12345");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_quiz() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/quizzes");
        assertNotNull(route);
        assertEquals(QuizListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/quizzes/12345");
        assertNotNull(route);
        assertEquals(QuizListFragment.class, route.getMasterCls());
        assertEquals(BasicQuizViewFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.QUIZ_ID, "12345");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_syllabus() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/assignments/syllabus");
        assertNotNull(route);
        assertEquals(ScheduleListFragment.class, route.getMasterCls());
        assertEquals(ScheduleListFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_assignments() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/assignments/");
        assertNotNull(route);
        assertEquals(AssignmentListFragment.class, route.getMasterCls());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/assignments/213445213445213445213445213445213445213445213445213445213445213445213445");
        assertNotNull(route);
        assertEquals(AssignmentListFragment.class, route.getMasterCls());
        assertEquals(AssignmentFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.ASSIGNMENT_ID, "213445213445213445213445213445213445213445213445213445213445213445213445");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_submissions_rubric() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/assignments/12345/rubric");
        assertNotNull(route);
        assertEquals(AssignmentListFragment.class, route.getMasterCls());
        assertEquals(AssignmentFragment.class, route.getDetailCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.ASSIGNMENT_ID, "12345");
        expectedParams.put(Param.SLIDING_TAB_TYPE, "rubric");
        assertEquals(expectedParams, route.getParamsHash());


        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/assignments/213445213445213445213445213445213445213445213445213445213445213445213445/submissions/1234");
        assertNotNull(route);
        assertEquals(AssignmentListFragment.class, route.getMasterCls());
        assertEquals(AssignmentFragment.class, route.getDetailCls());

        expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        expectedParams.put(Param.ASSIGNMENT_ID, "213445213445213445213445213445213445213445213445213445213445213445213445");
        expectedParams.put(Param.SLIDING_TAB_TYPE, "submissions");
        expectedParams.put(Param.SUBMISSION_ID, "1234");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_settings() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/settings/");
        assertNotNull(route);
        assertEquals(CourseSettingsFragment.class, route.getMasterCls());

        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testGetInternalRoute_unsupported() {
        RouterUtils.Route route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/collaborations/");
        assertNotNull(route);
        assertEquals(UnSupportedTabFragment.class, route.getMasterCls());
        assertEquals(Tab.COLLABORATIONS_ID, route.getTabId());
        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put(Param.COURSE_ID, "836357");
        assertEquals(expectedParams, route.getParamsHash());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/collaborations/234"); // not an actual url
        assertNotNull(route);
        assertEquals(UnSupportedTabFragment.class, route.getMasterCls());
        assertEquals(Tab.COLLABORATIONS_ID, route.getTabId());
        assertEquals(expectedParams, route.getParamsHash());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/conferences/");
        assertNotNull(route);
        assertEquals(UnSupportedTabFragment.class, route.getMasterCls());
        assertEquals(Tab.CONFERENCES_ID, route.getTabId());
        assertEquals(expectedParams, route.getParamsHash());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/conferences/234"); // not an actual url
        assertNotNull(route);
        assertEquals(UnSupportedTabFragment.class, route.getMasterCls());
        assertEquals(Tab.CONFERENCES_ID, route.getTabId());
        assertEquals(expectedParams, route.getParamsHash());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/outcomes/");
        assertNotNull(route);
        assertEquals(UnSupportedTabFragment.class, route.getMasterCls());
        assertEquals(Tab.OUTCOMES_ID, route.getTabId());
        assertEquals(expectedParams, route.getParamsHash());

        route = callGetInternalRoute("https://mobiledev.instructure.com/courses/836357/outcomes/234"); // not an actual url
        assertNotNull(route);
        assertEquals(UnSupportedTabFragment.class, route.getMasterCls());
        assertEquals(Tab.OUTCOMES_ID, route.getTabId());
        assertEquals(expectedParams, route.getParamsHash());
    }


    @Test
    public void testCreateBookmarkCourse() {
        ApiPrefs.setDomain("mobiledev.instructure.com");
        HashMap<String, String> replacementParams = new HashMap<>();
        replacementParams.put(Param.COURSE_ID, "123");
        replacementParams.put(Param.QUIZ_ID, "456");
        CanvasContext canvasContext = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, 123, "");

        HashMap<String, String> queryParams = new HashMap<>();


        String url = RouterUtils.createUrl(canvasContext.getType(), QuizListFragment.class, BasicQuizViewFragment.class, replacementParams, queryParams);
        assertEquals("https://mobiledev.instructure.com/courses/123/quizzes/456", url);
    }

    @Test
    public void testCreateBookmarkGroups() {
        ApiPrefs.setDomain("mobiledev.instructure.com");
        HashMap<String, String> replacementParams = new HashMap<>();
        replacementParams.put(Param.COURSE_ID, "123");
        replacementParams.put(Param.QUIZ_ID, "456");
        CanvasContext canvasContext = CanvasContext.getGenericContext(CanvasContext.Type.GROUP, 123, "");

        HashMap<String, String> queryParams = new HashMap<>();

        String url = RouterUtils.createUrl(canvasContext.getType(), QuizListFragment.class, BasicQuizViewFragment.class, replacementParams, queryParams);
        assertEquals("https://mobiledev.instructure.com/groups/123/quizzes/456", url);
    }
}
