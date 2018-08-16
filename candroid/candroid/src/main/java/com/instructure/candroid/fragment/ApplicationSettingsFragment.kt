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

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.instructure.candroid.BuildConfig
import com.instructure.candroid.R
import com.instructure.candroid.activity.NotificationPreferencesActivity
import com.instructure.candroid.activity.SettingsActivity
import com.instructure.candroid.dialog.HelpDialogStyled
import com.instructure.candroid.dialog.LegalDialogStyled
import com.instructure.candroid.util.Analytics
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.onClick
import com.instructure.pandautils.utils.setGone
import com.instructure.pandautils.utils.setupAsBackButton
import kotlinx.android.synthetic.main.fragment_application_settings.*
import kotlinx.android.synthetic.main.dialog_about.*

@PageView(url = "profile/settings")
class ApplicationSettingsFragment : ParentFragment() {

    override fun allowBookmarking() = false

    override fun getFragmentPlacement() = FragmentInteractions.Placement.DETAIL

    override fun title(): String = getString(R.string.settings)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_application_settings, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyTheme()
        setupViews()
    }

    override fun applyTheme() {
        toolbar.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK, false)
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        profileSettings.onClick { addFragment(ProfileSettingsFragment()) }
        accountPreferences.onClick { addFragment(AccountPreferencesFragment()) }
        legal.onClick { LegalDialogStyled().show(fragmentManager, LegalDialogStyled.TAG) }
        help.onClick { HelpDialogStyled.show(activity, true) }
        pinAndFingerprint.setGone() // TODO: Wire up once implemented

        pushNotifications.onClick {
            Analytics.trackAppFlow(activity, NotificationPreferencesActivity::class.java)
            startActivity(Intent(activity, NotificationPreferencesActivity::class.java))
        }

        about.onClick {
            AlertDialog.Builder(context)
                    .setTitle(R.string.about)
                    .setView(R.layout.dialog_about)
                    .show()
                    .apply {
                        domain.text = ApiPrefs.domain
                        loginId.text = ApiPrefs.user!!.loginId
                        email.text = ApiPrefs.user!!.email ?: ApiPrefs.user!!.primaryEmail
                        version.text = "${getString(R.string.canvasVersionNum)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    }
        }
    }

    private fun addFragment(fragment: Fragment) {
        (activity as? SettingsActivity)?.addFragment(fragment)
    }
}
