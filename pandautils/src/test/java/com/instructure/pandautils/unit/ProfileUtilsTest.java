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

package com.instructure.pandautils.unit;

import com.instructure.canvasapi2.models.User;
import com.instructure.pandautils.utils.ProfileUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProfileUtilsTest {

    @Test
    public void getUserInitials_string1() throws Exception {
        User user = new User();
        user.setShortName("Billy Bob");

        String testValue = ProfileUtils.getUserInitials(user.getShortName());

        assertEquals("BB", testValue);
    }

    @Test
    public void getUserInitials_string2() throws Exception {
        User user = new User();
        user.setShortName("Billy Joel");

        String testValue = ProfileUtils.getUserInitials(user.getShortName());

        assertEquals("BJ", testValue);
    }

    @Test
    public void getUserInitials_string3() throws Exception {
        User user = new User();
        user.setShortName("Billy Bob Thorton");

        String testValue = ProfileUtils.getUserInitials(user.getShortName());

        assertEquals("B", testValue);
    }

    @Test
    public void getUserInitials_various() throws Exception {
        assertEquals("BB", ProfileUtils.getUserInitials("Billy Bob"));
        assertEquals("BB", ProfileUtils.getUserInitials("billy bob"));
        assertEquals("BB", ProfileUtils.getUserInitials("Billy    Bob"));
        assertEquals("BB", ProfileUtils.getUserInitials("  Billy Bob"));
        assertEquals("BB", ProfileUtils.getUserInitials("Billy Bob   "));

        assertEquals("B", ProfileUtils.getUserInitials("Billy Bob Joe"));
        assertEquals("B", ProfileUtils.getUserInitials("Billy"));
        assertEquals("B", ProfileUtils.getUserInitials("   Billy"));
        assertEquals("B", ProfileUtils.getUserInitials("Billy   "));
        assertEquals("B", ProfileUtils.getUserInitials("billy"));

        assertEquals("?", ProfileUtils.getUserInitials(""));
        assertEquals("?", ProfileUtils.getUserInitials(" "));
        assertEquals("?", ProfileUtils.getUserInitials("\t"));
        assertEquals("?", ProfileUtils.getUserInitials(null));
    }

}
