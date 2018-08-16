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
 */    package com.instructure.candroid.AnnotationComments

import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.instructure.annotations.AnnotationDialogs.AnnotationCommentDialog
import com.instructure.annotations.createCommentReplyAnnotation
import com.instructure.annotations.generateAnnotationId
import com.instructure.candroid.R
import com.instructure.candroid.fragment.ParentFragment
import com.instructure.candroid.view.StudentSubmissionView
import com.instructure.canvasapi2.managers.CanvaDocsManager
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotation
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_annotation_comment_list.*
import kotlinx.coroutines.experimental.Job
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus

class AnnotationCommentListFragment : ParentFragment() {

    private var annotations by ParcelableArrayListArg<CanvaDocAnnotation>()
    private var canvaDocId by StringArg()
    private var sessionId by StringArg()
    private var assigneeId by LongArg()
    private var canvaDocDomain by StringArg()

    private var recyclerAdapter: AnnotationCommentListRecyclerAdapter? = null

    private var sendCommentJob: Job? = null
    private var editCommentJob: Job? = null
    private var deleteCommentJob: Job? = null

    override fun title() = getString(R.string.comments)
    override fun allowBookmarking() = false
    override fun applyTheme() {
        toolbar.title = title()
        toolbar.setupAsCloseButton(this)
        ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK, false)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            layoutInflater.inflate(R.layout.fragment_annotation_comment_list, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recyclerAdapter = AnnotationCommentListRecyclerAdapter(context, { annotation, position ->
            AnnotationCommentDialog.getInstance(fragmentManager, annotation.contents ?: "", context.getString(R.string.editComment)) { cancelled, text ->
                if(!cancelled) {
                    annotation.contents = text
                    editComment(annotation, position)
                }
            }.show(fragmentManager, AnnotationCommentDialog::class.java.simpleName)
        }, { annotation, position ->
            val builder = AlertDialog.Builder(context)
            //we want to show a different title for the head annotation
            builder.setTitle(R.string.deleteComment)
            builder.setMessage(if(position == 0) R.string.deleteHeadCommentConfirmation else R.string.deleteCommentConfirmation)
            builder.setPositiveButton(getString(R.string.delete).toUpperCase(), { _, _ ->
                deleteComment(annotation, position)
            })
            builder.setNegativeButton(getString(R.string.cancel).toUpperCase(), null)
            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.getButton(AppCompatDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
                dialog.getButton(AppCompatDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
            }
            dialog.show()
        })

        configureRecyclerView()
        applyTheme()
        setupCommentInput()

        if(recyclerAdapter?.size() == 0) {
            recyclerAdapter?.addAll(annotations)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sendCommentJob?.cancel()
        editCommentJob?.cancel()
        deleteCommentJob?.cancel()
    }

    fun configureRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        annotationCommentsRecyclerView.layoutManager = layoutManager
        annotationCommentsRecyclerView.itemAnimator = DefaultItemAnimator()
        annotationCommentsRecyclerView.adapter = recyclerAdapter
    }

    private fun setupCommentInput() {
        sendCommentButton.imageTintList = ViewStyler.generateColorStateList(
                intArrayOf(-android.R.attr.state_enabled) to ContextCompat.getColor(context, R.color.defaultTextGray),
                intArrayOf() to ThemePrefs.buttonColor
        )

        sendCommentButton.isEnabled = false
        commentEditText.onTextChanged { sendCommentButton.isEnabled = it.isNotBlank() }
        sendCommentButton.onClickWithRequireNetwork {
            sendComment(commentEditText.text.toString())
        }
    }

    private fun showSendingStatus() {
        sendCommentButton.setInvisible()
        sendingProgressBar.setVisible()
        sendingProgressBar.announceForAccessibility(getString(R.string.sendingSimple))
        sendingErrorTextView.setGone()
        commentEditText.isEnabled = false
    }

    private fun hideSendingStatus(success: Boolean) {
        sendingProgressBar.setGone()
        sendCommentButton.setVisible()
        commentEditText.isEnabled = true
        if (success) {
            commentEditText.setText("")
            commentEditText.hideKeyboard()
        } else {
            sendingErrorTextView.setVisible()
        }
    }


    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun sendComment(comment: String) {
        sendCommentJob = weave {
            try {
                showSendingStatus()
                //first we need to find the head comment
                val headAnnotation = annotations.firstOrNull()
                if (headAnnotation != null) {
                    val newCommentReply = awaitApi<CanvaDocAnnotation> { CanvaDocsManager.putAnnotation(sessionId, generateAnnotationId(), createCommentReplyAnnotation(comment, headAnnotation.annotationId, canvaDocId, ApiPrefs.user?.id.toString(), headAnnotation.page), canvaDocDomain, it) }
                    EventBus.getDefault().post(StudentSubmissionView.AnnotationCommentAdded(newCommentReply, assigneeId))
                    // The put request doesn't return this property, so we need to set it to true
                    newCommentReply.isEditable = true
                    recyclerAdapter?.add(newCommentReply) //ALSO, add it to the UI
                    hideSendingStatus(true)
                } else {
                    hideSendingStatus(false)
                }
            } catch (e: Throwable) {
                hideSendingStatus(false)
            }
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun editComment(annotation: CanvaDocAnnotation, position: Int) {
        editCommentJob = tryWeave {
            awaitApi<CanvaDocAnnotation> { CanvaDocsManager.putAnnotation(sessionId, annotation.annotationId, annotation, canvaDocDomain, it) }
            EventBus.getDefault().post(StudentSubmissionView.AnnotationCommentEdited(annotation, assigneeId))
            // Update the UI
            recyclerAdapter?.add(annotation)
            recyclerAdapter?.notifyItemChanged(position)
        } catch {
            hideSendingStatus(false)
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun deleteComment(annotation: CanvaDocAnnotation, position: Int) {
        deleteCommentJob = tryWeave {
            awaitApi<ResponseBody> { CanvaDocsManager.deleteAnnotation(sessionId, annotation.annotationId, canvaDocDomain, it) }
            if(annotation.annotationId == annotations.firstOrNull()?.annotationId) {
                //this is the head annotation, deleting this deletes the entire thread
                EventBus.getDefault().post(StudentSubmissionView.AnnotationCommentDeleted(annotation, true, assigneeId))
                headAnnotationDeleted()
            } else {
                EventBus.getDefault().post(StudentSubmissionView.AnnotationCommentDeleted(annotation, false, assigneeId))
                recyclerAdapter?.remove(annotation)
                recyclerAdapter?.notifyItemChanged(position)
            }
        } catch {
            hideSendingStatus(false)
        }
    }

    private fun headAnnotationDeleted() {
        activity.onBackPressed()
    }

    companion object {
        @JvmStatic val ANNOTATIONS = "annotations"
        @JvmStatic val CANVADOC_ID = "canvaDocId"
        @JvmStatic val SESSION_ID = "sessionId"
        @JvmStatic val ASSIGNEE_ID = "assigneeId"
        @JvmStatic val CANVADOCS_DOMAIN = "canvaDocDomain"

        @JvmStatic
        fun newInstance(bundle: Bundle) = AnnotationCommentListFragment().apply { arguments = bundle }

        @JvmStatic
        fun makeBundle(annotations: ArrayList<CanvaDocAnnotation>, canvaDocId: String, sessionId: String, canvaDocsDomain: String, assigneeId: Long): Bundle {
            val args = Bundle()
            args.putParcelableArrayList(ANNOTATIONS, annotations)
            args.putString(CANVADOC_ID, canvaDocId)
            args.putString(SESSION_ID, sessionId)
            args.putLong(ASSIGNEE_ID, assigneeId)
            args.putString(CANVADOCS_DOMAIN, canvaDocsDomain)
            return args
        }
    }
}