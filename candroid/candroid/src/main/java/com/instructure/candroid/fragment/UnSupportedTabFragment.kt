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

package com.instructure.candroid.fragment

import android.os.Bundle
import android.support.annotation.StringRes
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Tab
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ContextKeeper
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.setupAsBackButton

class UnSupportedTabFragment : InternalWebviewFragment() {

    override fun applyTheme() {
        super.applyTheme()
        toolbar?.setupAsBackButton {
            navigation?.popCurrentFragment()
        }
    }

    companion object {

        @JvmStatic
        fun createBundle(canvasContext: CanvasContext, tabId: String, @StringRes title: Int): Bundle {
            var url = ApiPrefs.fullDomain
            val featureTitle = ContextKeeper.appContext.getString(title)
            when {
                tabId.equals(Tab.CONFERENCES_ID, ignoreCase = true) -> {
                    url += canvasContext.toAPIString() + "/conferences"
                }
                tabId.equals(Tab.COLLABORATIONS_ID, ignoreCase = true) -> {
                    url += canvasContext.toAPIString() + "/collaborations"
                }
                tabId.equals(Tab.OUTCOMES_ID, ignoreCase = true) -> {
                    url += canvasContext.toAPIString() + "/outcomes"
                }
            }
            val bundle = InternalWebviewFragment.createBundle(canvasContext, url, featureTitle, true, true, false)
            bundle.putString(Const.TAB_ID, tabId)
            return bundle
        }
    }
}
