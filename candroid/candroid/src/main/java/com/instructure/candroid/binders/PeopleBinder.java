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

package com.instructure.candroid.binders;

import android.content.Context;
import android.view.View;

import com.instructure.candroid.holders.PeopleViewHolder;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.canvasapi2.models.User;
import com.instructure.pandautils.utils.ProfileUtils;

public class PeopleBinder extends BaseBinder {

    public static void bind(
            final User item,
            final Context context,
            final PeopleViewHolder holder,
            final AdapterToFragmentCallback<User> adapterToFragmentCallback,
            final int courseColor,
            final boolean isFirstItem,
            final boolean isLastItem) {

        ProfileUtils.loadAvatarForUser(holder.icon, item);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterToFragmentCallback.onRowClicked(item, holder.getAdapterPosition(), true);
            }
        });

        holder.icon.setBorderColor(courseColor);
        holder.title.setText(item.getName());


        int enrollmentIndex = item.getEnrollmentIndex();
        if(enrollmentIndex >= 0 && enrollmentIndex < item.getEnrollments().size()){
            holder.role.setText(item.getEnrollments().get(item.getEnrollmentIndex()).getType());
            setVisible(holder.role);
        } else {
            holder.role.setText("");
            setGone(holder.role);
        }

        updateShadows(isFirstItem, isLastItem, holder.shadowTop, holder.shadowBottom);
    }
}
