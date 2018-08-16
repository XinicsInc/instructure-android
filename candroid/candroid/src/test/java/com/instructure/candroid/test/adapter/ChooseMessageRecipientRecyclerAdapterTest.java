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

import com.instructure.candroid.adapter.InboxRecipientAdapter;
import com.instructure.canvasapi2.models.Recipient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class ChooseMessageRecipientRecyclerAdapterTest extends InstrumentationTestCase {
    private InboxRecipientAdapter mAdapter;

    public static class ChooseMessageRecipientRecyclerAdapterWrapper extends InboxRecipientAdapter {
        protected ChooseMessageRecipientRecyclerAdapterWrapper(Context context) { super(context, "", null, null, false ); }
    }

    @Before
    public void setup(){
        mAdapter = new ChooseMessageRecipientRecyclerAdapterWrapper(RuntimeEnvironment.application);
    }

    @Test
    public void testAreContentsTheSame_SameName(){
        Recipient recipient = new Recipient("", "name", 0, 0, 0);
        assertTrue(mAdapter.getItemCallback().areContentsTheSame(recipient, recipient));
    }

    @Test
    public void testAreContentsTheSame_DifferentName(){
        Recipient recipient1 = new Recipient("", "name", 0, 0, 0);
        Recipient recipient2 = new Recipient("", "hodor", 0, 0, 0);
        assertFalse(mAdapter.getItemCallback().areContentsTheSame(recipient1, recipient2));
    }
}
