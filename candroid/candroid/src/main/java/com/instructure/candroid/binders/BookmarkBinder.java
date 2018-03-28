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

import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.ColorUtils;
import com.instructure.pandautils.views.RippleView;
import com.instructure.candroid.holders.BookmarkViewHolder;
import com.instructure.candroid.interfaces.BookmarkAdapterToFragmentCallback;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.canvasapi2.models.Bookmark;

@Deprecated
public class BookmarkBinder {

    public static void bind(
            final Context context,
            final boolean isShortcutActivity,
            final BookmarkViewHolder holder,
            final Bookmark bookmark,
            final BookmarkAdapterToFragmentCallback<Bookmark> adapterToFragmentCallback) {

        long courseId = bookmark.getCourseId();

        if(courseId == 0) {
            courseId = RouterUtils.getCourseIdFromURL(bookmark.getUrl());
            bookmark.setCourseId(courseId);
        }

        holder.title.setText(bookmark.getName());
        final int color = ColorKeeper.getOrGenerateColor(RouterUtils.getContextIdFromURL(bookmark.getUrl()));
        holder.icon.setImageDrawable(ColorUtils.colorIt(color, holder.icon.getDrawable()));
        holder.overflowRipple.setVisibility(isShortcutActivity ? View.INVISIBLE : View.VISIBLE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapterToFragmentCallback.onRowClicked(bookmark, holder.getAdapterPosition(), false);
            }
        });

        holder.overflowRipple.setOnRippleCompleteListener(new RippleView.OnRippleCompleteListener() {
            @Override
            public void onComplete(RippleView rippleView) {
                adapterToFragmentCallback.onOverflowClicked(bookmark, holder.getAdapterPosition(), rippleView);
            }
        });
    }
}
