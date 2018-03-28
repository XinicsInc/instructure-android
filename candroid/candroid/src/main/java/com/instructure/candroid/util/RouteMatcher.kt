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
package com.instructure.candroid.util

import android.support.v4.app.Fragment
import com.instructure.candroid.R
import com.instructure.candroid.activity.LoginActivity
import com.instructure.candroid.fragment.DashboardFragment
import com.instructure.candroid.fragment.EditFavoritesFragment
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ContextKeeper
import com.instructure.canvasapi2.utils.Logger
import com.instructure.interactions.router.*

object RouteMatcher : BaseRouteMatcher() {

    init {
        /* Fullscreen Fragments */
        fullscreenFragments.add(DashboardFragment::class.java)

        /* Bottom Sheet Fragments */
        bottomSheetFragments.add(EditFavoritesFragment::class.java)

    }

    fun routeUrl(url: String, routeContext: RouteContext) {
        routeUrl(url, ApiPrefs.domain, routeContext)
    }

    fun routeUrl(url: String, domain: String, routeContext: RouteContext) {
        /* Possible activity types we can navigate too: Unknown Link, InitActivity, Master/Detail, Fullscreen, WebView, ViewMedia */

        //Find the best route
        //Pass that along to the activity
        //One or two classes? (F, or M/D)

        route(getInternalRoute(url, domain))
    }


    fun route(route: Route?) {
        if (route == null || route.routeContext === RouteContext.DO_NOT_ROUTE) {
            if (route?.url != null) {
                //No route, no problem
                handleWebViewUrl(route.url)
            }
        } else if (route.routeContext === RouteContext.FILE) {
            if (route.queryParamsHash.containsKey(RouterParams.VERIFIER) && route.queryParamsHash.containsKey(RouterParams.DOWNLOAD_FRD)) {
                if (route.url != null) {
                    //openMedia(context as FragmentActivity, route.url)
                } else if (route.uri != null) {
                    //openMedia(context as FragmentActivity, route.uri!!.toString())
                }
            } else {
                //handleSpecificFile(context as FragmentActivity, route.getParamsHash()[RouterParams.FILE_ID])
            }

        } else if (route.routeContext === RouteContext.MEDIA) {
            handleMediaRoute(route)
        } else if (ContextKeeper.appContext.resources.getBoolean(R.bool.is_device_tablet)) {
            handleTabletRoute(route)
        } else {
            handleFullscreenRoute(route)
        }
    }

    private fun handleTabletRoute(route: Route) {
        Logger.i("RouteMatcher:handleTabletRoute()")
        LoginActivity.createIntent(ContextKeeper.appContext, route)
    }

    private fun handleMasterDetailRoute(route: Route) {
        Logger.i("RouteMatcher:handleMasterDetailRoute()")
        route.routeType = RouteType.MASTER_DETAIL
    }

    private fun handleFullscreenRoute(route: Route) {
        Logger.i("RouteMatcher:handleFullscreenRoute()")

    }

    private fun handleMediaRoute(route: Route) {
        Logger.i("RouteMatcher:handleMediaRoute()")

    }

    private fun handleWebViewUrl(url: String?) {
        Logger.i("RouteMatcher:handleWebViewRoute()")

    }

    /**
     * Returns true if url can be routed to a fragment, false otherwise
     * @param activity
     * @param url
     * @param routeIfPossible
     * @return
     */
    fun canRouteInternally(url: String, domain: String, routeIfPossible: Boolean): Boolean {
        val canRoute = getInternalRoute(url, domain) != null

        if (canRoute && routeIfPossible) {
            routeUrl(url, RouteContext.INTERNAL)
        }
        return canRoute
    }

    @Suppress("UNCHECKED_CAST")
    private fun<TYPE> getFrag(cls: Class<out Fragment>?, canvasContext: CanvasContext, route: Route): TYPE? where TYPE : Fragment {
        if (cls == null) return null

        var fragment: Fragment? = null

        if(DashboardFragment::class.java.isAssignableFrom(cls)) {
            fragment = DashboardFragment.newInstance(canvasContext)
        }

        return fragment as TYPE
    }
}

