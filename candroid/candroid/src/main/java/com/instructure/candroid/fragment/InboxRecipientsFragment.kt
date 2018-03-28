/*
 * Copyright (C) 2018 - present  Instructure, Inc.
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
package com.instructure.candroid.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.instructure.candroid.R
import com.instructure.candroid.adapter.InboxRecipientAdapter
import com.instructure.candroid.events.ChooseRecipientsEvent
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Recipient
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.setupAsBackButton
import kotlinx.android.synthetic.main.toolbar_layout.*
import org.greenrobot.eventbus.EventBus
import java.util.*

class InboxRecipientsFragment : ParentFragment() {

    override fun title(): String = getString(R.string.selectRecipients)

    override fun allowBookmarking() = false

    override fun applyTheme() {
        (view?.findViewById<View>(R.id.menu_done) as? TextView)?.setTextColor(ThemePrefs.buttonColor)
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
    }

    private val adapter: InboxRecipientAdapter by lazy {
        InboxRecipientAdapter(
            context,
            arguments.getParcelable(Const.CANVAS_CONTEXT),
            arguments.getParcelableArrayList<Recipient>(RECIPIENT_LIST).toHashSet()
        )
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_inbox_recipients, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        configureRecyclerView(view, context, adapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView)
    }

    private fun setupToolbar() {
        toolbar.setupAsBackButton(this)
        toolbar.title = title()
        toolbar.inflateMenu(R.menu.menu_done_text)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_done -> {
                    // Send the recipient list back to the message
                    EventBus.getDefault().postSticky(ChooseRecipientsEvent(adapter.getRecipients(), null))
                    // Clear the back stack because we want to go back to the message, not the previous screen
                    adapter.clearBackStack()
                    activity.onBackPressed()
                }
            }
            false
        }
    }

    override fun handleBackPressed() = adapter.popBackStack()

    companion object {

        private const val RECIPIENT_LIST = "recipient_list"

        fun createBundle(canvasContext: CanvasContext, addedRecipients: List<Recipient>): Bundle {
            val bundle = Bundle()
            bundle.putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            bundle.putParcelableArrayList(RECIPIENT_LIST, ArrayList(addedRecipients))
            return bundle
        }
    }
}
