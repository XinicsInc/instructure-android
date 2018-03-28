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

package com.instructure.candroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.activity.InternalWebViewActivity
import com.instructure.candroid.util.Analytics
import com.instructure.candroid.util.LoggingUtility
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.loginapi.login.api.zendesk.utilities.ZendeskDialogStyled
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.help_dialog.view.*
import java.util.*

class HelpDialogStyled : DialogFragment() {
    var showAskInstructor: Boolean by BooleanArg()

    private val installDateString: String
        get() {
            return try {
                val installed = activity.packageManager
                        .getPackageInfo(activity.packageName, 0)
                        .firstInstallTime
                DateHelper.getDayMonthYearFormat().format(Date(installed))
            } catch (e: Exception) {
                ""
            }
        }

    @SuppressLint("InflateParams") // Suppress lint warning about null parent when inflating layout
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity).setTitle(activity.getString(R.string.help))
        val view = LayoutInflater.from(activity).inflate(R.layout.help_dialog, null)

        builder.setView(view)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)

        // Check if the user is only a teacher. if they only have teacher enrollments we don't want to show the askInstructor button
        view.askInstructor.setVisible(showAskInstructor)

        setupListeners(view)

        return dialog
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance)
            dialog.setDismissMessage(null)

        super.onDestroyView()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers
    ///////////////////////////////////////////////////////////////////////////

    private fun setupListeners(layoutView: View) {
        with (layoutView) {
            askInstructor.setOnClickListener {
                // Open the ask instructor dialog
                AskInstructorDialogStyled().show(fragmentManager, AskInstructorDialogStyled.TAG)

                // Log to GA
                Analytics.trackButtonPressed(activity, "[HelpDialog] AskInstructor", null)
            }

            searchGuides.setOnClickListener {
                // Search guides
                startActivity(InternalWebViewActivity.createIntent(activity, Const.CANVAS_USER_GUIDES, getString(R.string.canvasGuides), false))

                // Log to GA
                Analytics.trackButtonPressed(activity, "Search Guides", 0L)
            }

            reportProblem.setOnClickListener {
                val dialog = ZendeskDialogStyled()

                dialog.arguments = ZendeskDialogStyled.createBundle(false)
                dialog.show(activity.supportFragmentManager, ZendeskDialogStyled.TAG)

                // Log to GA
                Analytics.trackButtonPressed(activity, "Show Zendesk", 0L)
            }

            requestFeature.setOnClickListener {
                // Let the user open their favorite mail client
                val intent = populateMailIntent(activity.getString(R.string.featureSubject), activity.getString(R.string.understandRequest), false)

                startActivity(Intent.createChooser(intent, activity.getString(R.string.sendMail)))

                // Log to GA
                Analytics.trackButtonPressed(activity, "RequestFeature", null)
            }

            shareLove.setOnClickListener {
                Utils.goToAppStore(AppType.CANDROID, activity)
                // Log to GA
                Analytics.trackButtonPressed(activity, "Feedback", null)
            }
        }
    }

    /*
        Pass in the subject and first line of the e-mail, all the other data is the same
     */
    private fun populateMailIntent(subject: String, title: String, supportFlag: Boolean): Intent {
        // Let the user open their favorite mail client
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc822"

        if (supportFlag) {
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(activity.getString(R.string.utils_supportEmailAddress)))
        } else {
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(activity.getString(R.string.utils_mobileSupportEmailAddress)))
        }

        // Try to get the version number and version code
        var pInfo: PackageInfo?
        var versionName = ""
        var versionCode = 0
        try {
            pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            versionName = pInfo!!.versionName
            versionCode = pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            LoggingUtility.LogConsole(e.message)
        }

        intent.putExtra(Intent.EXTRA_SUBJECT, "[$subject] Issue with Canvas [Android] $versionName")

        val user = ApiPrefs.user
        // Populate the email body with information about the user
        var emailBody = ""
        emailBody += title + "\n"
        emailBody += activity.getString(R.string.help_userId) + " " + user!!.id + "\n"
        emailBody += activity.getString(R.string.help_email) + " " + user.email + "\n"
        emailBody += activity.getString(R.string.help_domain) + " " + ApiPrefs.domain + "\n"
        emailBody += activity.getString(R.string.help_versionNum) + " " + versionName + " " + versionCode + "\n"
        emailBody += activity.getString(R.string.help_locale) + " " + Locale.getDefault() + "\n"
        emailBody += activity.getString(R.string.installDate) + " " + installDateString + "\n"
        emailBody += "----------------------------------------------\n"

        intent.putExtra(Intent.EXTRA_TEXT, emailBody)

        return intent
    }

    companion object {
        const val TAG = "helpDialogStyled"
        private const val SHOW_ASK_INSTRUCTOR = "showAskInstructor"

        fun show(activity: FragmentActivity, showAskInstructor: Boolean): HelpDialogStyled =
                HelpDialogStyled().apply {
                    val args = Bundle()
                    args.putBoolean(SHOW_ASK_INSTRUCTOR, showAskInstructor)
                    arguments = args
                    show(activity.supportFragmentManager, TAG)
                }
    }
}
