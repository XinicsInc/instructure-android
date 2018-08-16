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

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.adapter.NotificationPreferencesRecyclerAdapter
import com.instructure.canvasapi2.managers.CommunicationChannelsManager
import com.instructure.canvasapi2.models.CommunicationChannel
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.setupAsBackButton
import kotlinx.android.synthetic.main.notification_preferences_activity.*
import kotlinx.android.synthetic.main.notification_preferences_activity.listView as mRecyclerView

@PageView(url = "profile/communication")
class NotificationPreferencesActivity : AppCompatActivity() {

    lateinit private var mAdapter: NotificationPreferencesRecyclerAdapter

    private var mPushChannel: CommunicationChannel? = null

    private var mApiCalls: WeaveJob? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notification_preferences_activity)
        setupToolbar()
        configureRecyclerView()
        savedInstanceState?.getParcelable<CommunicationChannel>(Const.ITEM)?.let {
            mPushChannel = it
            mAdapter.fetchNotificationPreferences(it)
        } ?: fetchCommunicationChannels()
    }

    private fun setupToolbar() {
        toolbar.setupAsBackButton { finish() }
        ViewStyler.themeToolbar(this, toolbar, Color.WHITE, Color.BLACK, false)
    }

    fun configureRecyclerView() {
        mAdapter = NotificationPreferencesRecyclerAdapter(this)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.isSelectionEnabled = false
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.setEmptyView(emptyPandaView)
    }

    private fun fetchCommunicationChannels() {
        mApiCalls?.cancel()
        mApiCalls = tryWeave {
            val channels = awaitApi<List<CommunicationChannel>> { CommunicationChannelsManager.getCommunicationChannels(ApiPrefs.user!!.id, it, false) }
            mPushChannel = channels.first { "push".equals(it.type, true) }
            mAdapter.fetchNotificationPreferences(mPushChannel!!)
        } catch {
            Toast.makeText(this, R.string.pushNotificationsError, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mPushChannel != null) outState.putParcelable(Const.ITEM, mPushChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        mApiCalls?.cancel()
        mAdapter.cancel()
    }

}
