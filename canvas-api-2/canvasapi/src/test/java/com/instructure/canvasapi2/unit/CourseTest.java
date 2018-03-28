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

package com.instructure.canvasapi2.unit;

import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.Grades;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CourseTest {

    @Test
    public void isStudent_hasStudentEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(true, course.isStudent());
    }

    @Test
    public void isStudent_noStudentEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("teacher");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(false, course.isStudent());
    }

    @Test
    public void isStudent_noEnrollments() {
        Course course = new Course();
        course.setEnrollments(new ArrayList<Enrollment>());

        assertEquals(false, course.isStudent());
    }

    @Test
    public void isStudent_nullEnrollments() {
        Course course = new Course();
        course.setEnrollments(null);

        assertEquals(false, course.isStudent());
    }

    @Test
    public void isTeacher_hasTeacherEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("teacher");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(true, course.isTeacher());
    }

    @Test
    public void isTeacher_noTeacherEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(false, course.isTeacher());
    }

    @Test
    public void isTeacher_noEnrollments() {
        Course course = new Course();
        course.setEnrollments(new ArrayList<Enrollment>());

        assertEquals(false, course.isTeacher());
    }

    @Test
    public void isTeacher_nullEnrollments() {
        Course course = new Course();
        course.setEnrollments(null);

        assertEquals(false, course.isTeacher());
    }

    @Test
    public void isTA_hasTaEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("ta");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(true, course.isTA());
    }

    @Test
    public void isTA_noTaEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(false, course.isTA());
    }

    @Test
    public void isTA_noEnrollments() {
        Course course = new Course();
        course.setEnrollments(new ArrayList<Enrollment>());

        assertEquals(false, course.isTA());
    }

    @Test
    public void isTA_nullEnrollments() {
        Course course = new Course();
        course.setEnrollments(null);

        assertEquals(false, course.isTA());
    }

    @Test
    public void isObserver_hasObserverEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("observer");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(true, course.isObserver());
    }

    @Test
    public void isObserver_noObserverEnrollment() {
        Course course = new Course();
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertEquals(false, course.isObserver());
    }

    @Test
    public void isObserver_noEnrollments() {
        Course course = new Course();
        course.setEnrollments(new ArrayList<Enrollment>());

        assertEquals(false, course.isObserver());
    }

    @Test
    public void isObserver_nullEnrollments() {
        Course course = new Course();
        course.setEnrollments(null);

        assertEquals(false, course.isObserver());
    }

    @Test
    public void addEnrollment() {
        Course course = new Course();
        course.setEnrollments(null);

        Enrollment enrollment = new Enrollment();
        course.addEnrollment(enrollment);

        assertEquals(true, course.getEnrollments().contains(enrollment));
    }

    @Test
    public void licenseToAPIString_all() {
        for (Course.LICENSE license : Course.LICENSE.values()) {
            assertNotEquals("Expected valid API license string for Course.LICENSE." + license.name(), "", Course.licenseToAPIString(license));
        }
    }

    @Test
    public void licenseToAPIString_PRIVATE_COPYRIGHTED() {
        assertEquals("private", Course.licenseToAPIString(Course.LICENSE.PRIVATE_COPYRIGHTED));
    }

    @Test
    public void licenseToAPIString_CC_ATTRIBUTION_NON_COMMERCIAL_NO_DERIVATIVE() {
        assertEquals("cc_by_nc_nd", Course.licenseToAPIString(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL_NO_DERIVATIVE));
    }

    @Test
    public void licenseToAPIString_CC_ATTRIBUTION_NON_COMMERCIAL_SHARE_ALIKE() {
        assertEquals("c_by_nc_sa", Course.licenseToAPIString(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL_SHARE_ALIKE));
    }

    @Test
    public void licenseToAPIString_CC_ATTRIBUTION_NON_COMMERCIAL() {
        assertEquals("cc_by_nc", Course.licenseToAPIString(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL));
    }

    @Test
    public void licenseToAPIString_CC_ATTRIBUTION_NO_DERIVATIVE() {
        assertEquals("cc_by_nd", Course.licenseToAPIString(Course.LICENSE.CC_ATTRIBUTION_NO_DERIVATIVE));
    }

    @Test
    public void licenseToAPIString_CC_ATTRIBUTION_SHARE_ALIKE() {
        assertEquals("cc_by_sa", Course.licenseToAPIString(Course.LICENSE.CC_ATTRIBUTION_SHARE_ALIKE));
    }

    @Test
    public void licenseToAPIString_CC_ATTRIBUTION() {
        assertEquals("cc_by", Course.licenseToAPIString(Course.LICENSE.CC_ATTRIBUTION));
    }

    @Test
    public void licenseToAPIString_PUBLIC_DOMAIN() {
        assertEquals("public_domain", Course.licenseToAPIString(Course.LICENSE.PUBLIC_DOMAIN));
    }

    @Test
    public void licenseToAPIString_nullInput() {
        assertEquals(null, Course.licenseToAPIString(null));
    }

    @Test
    public void licenseToPrettyPrint_all() {
        for (Course.LICENSE license : Course.LICENSE.values()) {
            assertNotEquals("Expected valid pretty print string for Course.LICENSE." + license.name(), "", Course.licenseToPrettyPrint(license));
        }
    }

    @Test
    public void licenseToPrettyPrint_PRIVATE_COPYRIGHTED() {
        assertEquals("Private (Copyrighted)", Course.licenseToPrettyPrint(Course.LICENSE.PRIVATE_COPYRIGHTED));
    }

    @Test
    public void licenseToPrettyPrint_CC_ATTRIBUTION_NON_COMMERCIAL_NO_DERIVATIVE() {
        assertEquals("CC Attribution Non-Commercial No Derivatives", Course.licenseToPrettyPrint(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL_NO_DERIVATIVE));
    }

    @Test
    public void licenseToPrettyPrint_CC_ATTRIBUTION_NON_COMMERCIAL_SHARE_ALIKE() {
        assertEquals("CC Attribution Non-Commercial Share Alike", Course.licenseToPrettyPrint(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL_SHARE_ALIKE));
    }

    @Test
    public void licenseToPrettyPrint_CC_ATTRIBUTION_NON_COMMERCIAL() {
        assertEquals("CC Attribution Non-Commercial", Course.licenseToPrettyPrint(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL));
    }

    @Test
    public void licenseToPrettyPrint_CC_ATTRIBUTION_NO_DERIVATIVE() {
        assertEquals("CC Attribution No Derivatives", Course.licenseToPrettyPrint(Course.LICENSE.CC_ATTRIBUTION_NO_DERIVATIVE));
    }

    @Test
    public void licenseToPrettyPrint_CC_ATTRIBUTION_SHARE_ALIKE() {
        assertEquals("CC Attribution Share Alike", Course.licenseToPrettyPrint(Course.LICENSE.CC_ATTRIBUTION_SHARE_ALIKE));
    }

    @Test
    public void licenseToPrettyPrint_CC_ATTRIBUTION() {
        assertEquals("CC Attribution", Course.licenseToPrettyPrint(Course.LICENSE.CC_ATTRIBUTION));
    }

    @Test
    public void licenseToPrettyPrint_PUBLIC_DOMAIN() {
        assertEquals("Public Domain", Course.licenseToPrettyPrint(Course.LICENSE.PUBLIC_DOMAIN));
    }

    @Test
    public void getLicense_PRIVATE_COPYRIGHTED() {
        Course course = new Course();
        course.setLicense("private");
        assertEquals(Course.LICENSE.PRIVATE_COPYRIGHTED, course.getLicense());
    }

    @Test
    public void getLicense_CC_ATTRIBUTION_NON_COMMERCIAL_NO_DERIVATIVE() {
        Course course = new Course();
        course.setLicense("cc_by_nc_nd");
        assertEquals(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL_NO_DERIVATIVE, course.getLicense());
    }

    @Test
    public void getLicense_CC_ATTRIBUTION_NON_COMMERCIAL_SHARE_ALIKE() {
        Course course = new Course();
        course.setLicense("c_by_nc_sa");
        assertEquals(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL_SHARE_ALIKE, course.getLicense());
    }

    @Test
    public void getLicense_CC_ATTRIBUTION_NON_COMMERCIAL() {
        Course course = new Course();
        course.setLicense("cc_by_nc");
        assertEquals(Course.LICENSE.CC_ATTRIBUTION_NON_COMMERCIAL, course.getLicense());
    }

    @Test
    public void getLicense_CC_ATTRIBUTION_NO_DERIVATIVE() {
        Course course = new Course();
        course.setLicense("cc_by_nd");
        assertEquals(Course.LICENSE.CC_ATTRIBUTION_NO_DERIVATIVE, course.getLicense());
    }

    @Test
    public void getLicense_CC_ATTRIBUTION_SHARE_ALIKE() {
        Course course = new Course();
        course.setLicense("cc_by_sa");
        assertEquals(Course.LICENSE.CC_ATTRIBUTION_SHARE_ALIKE, course.getLicense());
    }

    @Test
    public void getLicense_CC_ATTRIBUTION() {
        Course course = new Course();
        course.setLicense("cc_by");
        assertEquals(Course.LICENSE.CC_ATTRIBUTION, course.getLicense());
    }

    @Test
    public void getLicense_PUBLIC_DOMAIN() {
        Course course = new Course();
        course.setLicense("public_domain");
        assertEquals(Course.LICENSE.PUBLIC_DOMAIN, course.getLicense());
    }

    @Test
    public void getLicense_empty() {
        Course course = new Course();
        course.setLicense("");
        assertEquals(Course.LICENSE.PRIVATE_COPYRIGHTED, course.getLicense());
    }

    @Test
    public void getLicense_all() {
        for (Course.LICENSE license : Course.LICENSE.values()) {
            Course course = new Course();
            course.setLicense(Course.licenseToAPIString(license));
            assertEquals(license, course.getLicense());
        }
    }

    @Test
    public void isCourseGradeLocked_hideFinal() {
        Course course = new Course();
        course.setHideFinalGrades(true);

        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);


        assertTrue(course.getCourseGrade(false).isLocked());
    }

    @Test
    public void isCourseGradeLocked_hideAllGradingPeriods() {
        Course course = new Course();
        course.setHasGradingPeriods(true);

        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setCurrentGradingPeriodId(0);
        enrollment.setMultipleGradingPeriodsEnabled(true);
        enrollment.setTotalsForAllGradingPeriodsOption(false);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).isLocked());
    }

    @Test
    public void courseHasNoCurrentGrade() {
        Course course = new Course();

        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setComputedCurrentGrade("");
        enrollment.setComputedCurrentScore(null);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getNoCurrentGrade());
    }

    @Test
    public void courseHasNoFinalGrade() {
        Course course = new Course();

        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setComputedFinalGrade("");
        enrollment.setComputedFinalScore(null);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getNoFinalGrade());
    }

    @Test
    public void courseHasCurrentGrade() {
        Course course = new Course();

        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setComputedCurrentGrade("A");
        enrollment.setComputedCurrentScore(95.0);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertFalse(course.getCourseGrade(false).getNoCurrentGrade());
    }

    @Test
    public void courseHasFinalGrade() {
        Course course = new Course();

        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setComputedFinalGrade("A");
        enrollment.setComputedFinalScore(95.0);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertFalse(course.getCourseGrade(false).getNoFinalGrade());
    }

    @Test
    public void courseGrade_currentGradeMGP() {
        Course course = new Course();
        course.setHasGradingPeriods(true);

        String currentGrade = "A";
        String finalGrade = "C";
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setCurrentGradingPeriodId(27);
        enrollment.setMultipleGradingPeriodsEnabled(true);
        enrollment.setCurrentPeriodComputedCurrentGrade(currentGrade);
        enrollment.setCurrentPeriodComputedFinalGrade(finalGrade);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getCurrentGrade().equals(currentGrade));
    }

    @Test
    public void courseGrade_currentScoreMGP() {
        Course course = new Course();
        course.setHasGradingPeriods(true);

        double currentScore = 96.0;
        double finalScore = 47.0;
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setCurrentGradingPeriodId(27);
        enrollment.setMultipleGradingPeriodsEnabled(true);
        enrollment.setCurrentPeriodComputedCurrentScore(currentScore);
        enrollment.setCurrentPeriodComputedFinalScore(finalScore);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getCurrentScore() == currentScore);
    }

    @Test
    public void courseGrade_currentGradeNonMGP() {
        Course course = new Course();

        String currentGrade = "A";
        String finalGrade = "C";
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setCurrentGradingPeriodId(27);
        enrollment.setComputedCurrentGrade(currentGrade);
        enrollment.setComputedFinalGrade(finalGrade);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getCurrentGrade().equals(currentGrade));
    }

    @Test
    public void courseGrade_currentScoreNonMGP() {
        Course course = new Course();

        double currentScore = 96.0;
        double finalScore = 47.0;
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setCurrentGradingPeriodId(27);
        enrollment.setComputedCurrentScore(currentScore);
        enrollment.setComputedFinalScore(finalScore);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getCurrentScore() == currentScore);
    }

    @Test
    public void courseGrade_finalGradeMGP() {
        Course course = new Course();
        course.setHasGradingPeriods(true);

        String currentGrade = "A";
        String finalGrade = "C";
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setCurrentGradingPeriodId(27);
        enrollment.setMultipleGradingPeriodsEnabled(true);
        enrollment.setCurrentPeriodComputedFinalGrade(finalGrade);
        enrollment.setCurrentPeriodComputedCurrentGrade(currentGrade);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getFinalGrade().equals(finalGrade));
    }

    @Test
    public void courseGrade_finalScoreMGP() {
        Course course = new Course();
        course.setHasGradingPeriods(true);

        double currentScore = 96.0;
        double finalScore = 47.0;
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setCurrentGradingPeriodId(27);
        enrollment.setMultipleGradingPeriodsEnabled(true);
        enrollment.setCurrentPeriodComputedFinalScore(finalScore);
        enrollment.setCurrentPeriodComputedCurrentScore(currentScore);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getFinalScore() == finalScore);
    }

    @Test
    public void courseGrade_finalGradeNonMGP() {
        Course course = new Course();

        String currentGrade = "A";
        String finalGrade = "C";
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setComputedFinalGrade(finalGrade);
        enrollment.setComputedCurrentGrade(currentGrade);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getFinalGrade().equals(finalGrade));
    }

    @Test
    public void courseGrade_finalScoreNonMGP() {
        Course course = new Course();

        double currentScore = 96.0;
        double finalScore = 47.0;
        Enrollment enrollment = new Enrollment();
        enrollment.setType("student");
        enrollment.setComputedFinalScore(finalScore);
        enrollment.setComputedCurrentScore(currentScore);

        ArrayList<Enrollment> enrollments = new ArrayList<>();
        enrollments.add(enrollment);

        course.setEnrollments(enrollments);

        assertTrue(course.getCourseGrade(false).getFinalScore() == finalScore);
    }

}