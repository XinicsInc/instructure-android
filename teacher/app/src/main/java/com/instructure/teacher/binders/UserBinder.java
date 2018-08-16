/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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
 */

package com.instructure.teacher.binders;

import android.content.Context;
import android.view.View;

import com.instructure.canvasapi2.models.BasicUser;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.User;
import com.instructure.teacher.holders.UserViewHolder;
import com.instructure.teacher.interfaces.AdapterToFragmentCallback;
import com.instructure.pandautils.utils.ProfileUtils;

import java.util.HashSet;


public class UserBinder {

    public static void bind(final Context context, final User user, final AdapterToFragmentCallback<User> adapterToFragmentCallback, final UserViewHolder holder, final int position) {
        // Set student avatar
        BasicUser basicUser = new BasicUser();
        basicUser.setName(user.getName());
        basicUser.setAvatarUrl(user.getAvatarUrl());
        ProfileUtils.loadAvatarForUser(holder.studentAvatar, basicUser);

        // Set student name
        holder.userName.setText(user.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterToFragmentCallback.onRowClicked(user, position);
            }
        });

        //List enrollmentApiModel type(s)
        //get a list of strings of the enrollments
        // Use hashSet to prevent duplicate enrollments
        HashSet<String> enrollments = new HashSet<>();
        for(Enrollment enrollment : user.getEnrollments()) {
            enrollments.add(enrollment.getType());
        }

        holder.userRole.setText(android.text.TextUtils.join(", ", enrollments));

    }
}
