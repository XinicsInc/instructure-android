/*
 * Copyright (C) 2017 - present Instructure, Inc.
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

package com.instructure.candroid.util

import android.os.Bundle
import com.instructure.candroid.fragment.*

import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.interactions.FragmentInteractions

object FragUtils {

    @JvmStatic
    fun <Type : FragmentInteractions> getFragment(cls: Class<Type>, bundle: Bundle): ParentFragment? {
        return ParentFragment.createParentFragment(cls, bundle)
    }

    @JvmStatic
    fun <Type : ParentFragment> getFrag(cls: Class<Type>, canvasContext: CanvasContext): Type {

        var bundle: Bundle? = null

        when {
            cls.isAssignableFrom(DashboardFragment::class.java) -> bundle = ParentFragment.createBundle(canvasContext)
            cls.isAssignableFrom(NotificationListFragment::class.java) -> bundle = NotificationListFragment.createBundle(canvasContext)
            cls.isAssignableFrom(ToDoListFragment::class.java) -> bundle = ToDoListFragment.createBundle(canvasContext)
            cls.isAssignableFrom(InboxFragment::class.java) -> bundle = InboxFragment.createBundle(canvasContext)
            cls.isAssignableFrom(CalendarListViewFragment::class.java) -> bundle = CalendarListViewFragment.createBundle(canvasContext)
            cls.isAssignableFrom(InternalWebviewFragment::class.java) -> bundle = InternalWebviewFragment.createDefaultBundle(canvasContext)
        }

        return ParentFragment.createFragment(cls, bundle)
    }

    @JvmStatic
    fun <Type : ParentFragment> getFrag(cls: Class<Type>, bundle: Bundle): Type {
        return ParentFragment.createFragment(cls, bundle)
    }

    @JvmStatic
    fun <Type : ParentFragment> getFrag(cls: Class<Type>): Type {
        val user = ApiPrefs.user
        return if(user != null) getFrag(cls, CanvasContext.currentUserContext(user))
        else getFrag(cls, CanvasContext.emptyUserContext())
    }
}
