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
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.widget.ImageView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.instructure.candroid.R
import com.instructure.candroid.activity.InternalWebViewActivity
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.TermsOfService
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.descendants
import com.instructure.pandautils.utils.onClick
import com.instructure.pandautils.utils.setVisible
import kotlinx.android.synthetic.main.legal.view.*
import kotlinx.coroutines.experimental.Job

class LegalDialogStyled : AppCompatDialogFragment() {

    private var termsJob: Job? = null
    private var html: String = ""

    init {
        retainInstance = true
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(activity).inflate(R.layout.legal, null)

        view.descendants<ImageView>().forEach {
            it.setColorFilter(ThemePrefs.brandColor)
        }

        // different institutions can have different terms of service, we need to get them from the api
        termsJob = tryWeave {

            val terms = awaitApi<TermsOfService> { UserManager.getTermsOfService(it,true) }
            terms.content?.let { html = it }


            // if the institution has set terms and conditions to be "no terms", just keep the item gone
            view.termsOfUse.setVisible(html.isNotBlank())
            // now set the rest of the items visible
            view.eula.setVisible()
            view.privacyPolicy.setVisible()
            view.openSource.setVisible()
        } catch {
            // something went wrong, make everything visible
            view.descendants.forEach { it.setVisible()}
        }

        view.termsOfUse.onClick {

            val intent = InternalWebViewActivity.createIntent(activity, "http://www.canvaslms.com/policies/terms-of-use", html, activity.getString(R.string.termsOfUse), false)
            activity.startActivity(intent)
            dialog.dismiss()
        }

        view.eula.onClick {
            val intent = InternalWebViewActivity.createIntent(activity, "http://www.canvaslms.com/policies/end-user-license-agreement", activity.getString(R.string.EULA), false)
            activity.startActivity(intent)
            dialog.dismiss()
        }

        view.privacyPolicy.onClick {
            val intent = InternalWebViewActivity.createIntent(activity, "https://www.canvaslms.com/policies/privacy", activity.getString(R.string.privacyPolicy), false)
            activity.startActivity(intent)
            dialog.dismiss()
        }

        view.openSource.onClick {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
            dialog.dismiss()
        }

        return AlertDialog.Builder(context)
                .setTitle(R.string.legal)
                .setView(view)
                .create()
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) dialog.setDismissMessage(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        termsJob?.cancel()
    }

    companion object {
        val TAG = "legalDialog"
    }

}
