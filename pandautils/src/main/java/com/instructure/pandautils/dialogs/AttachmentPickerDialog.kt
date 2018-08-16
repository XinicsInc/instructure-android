/*
 * Copyright (C) 2018 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.instructure.pandautils.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.instructure.canvasapi2.models.Attachment
import com.instructure.pandautils.R
import com.instructure.pandautils.utils.dismissExisting
import kotlin.properties.Delegates

class AttachmentPickerDialog : AppCompatDialogFragment() {

    init {
        retainInstance = true
    }

    private var mAttachmentRecyclerView: RecyclerView? = null
    private var mSelectionCallback: (Attachment) -> Unit by Delegates.notNull()

    companion object {
        val ATTACHMENT_LIST = "attachmentList"

        @JvmStatic
        fun show(manager: FragmentManager, attachments: ArrayList<Attachment>, callback: (Attachment) -> Unit) {
            manager.dismissExisting<AttachmentPickerDialog>()
            val dialog = AttachmentPickerDialog()
            val args = Bundle()
            args.putParcelableArrayList(ATTACHMENT_LIST, attachments)
            dialog.arguments = args
            dialog.mSelectionCallback = callback
            dialog.show(manager, AttachmentPickerDialog::class.java.simpleName)
        }

        @JvmStatic
        fun hide(manager: FragmentManager) {
            manager.dismissExisting<AttachmentPickerDialog>()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(ContextThemeWrapper(activity, 0), R.layout.dialog_attachment_picker, null)
        mAttachmentRecyclerView = view.findViewById<RecyclerView>(R.id.attachmentRecyclerView)
        val dialog = AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle(activity.getString(R.string.utils_attachments))
                .setView(view)
                .create()

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val attachments = arguments.getParcelableArrayList<Attachment>(ATTACHMENT_LIST)
        if(attachments.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }

        mAttachmentRecyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mAttachmentRecyclerView?.adapter = AttachmentRecyclerViewAdapter(attachments)
    }

    inner class AttachmentRecyclerViewAdapter(var attachments: ArrayList<Attachment>) : RecyclerView.Adapter<AttachmentViewHolder>() {

        override fun onBindViewHolder(holder: AttachmentViewHolder?, position: Int) {
            holder?.bind(attachments[position], mSelectionCallback)
        }

        override fun getItemCount(): Int {
            return attachments.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AttachmentViewHolder {
            val v = LayoutInflater.from(context).inflate(AttachmentViewHolder.holderResId, parent, false)
            return AttachmentViewHolder(v)
        }
    }

    class AttachmentViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        private var attachmentText = view.findViewById<TextView>(R.id.attachmentTitle)

        fun bind(attachment: Attachment, callback: (Attachment) -> Unit) = with(itemView) {
            attachmentText.text = attachment.displayName ?: attachment.filename
            attachmentText.setOnClickListener {
                callback.invoke(attachment)
            }
        }

        companion object {
            val holderResId = R.layout.adapter_attachment_layout
        }
    }
}

