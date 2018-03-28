/*
 * Copyright (C) 2016 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.candroid.fragment;

import android.os.Bundle;

import com.instructure.candroid.R;
import com.instructure.candroid.util.StudentPrefs;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;

import java.util.Calendar;

//Small customization needed to set custom data for adapter and layout for gridview
public class CanvasCalendarFragment extends CaldroidFragment {

    @Override
    public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year){
        return new CanvasCalendarGridAdapter(getActivity(), month, year, getCaldroidData(), extraData);
    }

    @Override
    protected int getGridViewRes() {
        return R.layout.canvas_calendar_gridview;
    }

    public static Bundle createBundle(Calendar calendar, int month, int year){
        Bundle args = new Bundle();
        if(month != -1 && year != -1){
            args.putInt(CaldroidFragment.MONTH, month);
            args.putInt(CaldroidFragment.YEAR, year);
        } else {
            args.putInt(CaldroidFragment.MONTH, calendar.get(Calendar.MONTH) + 1);
            args.putInt(CaldroidFragment.YEAR, calendar.get(Calendar.YEAR));
        }
        boolean startDayMonday = StudentPrefs.getWeekStartsOnMonday();
        if (startDayMonday) {
            args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        } else {
            args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.SUNDAY);
        }
        args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, false);
        return args;
    }
}
