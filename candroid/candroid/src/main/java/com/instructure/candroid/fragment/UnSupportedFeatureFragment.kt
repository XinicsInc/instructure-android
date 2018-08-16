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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.instructure.candroid.R
import com.instructure.candroid.util.Analytics
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.setupAsBackButton
import kotlinx.android.synthetic.main.fragment_unsupported_feature.*

open class UnSupportedFeatureFragment : ParentFragment() {

    private var featureName: String? = null
    private var url: String? = null

    private var placement: FragmentInteractions.Placement = FragmentInteractions.Placement.MASTER

    override fun title(): String {
        return getString(R.string.unsupported)
    }

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        return placement
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_unsupported_feature, container, false)
    }

    override fun applyTheme() {
        toolbar.title = title()
        toolbar.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, canvasContext)
        initViews()
    }

    private fun initViews() {

        //Set the text
        if (featureName != null) {
            featureText.text = String.format(getString(R.string.isNotSupportedFeature), featureName)
        } else {
            featureText.text = getString(R.string.isNotSupported)
        }

        openInBrowser.setOnClickListener {
            if (featureName != null) {
                Analytics.trackUnsupportedFeature(activity, featureName)
            } else if (url != null) {
                Analytics.trackUnsupportedFeature(activity, url)
            }

            //the last parameter needs to be true so the webpage will try to authenticate
            InternalWebviewFragment.loadInternalWebView(activity, navigation,
                    InternalWebviewFragment.createBundle(canvasContext, url!!, true, true))
        }

        ViewStyler.themeButton(openInBrowser)
    }

    fun setFeature(featureName: String, url: String) {
        this.featureName = featureName
        this.url = url
        initViews()
    }

    override fun handleIntentExtras(extras: Bundle?) {
        super.handleIntentExtras(extras)
        extras?.let {
            featureName = it.getString(Const.FEATURE_NAME)
            url = it.getString(Const.URL)

            if (it.containsKey(Const.PLACEMENT)) {
                placement = it.getSerializable(Const.PLACEMENT) as FragmentInteractions.Placement
            }
        }
    }

    override fun allowBookmarking(): Boolean {
        return false
    }

    companion object {

        @JvmStatic
        fun createBundle(canvasContext: CanvasContext, title: String, url: String? = null): Bundle {
            return Bundle().apply {
                putParcelable(Const.CANVAS_CONTEXT, canvasContext)
                putString(Const.FEATURE_NAME, title)
                if(url != null) putString(Const.URL, url)
            }
        }
    }
}
