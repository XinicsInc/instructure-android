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
package com.instructure.teacher.PSPDFKit.AnnotationComments

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.annotations.AnnotationDialogs.AnnotationCommentDialog
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotation
import com.instructure.pandautils.fragments.BaseListFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.utils.*
import kotlinx.android.synthetic.main.fragment_annotation_comment_list.*

class AnnotationCommentListFragment : BaseListFragment<
        CanvaDocAnnotation,
        AnnotationCommentListPresenter,
        AnnotationCommentListView,
        AnnotationCommentViewHolder,
        AnnotationCommentListAdapter>(), AnnotationCommentListView {

    private var mAnnotationList by ParcelableArrayListArg<CanvaDocAnnotation>()
    private var mCanvaDocId by StringArg()
    private var mSessionId by StringArg()
    private var mAssigneeId by LongArg()
    private var mCanvaDocsDomain by StringArg()
    private val mAnnotationCommentsAdapter by lazy {
        AnnotationCommentListAdapter(context, presenter, { annotation, position ->
            AnnotationCommentDialog.getInstance(fragmentManager, annotation.contents ?: "", context.getString(R.string.editComment)) { cancelled, text ->
                if(!cancelled) {
                    annotation.contents = text
                    presenter.editComment(annotation, position)
                }
            }.show(fragmentManager, AnnotationCommentDialog::class.java.simpleName)
        }, { annotation, position ->
            val builder = AlertDialog.Builder(context)
            //we want to show a different title for the head annotation
            builder.setTitle(if(position == 0) R.string.deleteAnnotation else R.string.deleteComment)
            builder.setMessage(if(position == 0) R.string.deleteHeadCommentConfirmation else R.string.deleteCommentConfirmation)
            builder.setPositiveButton(getString(R.string.delete).toUpperCase(), { _, _ ->
                presenter.deleteComment(annotation, position)
            })
            builder.setNegativeButton(getString(R.string.cancel).toUpperCase(), null)
            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.getButton(AppCompatDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
                dialog.getButton(AppCompatDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
            }
            dialog.show()
        })
    }

    override fun getAdapter() = mAnnotationCommentsAdapter
    override fun getRecyclerView(): RecyclerView = annotationCommentsRecyclerView
    override fun getList() = presenter.data
    override fun layoutResId() = R.layout.fragment_annotation_comment_list
    override fun onCreateView(view: View?) {}
    override fun checkIfEmpty() {} // we don't display this view if its empty, so no need to check
    override fun onRefreshFinished() {}
    override fun onRefreshStarted() {}
    override fun getPresenterFactory() = AnnotationCommentListPresenterFactory(mAnnotationList, mCanvaDocId, mSessionId, mCanvaDocsDomain, mAssigneeId)

    override fun onPresenterPrepared(presenter: AnnotationCommentListPresenter?) {
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = adapter
    }

    override fun onReadySetGo(presenter: AnnotationCommentListPresenter) {
        setupToolbar()
        presenter.loadData(false)
        setupCommentInput()
    }

    fun setupToolbar() {
        toolbar.title = getString(R.string.sg_tab_comments)
        toolbar.setupBackButton(this)
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
    }

    fun setupCommentInput() {
        sendCommentButton.imageTintList = ViewStyler.generateColorStateList(
                intArrayOf(-android.R.attr.state_enabled) to context.getColorCompat(R.color.defaultTextGray),
                intArrayOf() to ThemePrefs.buttonColor
        )

        sendCommentButton.isEnabled = false
        commentEditText.onTextChanged { sendCommentButton.isEnabled = it.isNotBlank() }
        sendCommentButton.onClickWithRequireNetwork {
            presenter.sendComment(commentEditText.text.toString())
        }
    }

    override fun showSendingStatus() {
        sendCommentButton.setInvisible()
        sendingProgressBar.setVisible()
        sendingProgressBar.announceForAccessibility(getString(R.string.sendingSimple))
        sendingErrorTextView.setGone()
        commentEditText.isEnabled = false
    }

    override fun hideSendingStatus(success: Boolean) {
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

    override fun notifyItemChanged(position: Int) {
        adapter.notifyItemChanged(position)
    }

    override fun headAnnotationDeleted() {
        activity.onBackPressed()
    }

    companion object {
        @JvmStatic val ANNOTATIONS = "mAnnotationList"
        @JvmStatic val CANVADOC_ID = "mCanvaDocId"
        @JvmStatic val SESSION_ID = "mSessionId"
        @JvmStatic val ASSIGNEE_ID = "mAssigneeId"
        @JvmStatic val CANVADOCS_DOMAIN = "mCanvaDocsDomain"

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
