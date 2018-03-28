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

import com.instructure.candroid.adapter.RubricRecyclerAdapter;
import com.instructure.candroid.model.RubricCommentItem;
import com.instructure.candroid.model.RubricRatingItem;
import com.instructure.canvasapi2.models.RubricCriterionRating;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class RubricRecyclerAdapterTest extends InstrumentationTestCase {
    private RubricRecyclerAdapter mAdapter;

    /**
     * Make it so the protected constructor can be called
     */
    public static class RubricRecyclerAdapterWrapper extends RubricRecyclerAdapter {
        protected RubricRecyclerAdapterWrapper(Context context) {
            super(context);
        }
    }

    @Before
    public void setup(){
        mAdapter = new RubricRecyclerAdapterWrapper(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testAreContentsTheSame_SameNotComment(){
        RubricCriterionRating rating = new RubricCriterionRating();
        rating.setDescription("item");
        RubricRatingItem item = new RubricRatingItem(rating);


        assertTrue(mAdapter.createItemCallback().areContentsTheSame(item, item));
    }

    @Test
    public void testAreContentsTheSame_DifferentNotComment(){
        RubricCriterionRating rating = new RubricCriterionRating();
        rating.setDescription("item");
        RubricRatingItem item = new RubricRatingItem(rating);

        RubricCriterionRating rating1 = new RubricCriterionRating();
        rating1.setDescription("item1");
        RubricRatingItem item1 = new RubricRatingItem(rating1);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(item, item1));
    }

    @Test
    public void testAreContentsTheSame_SameComment(){
        RubricCommentItem item = new RubricCommentItem("hodor", 0d);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(item, item));
    }
}
