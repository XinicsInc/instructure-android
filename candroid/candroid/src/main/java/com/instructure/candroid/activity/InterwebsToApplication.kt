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
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window

import com.instructure.candroid.R
import com.instructure.candroid.util.Analytics
import com.instructure.candroid.util.LoggingUtility
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.Const
import kotlinx.android.synthetic.main.loading_canvas_view.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay

class InterwebsToApplication : AppCompatActivity() {

    private var loadingJob: Job? = null
    private var host = ""

    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.interwebs_to_application)
        loadingRoute.visibility = View.VISIBLE

        val url = intent.dataString

        if (TextUtils.isEmpty(url)) {
            finish()
            return
        }

        val data = Uri.parse(url)

        if (data == null) {
            finish()
            return
        }

        loadRoute(data, url)
    }

    private fun loadRoute(data: Uri, url: String) {
        loadingJob = tryWeave {
            host = data.host // example: "mobiledev.instructure.com"

            //Log to GA.
            Analytics.trackAppFlow(this@InterwebsToApplication)

            //Do some logging
            LoggingUtility.Log(this@InterwebsToApplication, Log.WARN, data.toString())

            val token = ApiPrefs.token

            val signedIn = token.isNotEmpty()
            val domain = ApiPrefs.domain
            if (!signedIn) {
                delay(700)
                val intent = LoginActivity.createIntent(this@InterwebsToApplication)
                startActivity(intent)
                finish()
                return@tryWeave
            }

            if (signedIn && !domain.contains(host)) {
                delay(700)
                val intent = Intent(this@InterwebsToApplication, NavigationActivity.startActivityClass)
                intent.putExtra(Const.MESSAGE, getString(R.string.differentDomainFromLink))
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                return@tryWeave
            } else {
                //Allow the UI to show.
                delay(700)
                RouterUtils.routeUrl(this@InterwebsToApplication, url, false)
                finish()
            }
        } catch {
            finish()
        }
    }

    override fun finish() {
        overridePendingTransition(0, 0)
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingJob?.cancel()
    }

    companion object {

        fun createIntent(context: Context, uri: Uri): Intent {
            val intent = Intent(context, InterwebsToApplication::class.java)
            intent.data = uri
            return intent
        }
    }
}
