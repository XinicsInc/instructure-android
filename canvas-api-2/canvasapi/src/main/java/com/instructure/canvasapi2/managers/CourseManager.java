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

package com.instructure.canvasapi2.managers;

import android.support.annotation.NonNull;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.CourseAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.CanvasContextPermission;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.Favorite;
import com.instructure.canvasapi2.models.GradingPeriodResponse;
import com.instructure.canvasapi2.models.Group;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.tests.CourseManager_Test;
import com.instructure.canvasapi2.utils.ExhaustiveListCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;


public class CourseManager extends BaseManager {

    public static boolean mTesting = false;

    public static void getFavoriteCourses(StatusCallback<List<Course>> callback, boolean forceNetwork) {

        if (isTesting() || mTesting) {
            CourseManager_Test.getFavoriteCourses(callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getFavoriteCourses(adapter, callback, params);
        }
    }

    public static void getAllFavoriteCourses(final boolean forceNetwork, StatusCallback<List<Course>> callback) {
        if (isTesting() || mTesting) {
            CourseManager_Test.getCourses(callback);
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            StatusCallback<List<Course>> depaginatedCallback = new ExhaustiveListCallback<Course>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<Course>> callback, @NonNull String nextUrl, boolean isCached) {
                    CourseAPI.getNextPageFavoriteCourses(forceNetwork, nextUrl, adapter, callback);
                }
            };

            adapter.setStatusCallback(depaginatedCallback);
            CourseAPI.getFirstPageFavoriteCourses(adapter, depaginatedCallback, params);
        }
    }


    public static void getCourses(final boolean forceNetwork, StatusCallback<List<Course>> callback) {
        if (isTesting() || mTesting) {
            CourseManager_Test.getCourses(callback);
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            StatusCallback<List<Course>> depaginatedCallback = new ExhaustiveListCallback<Course>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<Course>> callback, @NonNull String nextUrl, boolean isCached) {
                    CourseAPI.getNextPageCourses(forceNetwork, nextUrl, adapter, callback);
                }
            };

            adapter.setStatusCallback(depaginatedCallback);
            CourseAPI.getFirstPageCourses(adapter, depaginatedCallback, params);
        }
    }

    public static void getGradingPeriodsForCourse(StatusCallback<GradingPeriodResponse> callback, long courseId, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            CourseManager_Test.getGradingPeriodsForCourse(callback, courseId);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getGradingPeriodsForCourse(adapter, callback, params, courseId);
        }
    }

    public static void getCourse(long courseId, StatusCallback<Course> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            CourseManager_Test.getCourse(courseId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getCourse(courseId, adapter, callback, params);
        }
    }

    public static void getCourseWithSyllabus(long courseId, StatusCallback<Course> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            CourseManager_Test.getCourse(courseId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getCourseWithSyllabus(courseId, adapter, callback, params);
        }
    }

    public static void getCourseWithGrade(long courseId, StatusCallback<Course> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getCourseWithGrade(courseId, adapter, callback, params);
        }
    }

    public static void getCourseStudents(long courseId, StatusCallback<List<User>> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            CourseManager_Test.getCourseStudents(courseId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getCourseStudents(courseId, adapter, callback, params);
        }
    }


    public static void getAllCourseStudents(final boolean forceNetwork, long courseId, StatusCallback<List<User>> callback) {
        if (isTesting() || mTesting) {
            // TODO...
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            StatusCallback<List<User>> depaginatedCallback = new ExhaustiveListCallback<User>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<User>> callback, @NonNull String nextUrl, boolean isCached) {
                    CourseAPI.getNextPageCourseStudents(forceNetwork, nextUrl, adapter, callback);
                }
            };

            adapter.setStatusCallback(depaginatedCallback);
            CourseAPI.getFirstPageCourseStudents(courseId, adapter, depaginatedCallback, params);
        }
    }

    public static void getCourseStudent(long courseId, long studentId, StatusCallback<User> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            CourseManager_Test.getCourseStudent(courseId, studentId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getCourseStudent(courseId, studentId, adapter, callback, params);
        }
    }

    public static void addCourseToFavorites(long courseId, StatusCallback<Favorite> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            CourseManager_Test.addCourseToFavorites(courseId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.addCourseToFavorites(courseId, adapter, callback, params);
        }
    }

    public static void removeCourseFromFavorites(long courseId, StatusCallback<Favorite> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            CourseManager_Test.removeCourseFromFavorites(courseId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.removeCourseFromFavorites(courseId, adapter, callback, params);
        }
    }

    public static void editCourseName(long courseId, String newCourseName, StatusCallback<Course> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO:
            // CourseManager_Test.editCourseName(courseId, callback);
        } else {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("course[name]", newCourseName);

            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.updateCourse(courseId, queryParams, adapter, callback, params);
        }
    }

    public static void editCourseHomePage(long courseId, String newHomePage, boolean forceNetwork, StatusCallback<Course> callback) {
        if (isTesting() || mTesting) {
            // TODO:
            // CourseManager_Test.editCourseName(courseId, callback);
        } else {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("course[default_view]", newHomePage);

            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.updateCourse(courseId, queryParams, adapter, callback, params);
        }
    }

    public static void getCoursesWithEnrollmentType(boolean forceNetwork, StatusCallback<List<Course>> callback, String type) {
        if (isTesting() || mTesting) {
            // TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            CourseAPI.getCoursesByEnrollmentType(adapter, callback, params, type);
        }
    }

    public static void getGroupsForCourse(long courseId, StatusCallback<List<Group>> callback, final boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();
            final RestBuilder adapter = new RestBuilder(callback);
            StatusCallback<List<Group>> exhaustiveCallback = new ExhaustiveListCallback<Group>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<Group>> callback, @NonNull String nextUrl, boolean isCached) {
                    CourseAPI.getNextPageGroups(nextUrl, adapter, callback, params);
                }
            };
            adapter.setStatusCallback(exhaustiveCallback);
            CourseAPI.getFirstPageGroups(courseId, adapter, exhaustiveCallback, params);
        }
    }

    public static void getCoursePermissions(long courseId, List<String> requestedPermissions, StatusCallback<CanvasContextPermission> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .build();

            CourseAPI.getCoursePermissions(courseId, requestedPermissions, adapter, callback, params);
        }
    }

    public static void getEnrollmentsForGradingPeriod(long courseId, long gradingPeriodId, StatusCallback<List<Enrollment>> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CourseAPI.getEnrollmentsForGradingPeriod(courseId, gradingPeriodId, adapter, params, callback);
        }
    }

    //region Airwolf
    public static void getCourseWithGradeAirwolf(String airwolfDomain, String parentId, String studentId, long courseId, StatusCallback<Course> callback) {
        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .build();

            CourseAPI.getCourseWithGradeAirwolf(parentId, studentId, courseId, adapter, callback, params);
        }
    }

    public static void getCourseWithSyllabusAirwolf(String airwolfDomain, String parentId, String studentId, long courseId, StatusCallback<Course> callback) {
        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .build();

            CourseAPI.getCourseWithSyllabusAirwolf(parentId, studentId, courseId, adapter, callback, params);
        }
    }

    public static void getCoursesForUserAirwolf(String airwolfDomain, String parentId, String studentId, boolean forceNetwork, StatusCallback<List<Course>> callback) {
        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .build();

            CourseAPI.getCoursesForUserAirwolf(parentId, studentId, adapter, callback, params);
        }
    }
    //endregion

    @NonNull
    public static List<Course> getFavoriteCoursesSynchronous(final boolean forceNetwork) throws IOException {
        if (isTesting() || mTesting) {
            //TODO
            return new ArrayList<>();
        } else {
            final RestBuilder adapter = new RestBuilder();
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();


            Response<List<Course>> response = CourseAPI.getFavoriteCoursesSynchronously(adapter, params);
            if(response != null && response.isSuccessful() && response.body() != null) return response.body();
            else return new ArrayList<>();
        }
    }

    @NonNull
    public static List<Course> getCoursesSynchronous(final boolean forceNetwork) throws IOException {
        if (isTesting() || mTesting) {
            //TODO
            return new ArrayList<>();
        } else {
            final RestBuilder adapter = new RestBuilder();
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();


            List<Course> data = CourseAPI.getCoursesSynchronously(adapter, params);
            if(data != null) return data;
            else return new ArrayList<>();
        }
    }


    public static Map<Long, Course> createCourseMap(List<Course> courses) {
        Map<Long, Course> courseMap = new HashMap<Long, Course>();
        if (courses == null) {
            return courseMap;
        }
        for (Course course : courses) {
            courseMap.put(course.getId(), course);
        }
        return courseMap;
    }
}
