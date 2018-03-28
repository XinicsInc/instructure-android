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

import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;


public class APIHelperTest {
    @Test
    public void parseLinkHeaderResponse() throws Exception {
        Headers headers = new Headers.Builder().add("link: <https://mobiledev.instructure.com/api/v1/courses/123456/discussion_topics.json?page2>; rel=\"next\"").build();

        LinkHeaders linkHeaders = APIHelper.parseLinkHeaderResponse(headers);

        assertEquals(linkHeaders.nextUrl, "courses/123456/discussion_topics.json?page2");
    }

    @Test
    public void removeDomainFromUrl() throws Exception {
        String url = "https://mobiledev.instructure.com/api/v1/courses/833052/external_tools?include_parents=true";
        String urlNoDomain = "courses/833052/external_tools?include_parents=true";
        assertEquals(urlNoDomain, APIHelper.removeDomainFromUrl(url));
    }

    @Test
    public void isCachedResponse() throws Exception {
        Response cacheResponse = new okhttp3.Response.Builder() //
                .code(200)
                .message("")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .build();

        Response response = new okhttp3.Response.Builder() //
                .code(200)
                .message("")
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .cacheResponse(cacheResponse)
                .build();

        assertEquals(true, APIHelper.isCachedResponse(response));

    }

    @Test
    public void paramIsNull_multiple() throws Exception {
        assertEquals(true, APIHelper.paramIsNull("", null));
    }

    @Test
    public void paramIsNull_noNulls() throws Exception {
        assertEquals(false, APIHelper.paramIsNull("", 17, new ArrayList<>()));
    }

    @Test
    public void stringToDate() throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);
        Calendar calendar = Calendar.getInstance();
        //clear out milliseconds because we're not displaying that in the simple date format
        calendar.set(Calendar.MILLISECOND, 0);

        Date date = calendar.getTime();
        String nowAsString = df.format(date);

        //add a 'Z' at the end. Dates have a Z at the end of the string ("2037-07-28T19:38:31Z")
        //so we parse that out in the function
        assertEquals(date, APIHelper.stringToDate(nowAsString + "Z"));
    }

    @Test
    public void dateToString_date() throws Exception {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-6"));

        calendar.set(Calendar.YEAR, 2037);
        calendar.set(Calendar.MONTH, 6);
        calendar.set(Calendar.DAY_OF_MONTH, 28);
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 38);
        calendar.set(Calendar.SECOND, 31);
        calendar.set(Calendar.MILLISECOND, 0);

        String dateString = "2037-07-28T19:38:31-06:00";
        Date date = calendar.getTime();
        assertEquals(dateString, APIHelper.dateToString(date));
    }

    @Test
    public void dateToString_dateNull() throws Exception {
        assertEquals(null, APIHelper.dateToString((Date) null));
    }

    @Test
    public void dateToString_gregorianCalendar() throws Exception {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT-6"));
        calendar.set(Calendar.YEAR, 2027);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 28);
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 33);
        calendar.set(Calendar.SECOND, 31);
        calendar.set(Calendar.MILLISECOND, 0);

        String dateString = "2027-09-28T19:33:31-06:00";

        assertEquals(dateString, APIHelper.dateToString(calendar));
    }

    @Test
    public void dateToString_gregorianCalendarNull() throws Exception {
        assertEquals(null, APIHelper.dateToString((GregorianCalendar) null));
    }

    @Test
    public void booleanToInt_true() throws Exception {
        assertEquals(1, APIHelper.booleanToInt(true));
    }

    @Test
    public void booleanToInt_false() throws Exception {
        assertEquals(0, APIHelper.booleanToInt(false));
    }

    @Test
    public void simplifyHTML() throws Exception {
        StringBuilder builder = new StringBuilder();
        String sampleText = "Here is some sample text";
        builder.append(sampleText);
        builder.append((char) 65532);
        builder.append((char) 32);

        assertEquals(sampleText, APIHelper.simplifyHTML(builder.toString()));
    }

    @Test
    public void paramsWithDomain() throws Exception {
        String domain = "www.domain.com";
        RestParams params = new RestParams.Builder()
                .withShouldIgnoreToken(false)
                .withPerPageQueryParam(false)
                .withAPIVersion("")
                .build();

        assertNotNull(APIHelper.paramsWithDomain(domain, params));
    }

}
