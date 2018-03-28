/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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
 */

package com.instructure.teacher.presenters

import com.instructure.canvasapi2.managers.ToDoManager
import com.instructure.canvasapi2.models.ToDo
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.teacher.viewinterface.InitActivityView
import instructure.androidblueprint.Presenter

class InitActivityPresenter : Presenter<InitActivityView> {

    // Before the view is ready to be used we have a few pieces of data we need to get
    // The data can be from cache or network

    private var mView: InitActivityView? = null
    var apiCall: WeaveJob? = null

    override fun onViewAttached(view: InitActivityView): InitActivityPresenter {
        mView = view
        return this
    }

    fun loadData(forceNetwork: Boolean) {
        // Get the to do count
        apiCall = tryWeave {
            // Get To dos
            val todos = awaitApi<List<ToDo>> { ToDoManager.getUserTodos(it, forceNetwork) }
            // Now count now students need grading
            val count = todos.sumBy { it.needsGradingCount }
            mView?.updateTodoCount(count)

        } catch {
            it.printStackTrace()
        }
    }


    override fun onViewDetached() {
        mView = null
    }

    override fun onDestroyed() {
        apiCall?.cancel()
    }
}
