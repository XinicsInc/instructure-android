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
package com.instructure.teacher.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.interactions.FullScreenInteractions
import com.instructure.pandautils.interfaces.NavigationCallbacks
import com.instructure.pandautils.utils.Const
import com.instructure.teacher.R
import com.instructure.teacher.events.AssignmentDescriptionEvent
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import instructure.rceditor.RCEConst.HTML_RESULT
import instructure.rceditor.RCEFragment
import kotlinx.android.synthetic.main.activity_fullscreen.*
import org.greenrobot.eventbus.EventBus
import retrofit2.Response

class FullscreenActivity : BaseAppCompatActivity(), RCEFragment.RCEFragmentCallbacks, FullScreenInteractions {

    private var mRoute: Route? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)

        if (savedInstanceState == null) {

            mRoute = intent.extras.getParcelable(Route.ROUTE)

            if (mRoute == null) {
                finish()
            }

            if (mRoute?.canvasContext != null && mRoute?.canvasContext is Course) {
                setupWithCanvasContext(mRoute?.canvasContext as Course)
            } else {
                val contextId = Route.extractCourseId(mRoute)
                if (contextId == 0L) {
                    //No CanvasContext, No URL
                    setupWithCanvasContext(null)
                } else {
                    CourseManager.getCourse(contextId, object : StatusCallback<Course>() {
                        override fun onResponse(response: Response<Course>, linkHeaders: LinkHeaders, type: ApiType) {
                            setupWithCanvasContext(response.body() as Course)
                        }
                    }, false)
                }
            }
        }
    }

    private fun setupWithCanvasContext(course: Course?) {
        addFragment(RouteMatcher.getFullscreenFragment(course, mRoute!!))
    }

    private fun addFragment(fragment: Fragment?) {
        if(fragment == null) throw IllegalStateException("FullscreenActivity.class addFragment was null")
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        val currentFragment = fm.findFragmentById(container.id)
        if(currentFragment != null) {
            //Add to back stack if a fragment exists
            ft.addToBackStack(fragment.javaClass.simpleName)
        }
        ft.replace(container.id, fragment, fragment.javaClass.simpleName)
        ft.commit()
    }

    override fun onBackPressed() {
        //Captures back press to prevent accidental exiting of assignment editing.
        if(supportFragmentManager.findFragmentById(R.id.container) is RCEFragment) {
            (supportFragmentManager.findFragmentById(R.id.container) as RCEFragment).showExitDialog()
            return
        } else if(supportFragmentManager.findFragmentById(R.id.container) is NavigationCallbacks) {
            if((supportFragmentManager.findFragmentById(R.id.container) as NavigationCallbacks).onHandleBackPressed()) return
        }
        super.onBackPressed()
    }

    /**
     * Handles RCEFragment results and passes them along
     */
    override fun onResult(activityResult: Int, data: Intent?) {
        if (activityResult == Activity.RESULT_OK && data != null) {
            EventBus.getDefault().postSticky(AssignmentDescriptionEvent(data.getStringExtra(HTML_RESULT)))
            super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, route: Route): Intent {
            val intent = Intent(context, FullscreenActivity::class.java)
            intent.putExtra(Route.ROUTE, route as Parcelable)
            return intent
        }
    }
}
