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
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.widget.ImageView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.TermsOfService
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.descendants
import com.instructure.pandautils.utils.setVisible
import com.instructure.teacher.R
import com.instructure.teacher.activities.InternalWebViewActivity
import kotlinx.android.synthetic.main.dialog_legal.view.*
import kotlinx.coroutines.experimental.Job

class LegalDialog : AppCompatDialogFragment() {

    private var termsJob: Job? = null
    private var html: String = ""

    init {
        retainInstance = true
    }

    @SuppressLint("InflateParams") // Suppress lint warning about passing null during view inflation
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity).setTitle(activity.getString(R.string.legal))

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_legal, null)

        view.descendants<ImageView>().forEach {
            it.setColorFilter(ThemePrefs.brandColor)
        }

        // Different institutions can have different terms of service, we need to get them from the api
        termsJob = tryWeave {
            val terms = awaitApi<TermsOfService> { UserManager.getTermsOfService(it, true) }
            terms.content?.let { html = it }

            // If the institution has set terms and conditions to be "no terms", just keep the item gone
            view.termsOfUse.setVisible(html.isNotBlank())
            // Now set the rest of the items visible
            view.eula.setVisible()
            view.privacyPolicy.setVisible()
            view.openSource.setVisible()
        } catch {
            // Something went wrong, make everything visible
            view.descendants.forEach { it.setVisible() }
        }

        builder.setView(view)

        val dialog = builder.create()

        view.termsOfUse.setOnClickListener {
            val intent = InternalWebViewActivity.createIntent(activity, "http://www.canvaslms.com/policies/terms-of-use", html, activity.getString(R.string.termsOfUse), false)
            activity.startActivity(intent)
            dialog.dismiss()
        }

        view.eula.setOnClickListener {
            val intent = InternalWebViewActivity.createIntent(activity, "http://www.canvaslms.com/policies/end-user-license-agreement", activity.getString(R.string.EULA), false)
            activity.startActivity(intent)
            dialog.dismiss()
        }

        view.privacyPolicy.setOnClickListener {
            val intent = InternalWebViewActivity.createIntent(activity, "https://www.canvaslms.com/policies/privacy", activity.getString(R.string.privacyPolicy), false)
            activity.startActivity(intent)
            dialog.dismiss()
        }

        view.openSource.setOnClickListener {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            dialog.dismiss()
        }

        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onDestroyView() {
        if (retainInstance)
            dialog?.setDismissMessage(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        termsJob?.cancel()
    }

    companion object {
        const val TAG = "legalDialog"
    }
}

