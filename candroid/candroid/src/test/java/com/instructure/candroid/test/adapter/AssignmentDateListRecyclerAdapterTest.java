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

import com.instructure.candroid.adapter.AssignmentDateListRecyclerAdapter;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.utils.APIHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Date;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class AssignmentDateListRecyclerAdapterTest extends InstrumentationTestCase {
    private AssignmentDateListRecyclerAdapter mAdapter;
    private AdapterToFragmentCallback<Assignment> mCallback;

    /**
     * Make it so the protected constructor can be called
     */
    public static class AssignmentDateListRecyclerAdapterWrapper extends AssignmentDateListRecyclerAdapter {
        protected AssignmentDateListRecyclerAdapterWrapper(Context context) {
            super(context);
        }
    }

    @Before
    public void setup() {
        mAdapter = new AssignmentDateListRecyclerAdapterWrapper(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testAreContentsTheSame_sameName() {
        Assignment assignment  = new Assignment();
        assignment.setName("Assign1");
        assertTrue(mAdapter.createItemCallback().areContentsTheSame(assignment, assignment));
    }

    @Test
    public void testAreContentsTheSame_differentName() {
        Assignment assignment1  = new Assignment();
        assignment1.setName("Assign1");
        Assignment assignment2  = new Assignment();
        assignment2.setName("Assign2");
        assertFalse(mAdapter.createItemCallback().areContentsTheSame(assignment1, assignment2));
    }

    @Test
    public void testAreContentsTheSame_oneNullDueDate() {
        Assignment assignmentDueDate  = new Assignment();
        assignmentDueDate.setName("Assign1");
        assignmentDueDate.setDueAt(APIHelper.dateToString(new Date()));
        Assignment assignment1  = new Assignment();
        assignment1.setName("Assign1");
        assertFalse(mAdapter.createItemCallback().areContentsTheSame(assignmentDueDate, assignment1));
        assertFalse(mAdapter.createItemCallback().areContentsTheSame(assignment1, assignmentDueDate));
        assertTrue(mAdapter.createItemCallback().areContentsTheSame(assignmentDueDate, assignmentDueDate));
    }
}
