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

package com.instructure.candroid.test.adapter;

import android.content.Context;
import android.test.InstrumentationTestCase;
import com.instructure.candroid.adapter.GradesListRecyclerAdapter;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.Submission;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class GradesListRecyclerAdapterTest extends InstrumentationTestCase {
    private GradesListRecyclerAdapter mAdapter;

    /**
     * Make it so the protected constructor can be called
     */
    public static class GradesListRecyclerAdapterWrapper extends GradesListRecyclerAdapter {
        protected GradesListRecyclerAdapterWrapper(Context context) {
            super(context);
        }
    }

    @Before
    public void setup(){
        mAdapter = new GradesListRecyclerAdapterWrapper(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testAreContentsTheSame_SameNameAndPoints(){
        Assignment assignment = new Assignment();
        assignment.setName("assignment");
        assignment.setPointsPossible(0.0);
        assertTrue(mAdapter.createItemCallback().areContentsTheSame(assignment, assignment));
    }
    
    @Test
    public void testAreContentsTheSame_DifferentName(){
        Assignment assignment1 = new Assignment();
        assignment1.setName("assignment1");
        assignment1.setPointsPossible(0.0);

        Assignment assignment2 = new Assignment();
        assignment2.setName("assignment2");
        assignment2.setPointsPossible(0.0);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(assignment1, assignment2));
    }

    @Test
    public void testAreContentsTheSame_DifferentScore(){
        Assignment assignment1 = new Assignment();
        assignment1.setName("assignment1");
        assignment1.setPointsPossible(0.0);

        Assignment assignment2 = new Assignment();
        assignment2.setName("assignment1");
        assignment2.setPointsPossible(1.0);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(assignment1, assignment2));
    }

    @Test
    public void testAreContentsTheSame_SameWithSubmission(){
        Assignment assignment = new Assignment();
        assignment.setName("assignment");
        assignment.setPointsPossible(0.0);
        Submission submission = new Submission();
        submission.setGrade("A");
        assignment.setSubmission(submission);

        assertTrue(mAdapter.createItemCallback().areContentsTheSame(assignment, assignment));
    }

    @Test
    public void testAreContentsTheSame_SameWithSubmissionNullChange(){
        Assignment assignment1 = new Assignment();
        assignment1.setName("assignment");
        assignment1.setPointsPossible(0.0);
        Submission submission1 = new Submission();
        submission1.setGrade("A");
        assignment1.setSubmission(submission1);

        Assignment assignment2 = new Assignment();
        assignment2.setName("assignment1");
        assignment2.setPointsPossible(0.0);
        assignment2.setSubmission(null);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(assignment1, assignment2));
    }

    @Test
    public void testAreContentsTheSame_SameWithSubmissionNullGrade(){
        Assignment assignment1 = new Assignment();
        assignment1.setName("assignment");
        assignment1.setPointsPossible(0.0);
        Submission submission1 = new Submission();
        submission1.setGrade("A");
        assignment1.setSubmission(submission1);

        Assignment assignment2 = new Assignment();
        assignment2.setName("assignment1");
        assignment2.setPointsPossible(0.0);
        Submission submission2 = new Submission();
        submission1.setGrade(null);
        assignment1.setSubmission(submission2);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(assignment1, assignment2));
    }
}
