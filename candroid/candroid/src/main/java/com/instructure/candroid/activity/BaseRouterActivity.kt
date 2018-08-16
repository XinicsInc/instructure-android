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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.fragment.*
import com.instructure.candroid.fragment.LTIWebViewRoutingFragment
import com.instructure.candroid.model.PushNotification
import com.instructure.candroid.util.*
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.*
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.Logger
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.interactions.Navigation
import com.instructure.pandautils.loaders.OpenMediaAsyncTaskLoader
import com.instructure.pandautils.receivers.PushExternalReceiver
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.LoaderUtils
import kotlinx.coroutines.experimental.Job
import java.util.*

//Intended to handle all routing to fragments from links both internal and external
abstract class BaseRouterActivity : CallbackActivity() {

    private var routeCanvasContextJob: Job? = null
    private var routeModuleProgressionJob: Job? = null
    private var routeLTIJob: Job? = null

    protected abstract fun routeFragment(fragment: ParentFragment)
    protected abstract fun existingFragmentCount(): Int
    protected abstract fun loadLandingPage(clearBackStack: Boolean = false)

    protected abstract fun showLoadingIndicator()
    protected abstract fun hideLoadingIndicator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d("BaseRouterActivity: onCreate()")

        if (savedInstanceState == null) {
            parse(intent)
        }

        LoaderUtils.restoreLoaderFromBundle<LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia>>(
                this.supportLoaderManager, savedInstanceState, loaderCallbacks, R.id.openMediaLoaderID, Const.OPEN_MEDIA_LOADER_BUNDLE)

        if (savedInstanceState?.getBundle(Const.OPEN_MEDIA_LOADER_BUNDLE) != null) {
            showLoadingIndicator()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        LoaderUtils.saveLoaderBundle(outState, openMediaBundle, Const.OPEN_MEDIA_LOADER_BUNDLE)
        hideLoadingIndicator()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Logger.d("BaseRouterActivity: onNewIntent()")
        parse(intent)
    }

    //region OpenMediaAsyncTaskLoader

    private var openMediaBundle: Bundle? = null
    private var openMediaCallbacks: LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia>? = null

    //show pdf with PSPDFkit - set to null, otherwise the progressDialog will appear again
    private val loaderCallbacks: LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia>
        get() {
            if (openMediaCallbacks == null) {
                openMediaCallbacks = object : LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia> {
                    override fun onCreateLoader(id: Int, args: Bundle): Loader<OpenMediaAsyncTaskLoader.LoadedMedia> {
                        showLoadingIndicator()
                        return OpenMediaAsyncTaskLoader(context, args)
                    }

                    override fun onLoadFinished(loader: Loader<OpenMediaAsyncTaskLoader.LoadedMedia>, loadedMedia: OpenMediaAsyncTaskLoader.LoadedMedia) {
                        hideLoadingIndicator()

                        try {
                            if (loadedMedia.isError) {
                                Toast.makeText(context, getString(loadedMedia.errorMessage), Toast.LENGTH_LONG).show()
                            } else if (loadedMedia.isHtmlFile) {
                                InternalWebviewFragment.loadInternalWebView(this@BaseRouterActivity, this@BaseRouterActivity as Navigation, loadedMedia.bundle)
                            } else if (loadedMedia.intent != null) {
                                if (loadedMedia.intent.type!!.contains("pdf")) {
                                    val uri = loadedMedia.intent.data
                                    FileUtils.showPdfDocument(uri, loadedMedia, context)
                                } else {
                                    context.startActivity(loadedMedia.intent)
                                }
                            }
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, R.string.noApps, Toast.LENGTH_LONG).show()
                        }

                        openMediaBundle = null
                    }

                    override fun onLoaderReset(loader: Loader<OpenMediaAsyncTaskLoader.LoadedMedia>) {

                    }
                }
            }
            return openMediaCallbacks!!
        }

    // endregion

    /**
     * Handles the Route based on Navigation context, route type, and master/detail classes
     * Use RouterUtils.canRouteInternally()
     * @param route
     */
    fun handleRoute(route: RouterUtils.Route) {
        try {
            if (route.paramsHash.containsKey(Param.COURSE_ID)) {
                val courseId: Long? = parseCourseId(route.paramsHash[Param.COURSE_ID]!!) ?: return

                if (RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD == route.routeType) {
                    if (route.queryParamsHash.containsKey(Param.VERIFIER) && route.queryParamsHash.containsKey(Param.DOWNLOAD_FRD)) {
                        openMedia(CanvasContext.getGenericContext(CanvasContext.Type.COURSE, courseId!!, ""), route.url)
                        return
                    }
                    route.paramsHash[Param.FILE_ID]?.let { handleSpecificFile(courseId!!, it) }
                    return
                }


                if (RouterUtils.ROUTE_TYPE.LTI == route.routeType) {
                    routeLTI(courseId!!, route)
                } else {
                    val tab = TabHelper.getTabForType(this, route.tabId)
                    if (route.contextType == CanvasContext.Type.COURSE) {
                        routeToCourse(courseId!!, route, tab)
                    } else if (route.contextType == CanvasContext.Type.GROUP) {
                        routeToGroup(courseId!!, route, tab)
                    }
                }
                return  // do not remove return
            }

            val canvasContext = CanvasContext.emptyUserContext()
            if (RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD == route.routeType) {
                openMedia(canvasContext, route.url)
                return
            }

            if (RouterUtils.ROUTE_TYPE.NOTIFICATION_PREFERENCES == route.routeType) {
                Analytics.trackAppFlow(this@BaseRouterActivity, NotificationPreferencesActivity::class.java)
                startActivity(Intent(context, NotificationPreferencesActivity::class.java))
                return
            }

            if (route.masterCls != null) {
                val bundle = ParentFragment.createBundle(canvasContext, route.paramsHash, route.queryParamsHash, route.url, null)
                if (route.detailCls != null) {
                    if (existingFragmentCount() == 0) {
                        //Add the landing page fragment, then the details fragment.
                        loadLandingPage()
                    }
                    val fragment = FragUtils.getFragment(route.detailCls, bundle)
                    if(fragment != null) routeFragment(fragment)
                } else {
                    val fragment = FragUtils.getFragment(route.masterCls, bundle)
                    if(fragment != null) routeFragment(fragment)
                }
            }

        } catch (e: Exception) {
            LoggingUtility.LogExceptionPlusCrashlytics(this@BaseRouterActivity, e)
            Logger.e("Could not parse and route url in BaseRouterActivity")
            routeToCourseGrid()
        }
    }

    /**
     * The intent will have information about the url to open (usually from clicking on a link in an email)
     * @param intent
     */
    private fun parse(intent: Intent?) {
        if (intent == null || intent.extras == null) return

        val extras = intent.extras
        Logger.logBundle(extras)

        if (extras.containsKey(Const.MESSAGE) && extras.containsKey(Const.MESSAGE_TYPE)) {
            showMessage(extras.getString(Const.MESSAGE))
        }

        if (extras.containsKey(Const.PARSE)) {
            val url = extras.getString(Const.URL)
            RouterUtils.routeUrl(this, url, false)
        } else if (extras.containsKey(Const.BOOKMARK)) {
            val url = extras.getString(Const.URL)
            RouterUtils.routeUrl(this, url, false)
        } else if (extras.containsKey(PushExternalReceiver.NEW_PUSH_NOTIFICATION)) {
            val url = extras.getString(PushNotification.HTML_URL)
            RouterUtils.routeUrl(this, url, false)
        }
    }

    private fun routeLTI(courseId: Long, route: RouterUtils.Route) {
        //Since we do not know if the LTI is a tab we load in a details fragment.
        if (route.contextType == CanvasContext.Type.COURSE) {
            routeLTIForCourse(courseId, route)
        } else if (route.contextType == CanvasContext.Type.GROUP) {
            routeLTIForGroup(courseId, route)
        }
    }

    private fun routeLTIForCourse(courseId: Long, route: RouterUtils.Route) {
        Logger.d("BaseRouterActivity: routeLTIForCourse()")

        routeLTIJob = tryWeave {
            showLoadingIndicator()
            val course = awaitApi<Course?> { CourseManager.getCourseWithGrade(courseId, it, false) }
            if(course == null) {
                showMessage(getString(R.string.could_not_route_course))
            } else {
                routeFragment(ParentFragment.createFragment(LTIWebViewRoutingFragment::class.java,
                        LTIWebViewRoutingFragment.createBundle(course, route.url)))
            }
            hideLoadingIndicator()
        } catch {
            hideLoadingIndicator()
            Logger.e("Error routing to LTI for course: " + it.message)
        }
    }

    private fun routeLTIForGroup(groupId: Long, route: RouterUtils.Route) {
        Logger.d("BaseRouterActivity: routeLTIForGroup()")

        routeLTIJob = tryWeave {
            showLoadingIndicator()
            val group = awaitApi<Group?> { GroupManager.getDetailedGroup(groupId, it, false) }
            if(group == null) {
                showMessage(getString(R.string.could_not_route_group))
            } else {
                routeFragment(ParentFragment.createFragment(LTIWebViewRoutingFragment::class.java,
                        LTIWebViewRoutingFragment.createBundle(group, route.url)))
            }
            hideLoadingIndicator()
        } catch {
            hideLoadingIndicator()
            Logger.e("Error routing to LTI for group: " + it.message)
        }
    }

    private fun routeModuleProgression(canvasContext: CanvasContext, route: RouterUtils.Route) {
        Logger.d("BaseRouterActivity: routeModuleProgression()")

        val moduleItemId = route.queryParamsHash["module_item_id"]
        if(moduleItemId != null) {
            routeModuleProgressionJob = tryWeave {
                showLoadingIndicator()
                val moduleItemSequence = awaitApi<ModuleItemSequence> { ModuleManager.getModuleItemSequence(canvasContext, ModuleManager.MODULE_ASSET_MODULE_ITEM, moduleItemId, it, false) }
                //make sure that there is a sequence
                if (moduleItemSequence.items.isNotEmpty()) {
                    //get the current module item. we'll use the id of this down below
                    val current = moduleItemSequence.items[0].current

                    val moduleItems = awaitApi<List<ModuleItem>> { ModuleManager.getAllModuleItems(canvasContext, current.moduleId, it, false) }

                    val moduleItemsArrayList = ArrayList<ArrayList<ModuleItem>>(1)
                    moduleItemsArrayList.add(ArrayList(moduleItems))

                    val moduleObjectsArray = ArrayList<ModuleObject>()
                    moduleObjectsArray.add(moduleItemSequence.modules[0])

                    val moduleHelper = ModuleProgressionUtility.prepareModulesForCourseProgression(this@BaseRouterActivity, current.id, moduleObjectsArray, moduleItemsArrayList)

                    routeFragment(ParentFragment.createFragment(CourseModuleProgressionFragment::class.java, CourseModuleProgressionFragment.createBundle(
                            moduleObjectsArray,
                            moduleHelper.strippedModuleItems,
                            canvasContext as Course?,
                            moduleHelper.newGroupPosition,
                            moduleHelper.newChildPosition)))
                }
                hideLoadingIndicator()
            } catch {
                hideLoadingIndicator()
                Logger.e("Error routing modules: " + it.message)
            }
        }
    }

    private fun routeToCourseGrid() {
        Logger.d("BaseRouterActivity: routeToCourseGrid()")
        routeFragment(FragUtils.getFrag(DashboardFragment::class.java))
    }

    private fun routeMasterDetail(canvasContext: CanvasContext, route: RouterUtils.Route, tab: Tab?) {
        Logger.d("routing with tab: " + if (tab == null) "??" else tab.tabId)
        val bundle = ParentFragment.createBundle(canvasContext, route.paramsHash, route.queryParamsHash, route.url, tab)
        if (route.detailCls != null) {
            if (existingFragmentCount() == 0) {
                //Add the landing page fragment, then the details fragment.
                loadLandingPage()
            }

            //check if it's supposed to go to a quiz so we can check which page to route it to
            if(route.detailCls == BasicQuizViewFragment::class.java) {
                //if we can get a quiz and display it natively, we should do that
                routeToQuiz(route, canvasContext, bundle)
            } else {
                val fragment = FragUtils.getFragment(route.detailCls, bundle)
                if(fragment != null) routeFragment(fragment)
            }

        } else {
            if (route.masterCls != null) {
                val fragment = FragUtils.getFragment(route.masterCls, bundle)
                if(fragment != null) routeFragment(fragment)
            } else { // Used for Tab.Home (so that no masterCls has to be set)
                routeFragment(TabHelper.getFragmentByTab(tab, canvasContext))
            }
        }
    }

    private fun routeDetail(canvasContext: CanvasContext, route: RouterUtils.Route, tab: Tab?) {
        Logger.d("routing to single fragment: " + if (tab == null) "??" else tab.tabId)
        val bundle = ParentFragment.createBundle(canvasContext, route.paramsHash, route.queryParamsHash, route.url, tab)
        if (route.detailCls != null) {
            if (existingFragmentCount() == 0) {
                //Add the landing page fragment, then the details fragment.
                loadLandingPage()
            }

            //check if it's supposed to go to a quiz so we can check which page to route it to
            if(route.detailCls == BasicQuizViewFragment::class.java) {
                //if we can get a quiz and display it natively, we should do that
                routeToQuiz(route, canvasContext, bundle)
            } else {
                val fragment = FragUtils.getFragment(route.detailCls, bundle)
                if(fragment != null) routeFragment(fragment)
            }

        } else {
            // if the detailCls is null and the masterCls is null, that means we're linking to a navigation list (pages, people, assignments, etc)
            if (route.masterCls != null && tabCanStillBeLinkedTo(tab)) {
                val fragment = FragUtils.getFragment(route.masterCls, bundle)
                if(fragment != null) routeFragment(fragment)
            } else { // Used for Tab.Home (so that no masterCls has to be set)
                //cannot route because tabs are locked.
                showMessage(getString(R.string.could_not_route_locked))
            }
        }
    }

    /*
        Some tabs are hidden and can still be linked to (People, Discussions, Grades) and others are truly disabled (can't be linked to)
     */
    private fun tabCanStillBeLinkedTo(tab: Tab?) : Boolean = (tab?.tabId == Tab.PEOPLE_ID || tab?.tabId == Tab.DISCUSSIONS_ID || tab?.tabId == Tab.GRADES_ID)

    private fun routeToQuiz(route: RouterUtils.Route, canvasContext: CanvasContext, bundle: Bundle) {
        var apiURL = route.url.substring(ApiPrefs.fullDomain.length)
        apiURL = ApiPrefs.fullDomain + "/api/v1" + apiURL

        tryWeave {
            val quiz = awaitApi<Quiz?> { QuizManager.getDetailedQuizByUrl(apiURL, true, it) }
            if (QuizListFragment.isNativeQuiz(canvasContext, quiz)) {
                val quizBundle = QuizStartFragment.createBundle(canvasContext, quiz)
                routeFragment(FragUtils.getFrag(QuizStartFragment::class.java, quizBundle))
            } else {
                val fragment = FragUtils.getFragment(route.detailCls, bundle)
                if(fragment != null) routeFragment(fragment)
            }
        } catch {
            val fragment = FragUtils.getFragment(route.detailCls, bundle)
            if(fragment != null) routeFragment(fragment)
        }
    }

    private fun routeToCourse(courseId: Long, route: RouterUtils.Route, tab: Tab) {
        Logger.d("BaseRouterActivity: routeToCourse()")

        routeCanvasContextJob = tryWeave {
            showLoadingIndicator()
            val course = awaitApi<Course?> { CourseManager.getCourseWithGrade(courseId, it, false) }
            if(course == null) {
                showMessage(getString(R.string.could_not_route_course))
            } else {
                val tabs = awaitApi<List<Tab>> { TabManager.getTabs(course, it, false) }
                val tabExists = tabs.any { it.tabId == tab.tabId }

                routeWithCanvasContextAndTab(course, route, tab, tabExists)
            }
            hideLoadingIndicator()
        } catch {
            hideLoadingIndicator()
            Logger.e("Error routing to course: " + it.message)
        }
    }

    private fun routeToGroup(groupId: Long, route: RouterUtils.Route, tab: Tab) {
        Logger.d("BaseRouterActivity: routeToGroup()")

        routeCanvasContextJob = tryWeave {
            showLoadingIndicator()
            val group = awaitApi<Group?> { GroupManager.getDetailedGroup(groupId, it, false) }
            if(group == null) {
                showMessage(getString(R.string.could_not_route_group))
            } else {
                val tabs = awaitApi<List<Tab>> { TabManager.getTabs(group, it, false) }
                val tabExists = tabs.any { it.tabId == tab.tabId }

                routeWithCanvasContextAndTab(group, route, tab, tabExists)
            }
            hideLoadingIndicator()
        } catch {
            hideLoadingIndicator()
            Logger.e("Error routing to group: " + it.message)
        }
    }

    private fun routeWithCanvasContextAndTab(canvasContext: CanvasContext, route: RouterUtils.Route, tab: Tab, tabExists: Boolean) {
        if (Tab.SYLLABUS_ID == tab.tabId) {
            //Route cause tab exists
            Logger.d("Attempting to route to group: " + canvasContext.name)
            routeMasterDetail(canvasContext, route, tab)
        } else if (route.queryParamsHash != null && route.queryParamsHash.containsKey("module_item_id")) {
            //if we're routing to something in a module then we need to open it inside of CourseModuleProgression
            routeModuleProgression(canvasContext, route)
        } else {
            Logger.d("Attempting to route to course or group: " + canvasContext.name)
            if(tabExists) {
                routeMasterDetail(canvasContext, route, tab)
            } else {
                routeDetail(canvasContext, route, tab)
            }
        }
    }

    private fun handleSpecificFile(courseId: Long, fileID: String) {
        val canvasContext = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, courseId, "")
        Logger.d("BaseRouterActivity: handleSpecificFile()")
        //If the file no longer exists (404), we want to show a different crouton than the default.
        val fileFolderCanvasCallback = object : StatusCallback<FileFolder>() {
            override fun onResponse(response: retrofit2.Response<FileFolder>, linkHeaders: LinkHeaders, type: ApiType) {
                response.body()?.let {
                    if (it.isLocked || it.isLockedForUser) {
                        Toast.makeText(context, String.format(context.getString(R.string.fileLocked), if (it.displayName == null) getString(R.string.file) else it.displayName), Toast.LENGTH_LONG).show()
                    } else {
                        openMedia(canvasContext, it.contentType.orEmpty(), it.url.orEmpty(), it.displayName.orEmpty())
                    }
                }
            }
        }

        FileFolderManager.getFileFolderFromURL("files/" + fileID, fileFolderCanvasCallback)
    }

    fun openMedia(canvasContext: CanvasContext?, url: String) {
        openMediaBundle = OpenMediaAsyncTaskLoader.createBundle(canvasContext, url)
        LoaderUtils.restartLoaderWithBundle<LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia>>(this.supportLoaderManager, openMediaBundle, loaderCallbacks, R.id.openMediaLoaderID)
    }

    fun openMedia(canvasContext: CanvasContext?, mime: String, url: String, filename: String) {
        openMediaBundle = OpenMediaAsyncTaskLoader.createBundle(canvasContext, mime, url, filename)
        LoaderUtils.restartLoaderWithBundle<LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia>>(this.supportLoaderManager, openMediaBundle, loaderCallbacks, R.id.openMediaLoaderID)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeCanvasContextJob?.cancel()
        routeModuleProgressionJob?.cancel()
        routeLTIJob?.cancel()
    }

    companion object {
        // region Used for param handling
        var SUBMISSIONS_ROUTE = "submissions"
        var RUBRIC_ROUTE = "rubric"

        @JvmStatic
        fun parseCourseId(courseId: String): Long? {
            try {
                return courseId.toLong()
            } catch (e: NumberFormatException) {
                Logger.e("Course ID passed to Router is invalid " + e)
                return null
            }
        }
    }
}