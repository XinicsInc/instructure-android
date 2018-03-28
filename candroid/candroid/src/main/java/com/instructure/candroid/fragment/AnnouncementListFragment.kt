/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
 */    package com.instructure.candroid.fragment

import com.instructure.candroid.R
import com.instructure.canvasapi2.utils.pageview.PageView

@PageView(url = "{canvasContext}/announcements")
class AnnouncementListFragment : DiscussionListFragment() {

    override val isAnnouncement: Boolean
        get() = true

    override fun title(): String {
        return getString(R.string.announcements)
    }
}
