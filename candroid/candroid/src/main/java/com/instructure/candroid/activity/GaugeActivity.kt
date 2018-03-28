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
package com.instructure.candroid.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.instructure.candroid.R
import com.instructure.canvasapi2.models.LaunchDefinition
import android.graphics.Color
import android.widget.Toast
import com.instructure.candroid.fragment.LTIWebViewFragment
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.Logger
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.setupAsBackButton
import kotlinx.android.synthetic.main.activity_gauge.*

class GaugeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gauge)

        //Keeping the toolbar here because we can get into a state where Gauge says we can go back but we cannot
        //this keeps the user on this screen with no way out. Trapped like a beaver in  a glove box.
        toolbar.setupAsBackButton { finish() }
        ViewStyler.themeToolbar(this, toolbar, Color.WHITE, Color.BLACK, false)

        val launchDefinition = intent.extras?.getParcelable<LaunchDefinition>(LAUNCH_DEFINITION)
        val user = ApiPrefs.user
        if(launchDefinition != null && user != null) {
            val args = LTIWebViewFragment.createBundle(
                    CanvasContext.currentUserContext(user),
                    launchDefinition.placements.globalNavigation.url,
                    getString(R.string.gauge), true, true)

            supportFragmentManager.beginTransaction().add(R.id.container, LTIWebViewFragment.newInstance(args), LTIWebViewFragment::class.java.simpleName).commit()
        } else {
            Toast.makeText(this, R.string.gaugeLaunchFailure, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.container)
        if(fragment is LTIWebViewFragment && fragment.canGoBack()) {
            //This prevents users from going back to the launch url for LITs, that url shows an error message and is
            //only used to forward the user to the actual LTI.
            val webBackForwardList = fragment.getCanvasWebView()?.copyBackForwardList()
            val historyUrl = webBackForwardList?.getItemAtIndex(webBackForwardList.currentIndex - 1)?.url
            if(historyUrl != null && historyUrl.contains("external_tools/sessionless_launch")) {
                finish()
            }
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private val LAUNCH_DEFINITION = "launchDefinition"

        fun createIntent(context: Context, launchDefinition: LaunchDefinition): Intent {
            val intent =  Intent(context, GaugeActivity::class.java)
            val extras = Bundle()
            extras.putParcelable(LAUNCH_DEFINITION, launchDefinition)
            intent.putExtras(extras)
            return intent
        }
    }
}