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
 *
 */

package com.instructure.teacher.unit;

import com.instructure.interactions.router.Route;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

@Config(sdk = 17)
@RunWith(RobolectricTestRunner.class)
public class RouteTest extends Assert {

    //region Matching
    @Test
    public void testRouteMatching() {
        Route route = new Route("/courses");
        assertFalse(route.apply("http://mobiledev.instructure.com/courses/953090/"));

        assertFalse(route.apply("http://mobiledev.instructure.com/courses/953090")); // no slash at the end
    }

    //endregion

    //region Params
    @Test
    public void testRouteNoParams() {
        HashMap<String, String> expectedParams = new HashMap<>();

        Route route = new Route("/courses");
        assertTrue(route.apply("http://mobiledev.instructure.com/courses/"));
        assertEquals(expectedParams, route.getParamsHash());

        assertTrue(route.apply("http://mobiledev.instructure.com/courses/")); // no slash at the end
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testRouteTwoCharParam() {
        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put("course_id", "833052");
        expectedParams.put("page_id", "page-3");

        Route route = new Route("/courses/:course_id/pages/:page_id");
        assertTrue(route.apply("https://mobiledev.instructure.com/courses/833052/pages/page-3/"));
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testRouteOneIntParam() {
        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put("course_id", "953090");

        Route route = new Route("/(?:courses|groups)/:course_id");
        assertTrue(route.apply("http://mobiledev.instructure.com/courses/953090/"));
        assertEquals(expectedParams, route.getParamsHash());

        route = new Route("/courses/:course_id/"); // Test with a optional slash at the end
        assertTrue(route.apply("http://mobiledev.instructure.com/courses/953090")); // no slash at the end
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testRouteTwoIntParams() {
        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put("course_id", "833052");
        expectedParams.put("file_id", "39506637");

        Route route = new Route("/courses/:course_id/files/:file_id");
        assertTrue(route.apply("https://mobiledev.instructure.com/courses/833052/files/39506637/"));
        assertEquals(expectedParams, route.getParamsHash());

        assertTrue(route.apply("https://mobiledev.instructure.com/courses/833052/files/39506637")); // no slash at the end
        assertEquals(expectedParams, route.getParamsHash());
    }

    @Test
    public void testRouteThreeIntParams() {
        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put("course_id", "953090");
        expectedParams.put("assignment_id", "2651861");
        expectedParams.put("submission_id", "3690827");

        Route route = new Route("/(?:courses|groups)/:course_id/assignments/:assignment_id/submissions/:submission_id");
        assertTrue(route.apply("http://mobiledev.instructure.com/courses/953090/assignments/2651861/submissions/3690827/"));
        assertEquals(expectedParams, route.getParamsHash());

        assertTrue(route.apply("http://mobiledev.instructure.com/courses/953090/assignments/2651861/submissions/3690827")); // no slash at the end
        assertEquals(expectedParams, route.getParamsHash());
    }

    //endregion

    //region Query Params
    @Test
    public void testRouteQueryParams() {
        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put("course_id", "836357");
        expectedParams.put("quiz_id", "990775");

        Route route = new Route("/courses/:course_id/quizzes/:quiz_id");
        assertTrue(route.apply("https://mobiledev.instructure.com/courses/836357/quizzes/990775?module_item_id=6723096/"));
        assertEquals(expectedParams, route.getParamsHash());
        assertEquals("module_item_id=6723096/", route.getQueryString());

        assertTrue(route.apply("https://mobiledev.instructure.com/courses/836357/quizzes/990775?module_item_id=6723096")); // no slash at the end
        assertEquals(expectedParams, route.getParamsHash());
        assertEquals("module_item_id=6723096", route.getQueryString());
    }
    
    //endregion

    /*
     * Fragment Identifier
     */
    @Test
    public void testRouteFragmentIdentifierParams() {
        HashMap<String, String> expectedParams = new HashMap<>();
        expectedParams.put("course_id", "836357");
        expectedParams.put("quiz_id", "990775");

        Route route = new Route("/courses/:course_id/quizzes/:quiz_id");
        assertTrue(route.apply("https://mobiledev.instructure.com/courses/836357/quizzes/990775?module_item_id=6723096#Fragment Identifier"));
        assertEquals(expectedParams, route.getParamsHash());
        assertEquals("module_item_id=6723096", route.getQueryString());
        assertEquals("Fragment Identifier", route.getFragmentIdentifier());
    }
}
