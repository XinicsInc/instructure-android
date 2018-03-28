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

import com.instructure.candroid.adapter.DiscussionListRecyclerAdapter;
import com.instructure.canvasapi2.models.DiscussionTopicHeader;
import com.instructure.canvasapi2.utils.APIHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class DiscussionListRecyclerAdapterTest extends InstrumentationTestCase {
    private DiscussionListRecyclerAdapter mAdapter;


    public static class DiscussionListRecyclerAdapterWrapper extends DiscussionListRecyclerAdapter {
        protected DiscussionListRecyclerAdapterWrapper(Context context) { super(context); }
    }

    @Before
    public void setup(){
        mAdapter = new DiscussionListRecyclerAdapterWrapper(RuntimeEnvironment.application);
    }

    @Test
    public void testAreContentsTheSame_SameTitle(){
        DiscussionTopicHeader discussionTopicHeader = new DiscussionTopicHeader();
        discussionTopicHeader.setTitle("discussion");
        assertTrue(mAdapter.createItemCallback().areContentsTheSame(discussionTopicHeader, discussionTopicHeader));
    }

    @Test
    public void testAreContentsTheSame_DifferentTitle(){
        DiscussionTopicHeader discussionTopicHeader1 = new DiscussionTopicHeader();
        discussionTopicHeader1.setTitle("discussion1");
        DiscussionTopicHeader discussionTopicHeader2 = new DiscussionTopicHeader();
        discussionTopicHeader2.setTitle("discussion2");
        assertFalse(mAdapter.createItemCallback().areContentsTheSame(discussionTopicHeader1, discussionTopicHeader2));
    }

    // region Compare tests

    @Test
    public void testCompare_bothHaveNullDates() {
        DiscussionTopicHeader onlyTitle1 = new DiscussionTopicHeader();
        onlyTitle1.setTitle("discussion1");
        DiscussionTopicHeader onlyTitle2 = new DiscussionTopicHeader();
        onlyTitle2.setTitle("discussion2");

        assertEquals(-1, mAdapter.createItemCallback().compare("", onlyTitle1, onlyTitle2));
        assertEquals(1, mAdapter.createItemCallback().compare("", onlyTitle2, onlyTitle1));
        assertEquals(0, mAdapter.createItemCallback().compare("", onlyTitle1, onlyTitle1));
    }

    @Test
    public void testCompare_oneNullDateLastReply() {
        DiscussionTopicHeader d1 = new DiscussionTopicHeader();
        d1.setTitle("discussion1");
        DateTime dateTime2 = new DateTime("2014-12-29");
        Date date = new Date(dateTime2.getMilliseconds(TimeZone.getDefault()));
        d1.setLastReplyAt(APIHelper.dateToString(date));

        DiscussionTopicHeader d2 = new DiscussionTopicHeader();
        d2.setTitle("discussion2");

        assertEquals(-1, mAdapter.createItemCallback().compare("", d1, d2));
        assertEquals(1, mAdapter.createItemCallback().compare("", d2, d1));
        assertEquals(0, mAdapter.createItemCallback().compare("", d1, d1));
    }

    @Test
    public void testCompare_oneNullDatePostedAt() {
        DiscussionTopicHeader d1 = new DiscussionTopicHeader();
        d1.setTitle("discussion1");
        DateTime dateTime2 = new DateTime("2014-12-29");
        Date date = new Date(dateTime2.getMilliseconds(TimeZone.getDefault()));
        d1.setPostedAt(APIHelper.dateToString(date));

        DiscussionTopicHeader d2 = new DiscussionTopicHeader();
        d2.setTitle("discussion2");

        assertEquals(-1, mAdapter.createItemCallback().compare("", d1, d2));
        assertEquals(1, mAdapter.createItemCallback().compare("", d2, d1));
        assertEquals(0, mAdapter.createItemCallback().compare("", d1, d1));
    }

    @Test
    public void testCompare_bothHaveDates() {
        DiscussionTopicHeader d1 = new DiscussionTopicHeader();
        d1.setTitle("discussion1");
        DateTime dateTime1 = new DateTime("2014-12-27");
        Date date1 = new Date(dateTime1.getMilliseconds(TimeZone.getDefault()));
        d1.setLastReplyAt(APIHelper.dateToString(date1));
        DiscussionTopicHeader d2 = new DiscussionTopicHeader();
        DateTime dateTime2 = new DateTime("2014-12-29");
        Date date2 = new Date(dateTime2.getMilliseconds(TimeZone.getDefault()));
        d2.setLastReplyAt(APIHelper.dateToString(date2));
        d2.setTitle("discussion2");

        // callback sorts most recent date first
        assertEquals(1, mAdapter.createItemCallback().compare("", d1, d2));
        assertEquals(-1, mAdapter.createItemCallback().compare("", d2, d1));
        assertEquals(0, mAdapter.createItemCallback().compare("", d1, d1));
    }
    // endregion
}
