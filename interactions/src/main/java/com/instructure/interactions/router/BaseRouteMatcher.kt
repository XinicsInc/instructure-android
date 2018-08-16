package com.instructure.interactions.router

import android.support.v4.app.Fragment
import com.instructure.canvasapi2.models.CanvasContext
import java.util.ArrayList
import java.util.HashMap

open class BaseRouteMatcher {

    private val COURSE_OR_GROUP_REGEX = "/(?:courses|groups)"

    protected fun courseOrGroup(route: String) = COURSE_OR_GROUP_REGEX + route

    protected val routes = ArrayList<Route>()
    protected val fullscreenFragments = ArrayList<Class<out Fragment>>()
    protected val bottomSheetFragments = ArrayList<Class<out Fragment>>()

    protected fun isFullScreenClass(clazz: Class<out Fragment>): Boolean {
        return fullscreenFragments.contains(clazz)
    }

    protected fun isBottomSheetClass(clazz: Class<out Fragment>): Boolean {
        return bottomSheetFragments.contains(clazz)
    }

    /**
     * Gets the Route, null if route cannot be handled internally
     * @param url A Url String
     * @return Route if application can handle link internally; null otherwise
     */
    fun getInternalRoute(url: String, domain: String): Route? {
        val urlValidator = UrlValidator(url, domain)

        if (!urlValidator.isHostForLoggedInUser || !urlValidator.isValid) {
            return null
        }

        return routes.find { it.apply(url) }?.takeUnless {
            RouteContext.INTERNAL === it.routeContext || RouteContext.DO_NOT_ROUTE === it.routeContext
        }
    }

    fun getInternalRoute(primaryClass: Class<out Fragment>, secondaryClass: Class<out Fragment>): Route? {
        return routes.find { it.apply(primaryClass, secondaryClass) }
    }

    /**
     * Gets a course id from a url, if url is invalid or could not be parsed a 0 will return an empty string.
     * @param url a Url String
     * @return a CanvasContext context_id (group_12345, course_12345)
     */
    fun getContextIdFromURL(url: String?, routes: List<Route>): String? {
        if (url.isNullOrEmpty()) {
            return ""
        }

        try {
            var params = HashMap<String, String>()
            var route: Route? = null

            for (r in routes) {
                if (r.apply(url)) {
                    route = r
                    params = r.paramsHash
                    break
                }
            }

            return when {
                route == null -> ""
                params.containsKey(RouterParams.COURSE_ID) -> return CanvasContext.makeContextId(route.getContextType(), params[RouterParams.COURSE_ID]!!.toLong())
                else -> return ""
            }

        } catch (e: Throwable) {
            return ""
        }
    }

    fun getCourseIdFromUrl(url: String?): String {
        if (url == null || url.isEmpty()) {
            return ""
        }

        return try {
            var params = HashMap<String, String>()
            val route = Route(courseOrGroup("/:course_id/(.*)"))

            if (route.apply(url)) {
                params = route.paramsHash
            }

            params[RouterParams.COURSE_ID] ?: ""
        } catch (e: Throwable) {
            ""
        }
    }
}