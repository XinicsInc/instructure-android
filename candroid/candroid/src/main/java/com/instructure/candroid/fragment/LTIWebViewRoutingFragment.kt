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

package com.instructure.candroid.fragment

import android.os.Bundle
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Tab
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.Const

class LTIWebViewRoutingFragment : LTIWebViewFragment() {

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        return FragmentInteractions.Placement.DETAIL
    }

    companion object {
        fun createBundle(canvasContext: CanvasContext, ltiTab: Tab?): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putBoolean(Const.AUTHENTICATE, false)
            extras.putParcelable(Const.TAB, ltiTab)
            return extras
        }

        @JvmStatic
        fun createBundle(canvasContext: CanvasContext, url: String): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putBoolean(Const.AUTHENTICATE, false)
            extras.putString(LTIWebViewFragment.LTI_URL, url)
            return extras
        }

        @JvmStatic
        fun createBundle(canvasContext: CanvasContext, url: String, title: String, sessionLessLaunch: Boolean): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putBoolean(Const.AUTHENTICATE, false)
            extras.putString(LTIWebViewFragment.LTI_URL, url)
            extras.putBoolean(Const.SESSIONLESS_LAUNCH, sessionLessLaunch)
            extras.putString(Const.TITLE, title)
            return extras
        }
    }
}
