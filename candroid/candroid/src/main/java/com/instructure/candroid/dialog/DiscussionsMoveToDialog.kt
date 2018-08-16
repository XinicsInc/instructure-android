/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
package com.instructure.candroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.AppCompatRadioButton
import android.view.View
import android.widget.RadioGroup
import com.instructure.candroid.R
import com.instructure.candroid.adapter.DiscussionListRecyclerAdapter
import com.instructure.canvasapi2.models.DiscussionTopicHeader
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.dismissExisting
import kotlin.properties.Delegates

class DiscussionsMoveToDialog : AppCompatDialogFragment() {

    init {
        retainInstance = true
    }

    private var mMoveToCallback: (String) -> Unit by Delegates.notNull()

    companion object {
        val GROUP = "group"
        val DISCUSSION_TOPIC_HEADER = "discussionTopicHeader"

        @JvmStatic
        fun show(manager: FragmentManager, group: String, discussionTopicHeader: DiscussionTopicHeader, callback: (String) -> Unit) {
            manager.dismissExisting<DiscussionsMoveToDialog>()
            val dialog = DiscussionsMoveToDialog()
            val args = Bundle()
            args.putString(GROUP, group)
            args.putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            dialog.arguments = args
            dialog.mMoveToCallback = callback
            dialog.show(manager, DiscussionsMoveToDialog::class.java.simpleName)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(ContextThemeWrapper(activity, 0), R.layout.dialog_move_discussion_to, null)
        val radioGroup = view.findViewById<RadioGroup>(R.id.coursePages)
        val group = arguments.getString(GROUP)
        val discussion = arguments.getParcelable<DiscussionTopicHeader>(DISCUSSION_TOPIC_HEADER)

        when(group) {
            DiscussionListRecyclerAdapter.PINNED -> {
                setupRadioButton(view.findViewById(R.id.rb_closedOpenForComments),
                        if (discussion.isLocked) getString(R.string.utils_discussionsOpen)
                        else getString(R.string.utils_discussionsClosed),
                        true, DiscussionListRecyclerAdapter.CLOSED_FOR_COMMENTS)
                setupRadioButton(view.findViewById(R.id.rb_pinnedUnpinned),
                        getString(R.string.utils_discussionsUnpin), false, DiscussionListRecyclerAdapter.UNPINNED)
            }
            DiscussionListRecyclerAdapter.UNPINNED -> {
                setupRadioButton(view.findViewById(R.id.rb_closedOpenForComments),
                        getString(R.string.utils_discussionsClosed), true, DiscussionListRecyclerAdapter.CLOSED_FOR_COMMENTS)
                setupRadioButton(view.findViewById(R.id.rb_pinnedUnpinned),
                        getString(R.string.utils_discussionsPin), false, DiscussionListRecyclerAdapter.PINNED)
            }
            DiscussionListRecyclerAdapter.CLOSED_FOR_COMMENTS -> {
                setupRadioButton(view.findViewById(R.id.rb_closedOpenForComments),
                        getString(R.string.utils_discussionsOpen), true, DiscussionListRecyclerAdapter.CLOSED_FOR_COMMENTS)
                setupRadioButton(view.findViewById(R.id.rb_pinnedUnpinned),
                        getString(R.string.utils_discussionsPin), false, DiscussionListRecyclerAdapter.PINNED)
            }
        }

        setupRadioButton(view.findViewById(R.id.rb_delete),
                getString(R.string.delete), false, DiscussionListRecyclerAdapter.DELETE)

        val dialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle(activity.getString(R.string.utils_discussionsOptions))
                .setView(view)
                .setPositiveButton(activity.getString(android.R.string.ok), null)
                .setNegativeButton(activity.getString(R.string.cancel), { _, _ -> })
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AppCompatDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
            dialog.getButton(AppCompatDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
            dialog.getButton(AppCompatDialog.BUTTON_POSITIVE).setOnClickListener {
                val selected = radioGroup.findViewById<AppCompatRadioButton>(radioGroup.checkedRadioButtonId)
                if(selected.tag != null) {
                    mMoveToCallback(selected.tag as String)
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

    @SuppressLint("RestrictedApi")
    private fun setupRadioButton(radioButton: AppCompatRadioButton, text: String, isChecked: Boolean, group: String) {

        radioButton.text = text
        radioButton.tag = group
        radioButton.isChecked = isChecked

        if(group == DiscussionListRecyclerAdapter.DELETE) {
            val destructiveColor =  ContextCompat.getColor(context, R.color.destructive)
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                radioButton.supportButtonTintList = ViewStyler.makeColorStateListForRadioGroup(destructiveColor, destructiveColor)
                radioButton.setTextColor(destructiveColor)
            }
        } else {
            val unselectedColor = ContextCompat.getColor(activity, R.color.unselected_radio_color)
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                radioButton.supportButtonTintList = ViewStyler.makeColorStateListForRadioGroup(unselectedColor, ThemePrefs.brandColor)
            }
        }
    }

    override fun onDestroyView() {
        // Fix for rotation bug
        dialog?.let { if (retainInstance) it.setDismissMessage(null) }
        super.onDestroyView()
    }
}
