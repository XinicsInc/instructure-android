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
package com.instructure.teacher.presenters

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Favorite
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.teacher.R
import com.instructure.teacher.events.CourseUpdatedEvent
import com.instructure.teacher.viewinterface.CanvasContextView
import instructure.androidblueprint.SyncPresenter
import org.greenrobot.eventbus.EventBus
import retrofit2.Call
import retrofit2.Response
import java.util.*

class EditFavoritesPresenter(filter: (Course) -> Boolean) : SyncPresenter<CanvasContext, CanvasContextView>(CanvasContext::class.java) {

    override fun loadData(forceNetwork: Boolean) {
        if(isEmpty) {
            onRefreshStarted()
            CourseManager.getCourses(forceNetwork, mCoursesCallback)
        }
    }

    override fun refresh(forceNetwork: Boolean) {
        onRefreshStarted()
        mCoursesCallback.reset()
        clearData()
        loadData(forceNetwork)
    }

    //region FavoriteApiModel Courses Request

    private val mCoursesCallback = object :  StatusCallback<List<Course>>() {
        override fun onResponse(response: Response<List<Course>>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let { data.addOrUpdate(it.filter(filter)) }
        }

        override fun onFinished(type: ApiType) {
            viewCallback?.checkIfEmpty()
            viewCallback?.onRefreshFinished()
        }
    }

    fun setFavorite(canvasContext: CanvasContext, isFavorite: Boolean) {

        val mFavoriteCallback = object : StatusCallback<Favorite>() {
            override fun onResponse(response: Response<Favorite>, linkHeaders: LinkHeaders, type: ApiType) {
                data.addOrUpdate(canvasContext)
                EventBus.getDefault().postSticky(CourseUpdatedEvent(canvasContext as Course))
            }

            override fun onFail(call: Call<Favorite>?, error: Throwable, response: Response<*>?) {
                //we already set the canvasContext to be favorited before we made the api call. Because it
                //failed we need to flip it
                (canvasContext as Course).isFavorite = !canvasContext.isFavorite
            }
        }

        if (canvasContext is Course) {
            if (isFavorite) {
                //make sure the term is still valid
                val date: Date = Date()
                if(canvasContext.term?.endAt?.before(date) == true) {
                    viewCallback?.showMessage(R.string.unable_to_add_course_to_favorites)
                    return
                }
                canvasContext.isFavorite = true
                mFavoriteCallback.reset()
                CourseManager.addCourseToFavorites(canvasContext.getId(), mFavoriteCallback, true)
            } else {

                canvasContext.isFavorite = false
                mFavoriteCallback.reset()
                CourseManager.removeCourseFromFavorites(canvasContext.getId(), mFavoriteCallback, true)
            }
        }
    }


    //endregion

    //region Comparison checks - Favorites API is not returning in the default ABC order as other apis
    public override fun compare(o1: CanvasContext, o2: CanvasContext): Int {
        return o2.name.toLowerCase(Locale.getDefault()).compareTo(o1.name.toLowerCase(Locale.getDefault()))
    }

    public override fun areItemsTheSame(item1: CanvasContext, item2: CanvasContext): Boolean {
        return item1.contextId.hashCode() == item2.contextId.hashCode()
    }

    public override fun areContentsTheSame(item1: CanvasContext, item2: CanvasContext): Boolean {
        return if (item1 is Course && item2 is Course) item1.isFavorite == item2.isFavorite else false
    }

    //endregion
}
