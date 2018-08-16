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

package com.instructure.candroid.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.candroid.binders.BaseBinder;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Group;
import com.instructure.pandautils.utils.ColorKeeper;

import java.util.ArrayList;
import java.util.List;

public class CanvasContextSpinnerAdapter extends ArrayAdapter<CanvasContext> {

    public static final int COURSE_SEPARATOR = -22222;
    public static final int GROUP_SEPARATOR = -11111;

    private ArrayList<CanvasContext> mData;
    private LayoutInflater mInflater;

    public CanvasContextSpinnerAdapter(Context context, ArrayList<CanvasContext> data) {
        super(context, R.layout.canvas_context_spinner_adapter_item, data);
        mInflater = LayoutInflater.from(context);
        mData = data;
    }

    public CanvasContext getItem(int position) {
        return mData.get(position);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        boolean isEnabled = true;
        if(mData.get(position).getId() == GROUP_SEPARATOR || mData.get(position).getId() == COURSE_SEPARATOR) {
            isEnabled = false;
        }
        return isEnabled;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final CanvasContextViewHolder viewHolder;

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.canvas_context_spinner_adapter_item, parent, false);
            viewHolder = new CanvasContextViewHolder();
            viewHolder.title = (TextView)convertView.findViewById(R.id.title);
            viewHolder.indicator = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (CanvasContextViewHolder)convertView.getTag();
        }

        CanvasContext item = mData.get(position);
        if(item != null) {
            viewHolder.title.setText(item.getName());
            viewHolder.indicator.setVisibility(View.VISIBLE);
            viewHolder.indicator.setBackground(BaseBinder.createIndicatorBackground(
                    ColorKeeper.getOrGenerateColor(item)));
        } else {
            viewHolder.indicator.setVisibility(View.GONE);
            viewHolder.title.setText("");
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        final CanvasContextViewHolder viewHolder;

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.canvas_context_spinner_adapter_item, parent, false);
            viewHolder = new CanvasContextViewHolder();
            viewHolder.title = (TextView)convertView.findViewById(R.id.title);
            viewHolder.indicator = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (CanvasContextViewHolder)convertView.getTag();
        }

        CanvasContext item = mData.get(position);

        if(item != null) {
            viewHolder.title.setText(item.getName());

            if (item.getId() == GROUP_SEPARATOR || item.getId() == COURSE_SEPARATOR) {
                viewHolder.title.setTypeface(null, Typeface.BOLD);
                viewHolder.indicator.setVisibility(View.GONE);
            } else {
                viewHolder.title.setTypeface(null, Typeface.NORMAL);
                viewHolder.indicator.setVisibility(View.VISIBLE);
                viewHolder.indicator.setBackground(BaseBinder.createIndicatorBackground(
                        ColorKeeper.getOrGenerateColor(item)));
            }
        }

        return convertView;
    }

    private static class CanvasContextViewHolder {
        TextView title;
        ImageView indicator;
    }


    public static CanvasContextSpinnerAdapter newAdapterInstance(Context context, List<Course> courses, List<Group> groups) {

        ArrayList<CanvasContext> canvasContexts = new ArrayList<>();

        if(!courses.isEmpty()) {
            Course courseSeparator = new Course();
            courseSeparator.setName(context.getString(R.string.courses));
            courseSeparator.setId(COURSE_SEPARATOR);
            canvasContexts.add(courseSeparator);

            canvasContexts.addAll(courses);
        }
        if(!groups.isEmpty()) {
            Course groupSeparator = new Course();
            groupSeparator.setName(context.getString(R.string.groups));
            groupSeparator.setId(GROUP_SEPARATOR);
            canvasContexts.add(groupSeparator);

            canvasContexts.addAll(groups);
        }
        return new CanvasContextSpinnerAdapter(context, canvasContexts);
    }
}
