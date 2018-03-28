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
package com.instructure.teacher.fragments

import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Toast
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.utils.ContextKeeper
import com.instructure.pandautils.fragments.BaseSyncFragment
import com.instructure.pandautils.utils.SerializableArg
import com.instructure.pandautils.utils.Utils
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.isTablet
import com.instructure.teacher.R
import com.instructure.teacher.adapters.EditFavoritesAdapter
import com.instructure.teacher.factory.EditFavoritesPresenterFactory
import com.instructure.teacher.holders.EditFavoritesViewHolder
import com.instructure.teacher.interfaces.AdapterToEditFavoriteCoursesCallback
import com.instructure.teacher.presenters.EditFavoritesPresenter
import com.instructure.teacher.utils.AppType
import com.instructure.teacher.utils.RecyclerViewUtils
import com.instructure.teacher.utils.isTablet
import com.instructure.teacher.utils.setupBackButton
import com.instructure.teacher.viewinterface.CanvasContextView
import kotlinx.android.synthetic.main.fragment_edit_favorites.*

class EditFavoritesFragment : BaseSyncFragment<
        CanvasContext,
        EditFavoritesPresenter,
        CanvasContextView,
        EditFavoritesViewHolder,
        EditFavoritesAdapter>(), CanvasContextView {

    lateinit private var mRecyclerView: RecyclerView

    // The user type, used when filtering the course list
    private var mAppType: AppType by SerializableArg(default = AppType.TEACHER)
    lateinit private var mLayoutManager : LinearLayoutManager
    override fun layoutResId() = R.layout.fragment_edit_favorites
    override fun getList() = presenter.data
    override fun getRecyclerView(): RecyclerView = mRecyclerView

    override fun onCreateView(view: View) {
       mLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    override fun onReadySetGo(presenter: EditFavoritesPresenter) {
        swipeRefreshLayout.setOnRefreshListener {
            if (!Utils.isNetworkAvailable(ContextKeeper.appContext)) {
                swipeRefreshLayout.isRefreshing = false
            } else {
                presenter.refresh(true)
            }
        }

        mRecyclerView.layoutManager = mLayoutManager
        addSwipeToRefresh(swipeRefreshLayout)
        mRecyclerView.adapter = adapter

        presenter.loadData(true)
    }

    override fun getPresenterFactory() = EditFavoritesPresenterFactory {
        when (mAppType) {
            AppType.TEACHER -> it.isTeacher || it.isTA || it.isDesigner
            AppType.STUDENT -> it.isStudent
            AppType.PARENT -> it.isObserver
        }
    }

    override fun onPresenterPrepared(presenter: EditFavoritesPresenter) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.favoritesRecyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))
        mRecyclerView.itemAnimator = null
    }

    override fun getAdapter(): EditFavoritesAdapter {
        if (mAdapter == null) {
            mAdapter = EditFavoritesAdapter(context, presenter, AdapterToEditFavoriteCoursesCallback {
                canvasContext,
                isFavorite -> presenter.setFavorite(canvasContext, isFavorite)
            })
        }
        return mAdapter
    }

    override fun showMessage(messageResId: Int) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun setupToolbar() {
        toolbar.title = getString(R.string.edit_courses)
        toolbar.setupBackButton(this)
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
    }

    override fun onRefreshFinished() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onRefreshStarted() {
        emptyPandaView.setLoading()
    }

    override fun checkIfEmpty() {
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    companion object {
        @JvmStatic val APP_TYPE = "appType"

        @JvmStatic
        fun newInstance(args: Bundle) = EditFavoritesFragment().apply {
            mAppType = args.getSerializable(APP_TYPE) as AppType
        }

        @JvmStatic
        fun makeBundle(appType: AppType): Bundle {
            val args = Bundle()
            args.putSerializable(EditFavoritesFragment.APP_TYPE, appType)
            return args
        }
    }
}
