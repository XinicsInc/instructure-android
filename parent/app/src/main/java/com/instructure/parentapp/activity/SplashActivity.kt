/*
 * Copyright (C) 2016 - present  Instructure, Inc.
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

package com.instructure.parentapp.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.apis.AlertAPI
import com.instructure.canvasapi2.apis.PingAPI
import com.instructure.canvasapi2.builders.RestBuilder
import com.instructure.canvasapi2.builders.RestParams
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.Student
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.Prefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.parentapp.BuildConfig
import com.instructure.parentapp.R
import com.instructure.parentapp.util.ViewUtils
import kotlinx.coroutines.experimental.Job
import retrofit2.Call
import retrofit2.Response
import java.util.ArrayList
import java.util.HashMap

class SplashActivity : AppCompatActivity() {

    private var checkSignedInJob: Job? = null
    private var checkRegionJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        setContentView(R.layout.activity_splash)

        when {
            ApiPrefs.token.isNotBlank() -> checkSignedIn() // They have a token
            ApiPrefs.airwolfDomain.isEmpty() -> checkRegion() // They do not have a token but have an airwolf domain
            else -> navigateLoginLandingPage() // They have no token or airwolf domain
        }
    }

    private fun checkSignedIn() {
        checkSignedInJob = weave {
            //Now get it from the new place. This will be the true token whether they signed into dev/retrofit or the old way.
            val token = ApiPrefs.token
            ApiPrefs.protocol = "https"

            if (token.isNotBlank()) {
                //We now need to get the cache user
                val prefs = Prefs(ContextKeeper.appContext, getString(R.string.app_name_parent))
                val parentId = prefs.load(Const.ID, "")

                if (!TextUtils.isEmpty(parentId)) {
                    UserManager.getStudentsForParentAirwolf(ApiPrefs.airwolfDomain, parentId, object : StatusCallback<List<Student>>() {
                        override fun onResponse(response: Response<List<Student>>, linkHeaders: LinkHeaders, type: ApiType) {
                            val body = response.body()
                            if (body != null && body.isNotEmpty()) {
                                // They have students that they are observing, take them to that activity
                                startActivity(StudentViewActivity.createIntent(ContextKeeper.appContext, body))
                            } else {
                                // No response or no students observable; Take the parent to the add user page.
                                startActivity(FindSchoolActivity.createIntent(ContextKeeper.appContext, true))
                            }

                            finish()
                        }
                    })
                }
            }
        }
    }

    //is the region set?
    private fun checkRegion() {
        checkRegionJob = weave {
            if (BuildConfig.IS_TESTING) {
                Logger.d("QA Testing - Setting to Gamma Domain")
                ApiPrefs.airwolfDomain = BuildConfig.GAMMA_DOMAIN
                navigateLoginLandingPage()
                return@weave
            }

            //keep track of how many api calls have finished
            callbackCount = 0

            if (ApiPrefs.airwolfDomain.isEmpty()) {
                //get the region
                val pingMap = HashMap<String, ArrayList<Long>>()

                val pingCallback = object : StatusCallback<Void>() {
                    override fun onResponse(response: Response<Void>, linkHeaders: LinkHeaders, type: ApiType) {
                        callbackCount++
                        checkPingTime(pingMap, response)
                        checkCount(pingMap)
                    }

                    override fun onFail(call: Call<Void>?, error: Throwable, response: Response<*>?) {
                        callbackCount++
                        checkCount(pingMap)
                    }
                }

                val adapter = RestBuilder(pingCallback)
                val params = RestParams.Builder()
                        .withShouldIgnoreToken(false)
                        .withPerPageQueryParam(false)
                        .withAPIVersion("")
                        .build()

                //make the actual api calls
                for (url in AlertAPI.AIRWOLF_DOMAIN_LIST) { // 5 Pings per Domain
                    for (i in 0 until PING_COUNT) {
                        PingAPI.getPing(url, adapter, pingCallback, params)
                    }
                }
            } else {
                navigateLoginLandingPage()
            }
        }
    }

    private fun checkPingTime(pingMap: HashMap<String, ArrayList<Long>>, response: retrofit2.Response<*>?) {
        if (response == null) return

        try {
            val okResponse = response.raw()
            val url = "https://" + okResponse.request().url().url().host
            var ping: ArrayList<Long>? = pingMap[url]

            if (ping == null) ping = ArrayList()

            if (response.code() in 200..299) {
                //Only add pings that are valid 200s
                ping.add(okResponse.receivedResponseAtMillis() - okResponse.sentRequestAtMillis())
            }

            pingMap.put(url, ping)

        } catch (e: Exception) {
            Logger.e("Could not ping the pong.")
        }
    }

    private fun checkCount(pingMap: HashMap<String, ArrayList<Long>>) {
        //check to see if we've gone through all of the domains
        // (The multiplier should be equal to the number of pings we do below)
        if (callbackCount >= AlertAPI.AIRWOLF_DOMAIN_LIST.size * PING_COUNT) {
            var bestRegion = ""
            var bestTime = java.lang.Long.MAX_VALUE

            for (domain in pingMap.keys) {
                val pings = pingMap[domain]
                var sum = 0L
                if (pings?.isNotEmpty() == true) {
                    for (ping in pings) {
                        sum += ping
                    }
                    val average = sum / pings.size

                    if (average < bestTime) {
                        bestRegion = domain
                        bestTime = average
                        Log.d("Region", "New best time for region ($bestTime) $domain")
                    } else {
                        Log.d("Region", "Region didn't make the cut for user " + domain)
                    }
                } else {
                    Log.d("Region", "Failed to find pings for " + domain)
                }
            }

            Log.d("Region", "Closest url is " + bestRegion)
            //save the url with the lowest time
            ApiPrefs.airwolfDomain = bestRegion

            navigateLoginLandingPage()
        }
    }

    private fun navigateLoginLandingPage() {
        startActivity(ParentLoginActivity.createIntent(ContextKeeper.appContext))
        finish()
    }

    companion object {
        private val PING_COUNT = 5
        private var callbackCount: Int = 0

        @JvmStatic fun createIntent(context: Context): Intent {
            val intent = Intent(context, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            return intent
        }

        @JvmStatic fun createIntent(context: Context, showMessage: Boolean, message: String): Intent {
            val intent = Intent(context, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(Const.SHOW_MESSAGE, showMessage)
            intent.putExtra(Const.MESSAGE_TO_USER, message)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            return intent
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checkSignedInJob?.cancel()
        checkRegionJob?.cancel()
    }
}
