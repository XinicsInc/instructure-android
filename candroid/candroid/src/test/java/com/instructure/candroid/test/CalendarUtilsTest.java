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

package com.instructure.candroid.test;

import com.instructure.candroid.model.DateWindow;
import com.instructure.candroid.util.CanvasCalendarUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class CalendarUtilsTest extends Assert {

    @Test
    public void testWeekWindow(){
        boolean startDayMonday;
        //Test cases for start day Sunday
        startDayMonday = false;

        //Sunday
        DateTime dateTime1 = new DateTime("2014-12-28");
        Date date1 = new Date(dateTime1.getMilliseconds(TimeZone.getDefault()));
        //Sunday
        DateTime startTime = new DateTime("2014-12-28");
        //Saturday
        DateTime endTime = new DateTime("2015-01-03");

        DateWindow dateWindow1 = CanvasCalendarUtils.setSelectedWeekWindow(date1, startDayMonday);
        DateTime endTimeResult1 = DateTime.forInstant(dateWindow1.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult1 = DateTime.forInstant(dateWindow1.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult1.getEndOfDay().compareTo(startTime.getEndOfDay()) == 0);
        assertTrue(endTimeResult1.getEndOfDay().compareTo(endTime.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date1, dateWindow1.getStart(), dateWindow1.getStart()));


        //Monday
        DateTime dateTime2 = new DateTime("2014-12-29");
        Date date2 = new Date(dateTime2.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow2 = CanvasCalendarUtils.setSelectedWeekWindow(date2, startDayMonday);
        DateTime endTimeResult2 = DateTime.forInstant(dateWindow2.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult2 = DateTime.forInstant(dateWindow2.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult2.getEndOfDay().compareTo(startTime.getEndOfDay()) == 0);
        assertTrue(endTimeResult2.getEndOfDay().compareTo(endTime.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date2, dateWindow2.getStart(), dateWindow2.getEnd()));

        //Tuesday
        DateTime dateTime3 = new DateTime("2014-12-30");
        Date date3 = new Date(dateTime3.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow3 = CanvasCalendarUtils.setSelectedWeekWindow(date3, startDayMonday);
        DateTime endTimeResult3 = DateTime.forInstant(dateWindow3.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult3 = DateTime.forInstant(dateWindow3.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult3.getEndOfDay().compareTo(startTime.getEndOfDay()) == 0);
        assertTrue(endTimeResult3.getEndOfDay().compareTo(endTime.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date3, dateWindow3.getStart(), dateWindow3.getEnd()));

        //Wednesday
        DateTime dateTime4 = new DateTime("2014-12-31");
        Date date4 = new Date(dateTime4.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow4 = CanvasCalendarUtils.setSelectedWeekWindow(date4, startDayMonday);
        DateTime endTimeResult4 = DateTime.forInstant(dateWindow4.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult4 = DateTime.forInstant(dateWindow4.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult4.getEndOfDay().compareTo(startTime.getEndOfDay()) == 0);
        assertTrue(endTimeResult4.getEndOfDay().compareTo(endTime.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date4, dateWindow4.getStart(), dateWindow4.getEnd()));

        //Thursday
        DateTime dateTime5 = new DateTime("2015-01-01");
        Date date5 = new Date(dateTime5.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow5 = CanvasCalendarUtils.setSelectedWeekWindow(date5, startDayMonday);
        DateTime endTimeResult5 = DateTime.forInstant(dateWindow5.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult5 = DateTime.forInstant(dateWindow5.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult5.getEndOfDay().compareTo(startTime.getEndOfDay()) == 0);
        assertTrue(endTimeResult5.getEndOfDay().compareTo(endTime.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date5, dateWindow5.getStart(), dateWindow5.getEnd()));

        //Friday
        DateTime dateTime6 = new DateTime("2015-01-02");
        Date date6 = new Date(dateTime6.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow6 = CanvasCalendarUtils.setSelectedWeekWindow(date6, startDayMonday);
        DateTime endTimeResult6 = DateTime.forInstant(dateWindow6.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult6 = DateTime.forInstant(dateWindow6.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult6.getEndOfDay().compareTo(startTime.getEndOfDay()) == 0);
        assertTrue(endTimeResult6.getEndOfDay().compareTo(endTime.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date6, dateWindow6.getStart(), dateWindow6.getEnd()));

        //saturday
        DateTime dateTime7 = new DateTime("2015-01-02");
        Date date7 = new Date(dateTime7.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow7 = CanvasCalendarUtils.setSelectedWeekWindow(date4, startDayMonday);
        DateTime endTimeResult7 = DateTime.forInstant(dateWindow7.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult7 = DateTime.forInstant(dateWindow7.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult7.getEndOfDay().compareTo(startTime.getEndOfDay()) == 0);
        assertTrue(endTimeResult7.getEndOfDay().compareTo(endTime.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date7, dateWindow7.getStart(), dateWindow7.getEnd()));

        /////////////////////////////////////
        // Test cases for start day Monday //
        /////////////////////////////////////
        startDayMonday = true;

        //Monday
        DateTime dateTime1m = new DateTime("2014-12-29");
        Date date1m = new Date(dateTime1m.getMilliseconds(TimeZone.getDefault()));
        //Monday
        DateTime startTimeM = new DateTime("2014-12-29");
        //Sunday
        DateTime endTimeM = new DateTime("2015-01-04");

        DateWindow dateWindow1m = CanvasCalendarUtils.setSelectedWeekWindow(date1m, startDayMonday);
        DateTime endTimeResult1m = DateTime.forInstant(dateWindow1m.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult1m = DateTime.forInstant(dateWindow1m.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult1m.getEndOfDay().compareTo(startTimeM.getEndOfDay()) == 0);
        assertTrue(endTimeResult1m.getEndOfDay().compareTo(endTimeM.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date1m, dateWindow1.getStart(), dateWindow1m.getEnd()));


        //Tuesday
        DateTime dateTime2m = new DateTime("2014-12-30");
        Date date2m = new Date(dateTime2.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow2m = CanvasCalendarUtils.setSelectedWeekWindow(date2m, startDayMonday);
        DateTime endTimeResult2m = DateTime.forInstant(dateWindow2m.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult2m = DateTime.forInstant(dateWindow2m.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult2m.getEndOfDay().compareTo(startTimeM.getEndOfDay()) == 0);
        assertTrue(endTimeResult2m.getEndOfDay().compareTo(endTimeM.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date2m, dateWindow2m.getStart(), dateWindow2m.getEnd()));

        //Wednesday
        DateTime dateTime3m = new DateTime("2014-12-31");
        Date date3m = new Date(dateTime3m.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow3m = CanvasCalendarUtils.setSelectedWeekWindow(date3m, startDayMonday);
        DateTime endTimeResult3m = DateTime.forInstant(dateWindow3m.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult3m = DateTime.forInstant(dateWindow3m.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult3m.getEndOfDay().compareTo(startTimeM.getEndOfDay()) == 0);
        assertTrue(endTimeResult3m.getEndOfDay().compareTo(endTimeM.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date3m, dateWindow3m.getStart(), dateWindow3m.getEnd()));

        //Thursday
        DateTime dateTime4m = new DateTime("2015-01-01");
        Date date4m = new Date(dateTime4m.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow4m = CanvasCalendarUtils.setSelectedWeekWindow(date4m, startDayMonday);
        DateTime endTimeResult4m = DateTime.forInstant(dateWindow4m.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult4m = DateTime.forInstant(dateWindow4m.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult4m.getEndOfDay().compareTo(startTimeM.getEndOfDay()) == 0);
        assertTrue(endTimeResult4m.getEndOfDay().compareTo(endTimeM.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date4m, dateWindow4m.getStart(), dateWindow4m.getEnd()));

        //Friday
        DateTime dateTime5m = new DateTime("2015-01-02");
        Date date5m = new Date(dateTime5m.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow5m = CanvasCalendarUtils.setSelectedWeekWindow(date5, startDayMonday);
        DateTime endTimeResult5m = DateTime.forInstant(dateWindow5m.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult5m = DateTime.forInstant(dateWindow5m.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult5m.getEndOfDay().compareTo(startTimeM.getEndOfDay()) == 0);
        assertTrue(endTimeResult5m.getEndOfDay().compareTo(endTimeM.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date5m, dateWindow5m.getStart(), dateWindow5m.getEnd()));

        //Saturday
        DateTime dateTime6m = new DateTime("2015-01-03");
        Date date6m = new Date(dateTime6m.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow6m = CanvasCalendarUtils.setSelectedWeekWindow(date6m, startDayMonday);
        DateTime endTimeResult6m = DateTime.forInstant(dateWindow6m.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult6m = DateTime.forInstant(dateWindow6m.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult6m.getEndOfDay().compareTo(startTimeM.getEndOfDay()) == 0);
        assertTrue(endTimeResult6m.getEndOfDay().compareTo(endTimeM.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date6m, dateWindow6m.getStart(), dateWindow6m.getEnd()));

        //Sunday
        DateTime dateTime7m = new DateTime("2015-01-04");
        Date date7m = new Date(dateTime7m.getMilliseconds(TimeZone.getDefault()));

        DateWindow dateWindow7m = CanvasCalendarUtils.setSelectedWeekWindow(date7m, startDayMonday);
        DateTime endTimeResult7m = DateTime.forInstant(dateWindow7m.getEnd().getTime(), TimeZone.getDefault());
        DateTime startTimeResult7m = DateTime.forInstant(dateWindow7m.getStart().getTime(), TimeZone.getDefault());

        assertTrue(startTimeResult7m.getEndOfDay().compareTo(startTimeM.getEndOfDay()) == 0);
        assertTrue(endTimeResult7m.getEndOfDay().compareTo(endTimeM.getEndOfDay()) == 0);
        assertTrue(CanvasCalendarUtils.isWithinWeekWindow(date7m, dateWindow7m.getStart(), dateWindow7m.getEnd()));

    }

}
