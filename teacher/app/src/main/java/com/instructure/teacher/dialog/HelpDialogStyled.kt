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

package com.instructure.teacher.dialog

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
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.canvasapi2.utils.Logger
import com.instructure.loginapi.login.api.zendesk.utilities.ZendeskDialogStyled
import com.instructure.pandautils.dialogs.RatingDialog
import com.instructure.pandautils.utils.Const
import com.instructure.teacher.R
import com.instructure.teacher.activities.InternalWebViewActivity
import kotlinx.android.synthetic.main.dialog_help.view.*
import java.util.*


class HelpDialogStyled : DialogFragment() {

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

    @SuppressLint("InflateParams") // Suppress lint warning about passing in null as the parent of a view to be inflated
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity).setTitle(activity.getString(R.string.help))
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_help, null)

        builder.setView(view)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(true)

        setupListeners(view)

        return dialog
    }

    override fun onDestroyView() {
        if (retainInstance)
            dialog?.setDismissMessage(null)

        super.onDestroyView()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers
    ///////////////////////////////////////////////////////////////////////////

    private fun setupListeners(layoutView: View) {
        with (layoutView) {
            searchGuides.setOnClickListener {
                // Search guides
                startActivity(InternalWebViewActivity.createIntent(activity, Const.CANVAS_USER_GUIDES, getString(R.string.canvasGuides), false))
            }

            reportProblem.setOnClickListener {
                val dialog = ZendeskDialogStyled()

                dialog.arguments = ZendeskDialogStyled.createBundle(false, true)
                dialog.show(activity.supportFragmentManager, ZendeskDialogStyled.TAG)
            }

            requestFeature.setOnClickListener {
                // Let the user open their favorite mail client
                val intent = populateMailIntent(activity.getString(R.string.featureSubject), activity.getString(R.string.understandRequest), false)

                startActivity(Intent.createChooser(intent, activity.getString(R.string.sendMail)))
            }

            shareLove.setOnClickListener { RatingDialog.showRateDialog(activity, com.instructure.pandautils.utils.AppType.TEACHER) }
        }
    }

    /* Pass in the subject and first line of the e-mail, all the other data is the same */
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
        val pInfo: PackageInfo
        var versionName = ""
        var versionCode = 0
        try {
            pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            versionName = pInfo.versionName
            versionCode = pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.d(e.message)
        }

        intent.putExtra(Intent.EXTRA_SUBJECT, "[" + subject + "] " + getString(R.string.issue_with_canvas) + versionName)

        val user = ApiPrefs.user

        // Populate the email body with information about the user
        var emailBody = ""
        emailBody += title + "\n"
        if (user != null) {
            emailBody += activity.getString(R.string.help_userId) + " " + user.id + "\n"
            emailBody += activity.getString(R.string.help_email) + " " + user.email + "\n"
        } else {
            emailBody += activity.getString(R.string.no_user) + "\n"
        }

        emailBody += activity.getString(R.string.help_domain) + " " + ApiPrefs.domain + "\n"
        emailBody += activity.getString(R.string.help_versionNum) + " " + versionName + " " + versionCode + "\n"
        emailBody += activity.getString(R.string.help_locale) + " " + Locale.getDefault() + "\n"
        emailBody += activity.getString(R.string.installDate) + " " + installDateString + "\n"
        emailBody += "----------------------------------------------\n"

        intent.putExtra(Intent.EXTRA_TEXT, emailBody)

        return intent
    }

    companion object {
        const val TAG = "helpDialog"

        fun show(activity: FragmentActivity): HelpDialogStyled =
                HelpDialogStyled().apply {
                    arguments = Bundle()
                    show(activity.supportFragmentManager, TAG)
                }
    }
}

