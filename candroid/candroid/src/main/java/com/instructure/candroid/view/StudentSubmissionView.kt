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
package com.instructure.candroid.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.instructure.annotations.*
import com.instructure.annotations.AnnotationDialogs.AnnotationCommentDialog
import com.instructure.annotations.AnnotationDialogs.AnnotationErrorDialog
import com.instructure.annotations.AnnotationDialogs.FreeTextDialog
import com.instructure.annotations.FileCaching.DocumentListenerSimpleDelegate
import com.instructure.candroid.AnnotationComments.AnnotationCommentListFragment
import com.instructure.candroid.R
import com.instructure.candroid.activity.StudentSubmissionActivity
import com.instructure.canvasapi2.managers.CanvaDocsManager
import com.instructure.canvasapi2.managers.SubmissionManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotation
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotationResponse
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.loginapi.login.dialog.NoInternetConnectionDialog
import com.instructure.pandautils.utils.*
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationProvider
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.defaults.*
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.events.Commands
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.inspector.annotation.AnnotationCreationInspectorController
import com.pspdfkit.ui.inspector.annotation.AnnotationEditingInspectorController
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationCreationInspectorController
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationEditingInspectorController
import com.pspdfkit.ui.special_mode.controller.AnnotationCreationController
import com.pspdfkit.ui.special_mode.controller.AnnotationEditingController
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.ui.special_mode.manager.AnnotationManager
import com.pspdfkit.ui.toolbar.*
import kotlinx.android.synthetic.main.view_student_submission.view.*
import kotlinx.coroutines.experimental.Job
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.File
import java.util.*

@SuppressLint("ViewConstructor")
class StudentSubmissionView(
        context: Context,
        private val course: Course,
        private val studentSubmission: GradeableStudentSubmission,
        private val assignment: Assignment
) : FrameLayout(context), AnnotationManager.OnAnnotationCreationModeChangeListener, AnnotationManager.OnAnnotationEditingModeChangeListener {

    private val assignee: Assignee get() = studentSubmission.assignee
    private val rootSubmission: Submission? get() = studentSubmission.submission

    private var initJob: Job? = null

    //region pspdfkit stuff
    private val mPdfConfiguration: PdfConfiguration = PdfConfiguration.Builder()
            .scrollDirection(PageScrollDirection.VERTICAL)
            .enabledAnnotationTools(setupAnnotationCreationList())
            .editableAnnotationTypes(setupAnnotationEditList())
            .setAnnotationInspectorEnabled(true)
            .textSharingEnabled(false)
            .layoutMode(PageLayoutMode.SINGLE)
            .textSelectionEnabled(false)
            .disableCopyPaste()
            .build()
    private val mAnnotationCreationToolbar = AnnotationCreationToolbar(context)
    private val mAnnotationEditingToolbar = AnnotationEditingToolbar(context)
    private var mAnnotationEditingInspectorController: AnnotationEditingInspectorController? = null
    private var mAnnotationCreationInspectorController: AnnotationCreationInspectorController? = null
    private var mPdfFragment: PdfFragment? = null
    private var mFileJob: Job? = null
    private var mCreateAnnotationJob: Job? = null
    private var mUpdateAnnotationJob: Job? = null
    private var mDeleteAnnotationJob: Job? = null
    private var sendCommentJob: Job? = null
    private var mPdfContentJob: Job? = null
    private var mAnnotationsJob: Job? = null
    private var mSessionId: String? = null
    private var mCanvaDocId: String? = null
    private var mCanvaDocsDomain: String? = null
    private val mCommentRepliesHashMap: HashMap<String, ArrayList<CanvaDocAnnotation>> = HashMap()
    private var mCurrentAnnotationModeTool: AnnotationTool? = null
    private var mCurrentAnnotationModeType: AnnotationType? = null
    private var isUpdatingWithNoNetwork = false
    //endregion

    private val supportFragmentManager = (context as AppCompatActivity).supportFragmentManager

    // region Annotation Listeners
    val mAnnotationUpdateListener = object: AnnotationProvider.OnAnnotationUpdatedListener {
        override fun onAnnotationCreated(annotation: Annotation) {
            if(!annotation.isAttached || annotationNetworkCheck(annotation)) return

            // If it's a freetext and it's empty that means that they haven't had a chance to fill it out
            if((annotation.type == AnnotationType.FREETEXT || annotation.type == AnnotationType.NOTE) && annotation.contents.isNullOrEmpty()){
                return
            }
            createNewAnnotation(annotation)
        }

        override fun onAnnotationUpdated(annotation: Annotation) {
            if(!annotation.isAttached || annotationNetworkCheck(annotation)) return

            //Note is a special edge case and can't be created safely until it has contents
            if(annotation.type == AnnotationType.NOTE && annotation.contents.isValid() && annotation.name.isNullOrEmpty()) {
                createNewAnnotation(annotation)
                return
            }

            if (!annotation.flags.contains(AnnotationFlags.LOCKED) && annotation.isModified && annotation.name.isValid()) {
                //we only want to update the annotation if it isn't locked and IS modified
                updateAnnotation(annotation)
            }
        }

        override fun onAnnotationRemoved(annotation: Annotation) {
            if(annotationNetworkCheck(annotation)) return

            //removed annotation
            if(annotation.name.isValid()) {
                deleteAnnotation(annotation)
            }
        }
    }

    private fun annotationNetworkCheck(annotation: Annotation): Boolean {
        if(!APIHelper.hasNetworkConnection()) {
            if(isUpdatingWithNoNetwork) {
                isUpdatingWithNoNetwork = false
                return true
            } else {
                isUpdatingWithNoNetwork = true
                if(annotation.isAttached) {
                    mPdfFragment?.eventBus?.post(Commands.ClearSelectedAnnotations())
                    mPdfFragment?.document?.annotationProvider?.removeAnnotationFromPage(annotation)
                    mPdfFragment?.notifyAnnotationHasChanged(annotation)
                }
                NoInternetConnectionDialog.show(supportFragmentManager)
            }
        }
        return false
    }

    val mAnnotationSelectedListener = object: AnnotationManager.OnAnnotationSelectedListener {
        override fun onAnnotationSelected(annotation: Annotation, isCreated: Boolean) {}
        override fun onPrepareAnnotationSelection(p0: AnnotationSelectionController, annotation: Annotation, isCreated: Boolean): Boolean {
            if (APIHelper.hasNetworkConnection()) {
                if (annotation.type == AnnotationType.FREETEXT && annotation.name.isNullOrEmpty()) {
                    //this is a new free text annotation, and needs to be selected to be created
                    val dialog = FreeTextDialog.getInstance(supportFragmentManager, "", freeTextDialogCallback)
                    dialog.show(supportFragmentManager, FreeTextDialog::class.java.simpleName)
                }
            }

            if(annotation.type != AnnotationType.FREETEXT && annotation.name.isValid()) {
                if (annotation.contents.isNullOrEmpty() && annotation.flags.contains(AnnotationFlags.LOCKED)) {
                    //edge case for empty content annotation from non-user author
                    commentsButton.setGone()
                } else {
                    // if the annotation is an existing annotation (has an ID) and is NOT freetext
                    // we want to display the button to view/make comments
                    commentsButton.setVisible()
                }
            }

            return true
        }
    }

    val mAnnotationDeselectedListener = AnnotationManager.OnAnnotationDeselectedListener { _, _->
        commentsButton.setGone()
    }
    //endregion

    //region Annotation Manipulation
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun createNewAnnotation(annotation: Annotation) {
        // This is a new annotation; Post it
        val canvaDocId = mCanvaDocId ?: return
        val sessionId = mSessionId ?: return
        val canvaDocsDomain = mCanvaDocsDomain ?: return

        commentsButton.isEnabled = false

        mCreateAnnotationJob = tryWeave {
            val canvaDocAnnotation = annotation.convertPDFAnnotationToCanvaDoc(canvaDocId)
            if (canvaDocAnnotation != null) {

                // If this is a note annotation (map pin thing) we need to nuke its contents, and create a separate comment reply annotation.
                var commentReply = ""
                if(canvaDocAnnotation.annotationType == CanvaDocAnnotation.AnnotationType.TEXT) {
                    commentReply = canvaDocAnnotation.contents ?: ""
                    canvaDocAnnotation.contents = null
                }

                //store the response
                val newAnnotation = awaitApi<CanvaDocAnnotation> { CanvaDocsManager.putAnnotation(sessionId, generateAnnotationId(), canvaDocAnnotation, canvaDocsDomain, it) }

                if(commentReply.isValid()) {
                    // Now to create a new commentReply annotation for this..
                    createCommentAnnotation(newAnnotation.annotationId, newAnnotation.page, commentReply)
                    commentsButton.setVisible()
                }

                // Edit the annotation with the appropriate id
                annotation.name = newAnnotation.annotationId
                mPdfFragment?.document?.annotationProvider?.removeOnAnnotationUpdatedListener(mAnnotationUpdateListener)
                mPdfFragment?.notifyAnnotationHasChanged(annotation)
                mPdfFragment?.document?.annotationProvider?.addOnAnnotationUpdatedListener(mAnnotationUpdateListener)
                commentsButton.isEnabled = true
            }
        } catch {
            // Show general error, make more specific in the future?
            toast(R.string.errorOccurred)
            it.printStackTrace()
            commentsButton.isEnabled = true
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun updateAnnotation(annotation: Annotation) {
        // Annotation modified; Update it
        val canvaDocId = mCanvaDocId ?: return
        val sessionId = mSessionId ?: return
        val canvaDocsDomain = mCanvaDocsDomain ?: return

        mUpdateAnnotationJob = tryWeave {
            val canvaDocAnnotation = annotation.convertPDFAnnotationToCanvaDoc(canvaDocId)
            if (canvaDocAnnotation != null && !annotation.name.isNullOrEmpty()) {
                if (annotation.type == AnnotationType.NOTE) {
                    // This is a rough edge case. We need to strip the note of its contents, update it, and also update the commentReply if its contents have changed...
                    val noteContents = annotation.contents

                    // If the head comment has been changed we need to update it
                    mCommentRepliesHashMap[annotation.name]?.firstOrNull()?.let { headComment ->
                        if (noteContents != headComment.contents) {
                            val commentReply = awaitApi<CanvaDocAnnotation> { CanvaDocsManager.putAnnotation(sessionId, headComment.annotationId, headComment, canvaDocsDomain, it) }
                            mCommentRepliesHashMap[annotation.name]?.firstOrNull()?.contents = commentReply.contents
                        }
                    }
                }

                awaitApi<CanvaDocAnnotation> { CanvaDocsManager.putAnnotation(sessionId, annotation.name!!, canvaDocAnnotation, canvaDocsDomain, it) }
            }
        } catch {
            if(it is StatusCallbackError) {
                if (it.response?.raw()?.code() == 404) {
                    // Not found; Annotation has been deleted and no longer exists.
                    val dialog = AnnotationErrorDialog.getInstance(supportFragmentManager, {
                        // Delete annotation after user clicks OK on dialog
                        mPdfFragment?.eventBus?.post(Commands.ClearSelectedAnnotations())
                        mPdfFragment?.document?.annotationProvider?.removeAnnotationFromPage(annotation)
                        mPdfFragment?.notifyAnnotationHasChanged(annotation)
                    })
                    dialog.show(supportFragmentManager, AnnotationErrorDialog::class.java.simpleName)
                }
            }

            // Show general error, make more specific in the future?
            toast(R.string.errorOccurred)

            it.printStackTrace()
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun deleteAnnotation(annotation: Annotation) {
        // Annotation deleted; DELETE
        val sessionId = mSessionId ?: return
        val canvaDocsDomain = mCanvaDocsDomain ?: return

        mDeleteAnnotationJob = tryWeave {
            // If it is not found, don't hit the server (it will fail)
            if (!annotation.name.isNullOrEmpty())
                awaitApi<ResponseBody> { CanvaDocsManager.deleteAnnotation(sessionId, annotation.name!!, canvaDocsDomain, it) }
        } catch {
            // Show general error, make more specific in the future?
            toast(R.string.errorOccurred)
            it.printStackTrace()
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun createCommentAnnotation(inReplyToId: String, page: Int, comment: String?) {
        // Annotation modified; Update it
        val canvaDocId = mCanvaDocId ?: return
        val sessionId = mSessionId ?: return
        val canvaDocsDomain = mCanvaDocsDomain ?: return

        commentsButton.isEnabled = false

        sendCommentJob = tryWeave {
            val newCommentReply = awaitApi<CanvaDocAnnotation> {
                CanvaDocsManager.putAnnotation(sessionId, generateAnnotationId(), createCommentReplyAnnotation(comment ?: "", inReplyToId, canvaDocId, ApiPrefs.user?.id.toString(), page), canvaDocsDomain, it)
            }

            // The put request doesn't return this property, so we need to set it to true
            newCommentReply.isEditable = true
            mCommentRepliesHashMap[inReplyToId] = arrayListOf(newCommentReply)
            commentsButton.isEnabled = true
        } catch {
            // Show general error, make more specific in the future?
            toast(R.string.errorOccurred)
            it.printStackTrace()
            commentsButton.isEnabled = true
        }
    }

    //endregion

    init {
        View.inflate(context, R.layout.view_student_submission, this)

        setLoading(true)

        mAnnotationEditingInspectorController = DefaultAnnotationEditingInspectorController(context, inspectorCoordinatorLayout)
        mAnnotationCreationInspectorController = DefaultAnnotationCreationInspectorController(context, inspectorCoordinatorLayout)

        annotationToolbarLayout.setOnContextualToolbarLifecycleListener(object : ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener{
            override fun onDisplayContextualToolbar(p0: ContextualToolbar<*>) {}
            override fun onRemoveContextualToolbar(p0: ContextualToolbar<*>) {}

            override fun onPrepareContextualToolbar(toolbar: ContextualToolbar<*>) {
                toolbar.layoutParams = ToolbarCoordinatorLayout.LayoutParams(
                        ToolbarCoordinatorLayout.LayoutParams.Position.TOP, EnumSet.of(ToolbarCoordinatorLayout.LayoutParams.Position.TOP)
                )
            }
        })

        mAnnotationCreationToolbar.closeButton.setGone()

        mAnnotationCreationToolbar.setMenuItemGroupingRule { mutableList, i ->
            return@setMenuItemGroupingRule configureCreationMenuItemGrouping(mutableList, i)
        }

        mAnnotationEditingToolbar.setMenuItemGroupingRule { mutableList, _ ->
            return@setMenuItemGroupingRule configureEditMenuItemGrouping(mutableList)
        }

        mAnnotationEditingToolbar.setOnMenuItemClickListener { _, contextualToolbarMenuItem ->
            if (contextualToolbarMenuItem.title == context.getString(com.pspdfkit.R.string.pspdf__edit) &&
                    mCurrentAnnotationModeType == AnnotationType.FREETEXT) {

                val dialog = FreeTextDialog.getInstance(supportFragmentManager, mPdfFragment?.selectedAnnotations?.get(0)?.contents ?: "", freeTextDialogCallback)
                dialog.show(supportFragmentManager, FreeTextDialog::class.java.simpleName)

                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }

        configureCommentView()
    }

    private fun configureCommentView() {
        //we want to offset the comment button by the height of the action bar
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)
        val typedArray = context.obtainStyledAttributes(typedValue.resourceId, intArrayOf(android.R.attr.actionBarSize))
        val actionBarDp = typedArray.getDimensionPixelSize(0, -1)
        typedArray.recycle()

        val marginDp = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 12f, context.resources.displayMetrics)
        val layoutParams = commentsButton.layoutParams as LayoutParams
        commentsButton.drawable.setTint(Color.WHITE)
        layoutParams.gravity = Gravity.END or Gravity.TOP
        layoutParams.topMargin = marginDp.toInt() + actionBarDp
        layoutParams.rightMargin = marginDp.toInt()

        commentsButton.onClick {
            val canvaDocId = mCanvaDocId ?: return@onClick
            val sessionId = mSessionId ?: return@onClick
            val canvaDocsDomain = mCanvaDocsDomain ?: return@onClick
            //get current annotation in both forms
            val currentPdfAnnotation = mPdfFragment?.selectedAnnotations?.get(0)
            val currentAnnotation = currentPdfAnnotation?.convertPDFAnnotationToCanvaDoc(canvaDocId)
            //assuming neither is null, continue
            if(currentPdfAnnotation != null && currentAnnotation != null) {
                //if the contents of the current annotation are empty we want to prompt them to add a comment
                if(mCommentRepliesHashMap[currentAnnotation.annotationId] == null || mCommentRepliesHashMap[currentAnnotation.annotationId]?.isEmpty() == true) {
                    // No comments for this annotation, show a dialog for the user to add some if they want
                    AnnotationCommentDialog.getInstance(supportFragmentManager, "", context.getString(R.string.addAnnotationComment)) { _, text ->
                        // Create new comment reply for this annotation.
                        if(text.isValid()) {
                            createCommentAnnotation(currentAnnotation.annotationId, currentAnnotation.page, text)
                        }
                    }.show(supportFragmentManager, AnnotationCommentDialog::class.java.simpleName)
                } else {
                    //otherwise, show the comment list fragment
                    mCommentRepliesHashMap[currentAnnotation.annotationId]?.let {
                        if(!it.isEmpty()) {
                            val bundle = AnnotationCommentListFragment.makeBundle(it, canvaDocId, sessionId, canvaDocsDomain, assignee.id)
                            val fragment = AnnotationCommentListFragment.newInstance(bundle)
                            if(isAttachedToWindow) {
                                val ft = supportFragmentManager.beginTransaction()
                                ft.add(R.id.annotationCommentsContainer, fragment, fragment::class.java.name)
                                ft.addToBackStack(fragment::class.java.name)
                                ft.commit()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        loadingView?.setVisible(isLoading)
        contentRoot?.setVisible(!isLoading)
    }

    private fun setupAnnotationCreationList(): MutableList<AnnotationTool> {
        return listOf(AnnotationTool.INK, AnnotationTool.HIGHLIGHT, AnnotationTool.STRIKEOUT, AnnotationTool.SQUARE, AnnotationTool.NOTE, AnnotationTool.FREETEXT).toMutableList()
    }

    private fun setupAnnotationEditList(): MutableList<AnnotationType> {
        return listOf(AnnotationType.INK, AnnotationType.HIGHLIGHT, AnnotationType.STRIKEOUT, AnnotationType.SQUARE, AnnotationType.NOTE, AnnotationType.FREETEXT).toMutableList()
    }

    private fun configureCreationMenuItemGrouping(toolbarMenuItems: MutableList<ContextualToolbarMenuItem>, capacity: Int) : MutableList<ContextualToolbarMenuItem> {
        //There are 7 items total, and always need to leave room for the color, it has to show.
        //First we need to get all of the items and store them in variables for readability.... rip
        var freeText: ContextualToolbarMenuItem? = null
        var note: ContextualToolbarMenuItem? = null
        var strikeOut: ContextualToolbarMenuItem? = null
        var highlight: ContextualToolbarMenuItem? = null
        var ink: ContextualToolbarMenuItem? = null
        var rectangle: ContextualToolbarMenuItem? = null
        var color: ContextualToolbarMenuItem? = null

        for(item in toolbarMenuItems) {
            when(item.title) {
                context.getString(com.pspdfkit.R.string.pspdf__annotation_type_freetext) -> {
                    freeText = item
                }
                context.getString(com.pspdfkit.R.string.pspdf__annotation_type_note) -> {
                    note = item
                }
                context.getString(com.pspdfkit.R.string.pspdf__annotation_type_strikeout) -> {
                    strikeOut = item
                }
                context.getString(com.pspdfkit.R.string.pspdf__annotation_type_highlight) -> {
                    highlight = item
                }
                context.getString(com.pspdfkit.R.string.pspdf__annotation_type_ink) -> {
                    ink = item
                }
                context.getString(com.pspdfkit.R.string.pspdf__annotation_type_square) -> {
                    rectangle = item
                }
                context.getString(com.pspdfkit.R.string.pspdf__edit_menu_color) -> {
                    color = item
                }
            }
        }

        //check to make sure we have all of our items
        if(freeText != null && note != null && strikeOut != null && highlight != null
                && ink != null && rectangle != null && color != null) {
            when {
                capacity >= 6 -> return mutableListOf(note, highlight, freeText, strikeOut, ink, rectangle, color)
                capacity == 5 -> {
                    val inkGroup = ContextualToolbarMenuItem.createGroupItem(View.generateViewId(), ink.position, true, mutableListOf(ink, rectangle), ink)
                    return mutableListOf(note, highlight, freeText, strikeOut, inkGroup, color)
                }
                capacity == 4 -> {
                    val inkGroup = ContextualToolbarMenuItem.createGroupItem(View.generateViewId(), ink.position, true, mutableListOf(ink, rectangle), ink)
                    val highlightGroup = ContextualToolbarMenuItem.createGroupItem(View.generateViewId(), highlight.position, true, mutableListOf(highlight, strikeOut), highlight)
                    return mutableListOf(note, highlightGroup, freeText, inkGroup, color)
                }
                capacity == 3 -> {
                    val inkGroup = ContextualToolbarMenuItem.createGroupItem(View.generateViewId(), ink.position, true, mutableListOf(ink, rectangle), ink)
                    val highlightGroup = ContextualToolbarMenuItem.createGroupItem(View.generateViewId(), highlight.position, true, mutableListOf(highlight, strikeOut), highlight)
                    val freeTextGroup = ContextualToolbarMenuItem.createGroupItem(View.generateViewId(), freeText.position, true, mutableListOf(freeText, note), freeText)
                    return mutableListOf(note, highlightGroup, freeTextGroup, inkGroup, color)
                }
            //if all else fails, return default grouping unchanged
                else -> {
                    return toolbarMenuItems
                }
            }
        } else {
            //if we dont have all items, just return the default that we have
            return toolbarMenuItems
        }
    }


    private fun configureEditMenuItemGrouping(toolbarMenuItems: MutableList<ContextualToolbarMenuItem>): MutableList<ContextualToolbarMenuItem> {
        //if current tool == freeText add edit button
        //There are 7 items total, and always need to leave room for the color, it has to show.
        //First we need to get all of the items and store them in variables for readability.... rip
        var delete: ContextualToolbarMenuItem? = null
        var color: ContextualToolbarMenuItem? = null

        val edit: ContextualToolbarMenuItem? = if (mCurrentAnnotationModeType ?: AnnotationType.NONE == AnnotationType.FREETEXT) {
            ContextualToolbarMenuItem.createSingleItem(context, View.generateViewId(),
                    context.getDrawable(com.pspdfkit.R.drawable.pspdf__ic_edit),
                    context.getString(com.pspdfkit.R.string.pspdf__edit), -1, -1,
                    ContextualToolbarMenuItem.Position.END, false)
        } else null

        for (item in toolbarMenuItems) {
            when (item.title) {
                context.getString(com.pspdfkit.R.string.pspdf__edit_menu_color) -> {
                    color = item
                }
                context.getString(com.pspdfkit.R.string.pspdf__delete) -> {
                    delete = item
                }
            }
        }

        var list = mutableListOf<ContextualToolbarMenuItem>()
        //check to make sure we have all of our items

        if (color != null && delete != null) {
            if (edit != null)
                list.add(edit)
            list.add(color)
            list.add(delete)
        } else {
            // If we don't have all items, just return the default that we have
            list = if (mCurrentAnnotationModeType ?: AnnotationType.NONE == AnnotationType.NOTE) {
                // Edge case for NOTE annotations, we don't want them editing the contents using the pspdfkit ui
                toolbarMenuItems.filter { it.title != context.getString(R.string.pspdf__edit) }.toMutableList()
            } else {
                toolbarMenuItems
            }
        }

        return list
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupToolbar()
        obtainSubmissionData()
    }

    private fun setupToolbar() {
        studentSubmissionToolbar.setupAsBackButton {
            (context as? Activity)?.finish()
        }

        titleTextView.text = rootSubmission?.attachments?.firstOrNull()?.filename ?: ""
        if(rootSubmission?.submittedAt != null) subtitleTextView.text = DateHelper.getDateTimeString(context, rootSubmission?.submittedAt) ?: ""
        ViewStyler.colorToolbarIconsAndText(context as Activity, studentSubmissionToolbar, Color.BLACK)
        ViewStyler.setStatusBarLight(context as Activity)
        ViewStyler.setToolbarElevationSmall(context, studentSubmissionToolbar)
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun obtainSubmissionData() {
        initJob = tryWeave {
            if (!studentSubmission.isCached) {
                studentSubmission.submission = awaitApi { SubmissionManager.getSingleSubmission(course.id, assignment.id, studentSubmission.assigneeId, it, true) }
                studentSubmission.isCached = true
            }
            setup()
        } catch {
            loadingView.setGone()
            retryLoadingContainer.setVisible()
            retryLoadingButton.onClick {
                setLoading(true)
                obtainSubmissionData()
            }
        }
    }

    fun setup() {
        setupToolbar()
        setSubmission(rootSubmission)
        //we must set up the sliding panel prior to registering to the event
        EventBus.getDefault().register(this)
        setLoading(false)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterPdfFragmentListeners()
        mFileJob?.cancel()
        mCreateAnnotationJob?.cancel()
        mUpdateAnnotationJob?.cancel()
        mDeleteAnnotationJob?.cancel()
        mAnnotationsJob?.cancel()
        mPdfContentJob?.cancel()
        initJob?.cancel()
        EventBus.getDefault().unregister(this)
    }

    private fun setSubmission(submission: Submission?) {
        submission?.attachments?.firstOrNull()?.let {
            if(it.contentType == "application/pdf" || it.previewUrl?.contains("canvadoc") == true) {
                if(it.previewUrl?.contains("canvadoc") == true) {
                    handlePdfContent(it.previewUrl ?: "")
                } else {
                    handlePdfContent(it.url ?: "")
                }
            }
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun handlePdfContent(pdfUrl: String) {
        mPdfContentJob = tryWeave {
            if(pdfUrl.contains("canvadoc") == true) {
                val redirectUrl = getCanvaDocsRedirect(pdfUrl)
                //extract the domain for API use
                mCanvaDocsDomain = extractCanvaDocsDomain(redirectUrl)
                if (redirectUrl.isNotEmpty()) {
                    val responseBody = awaitApi<ResponseBody> { CanvaDocsManager.getCanvaDoc(redirectUrl, it) }
                    val canvaDocsJSON = JSONObject(responseBody.string())
                    val pdfDownloadUrl = mCanvaDocsDomain + (canvaDocsJSON.get("urls") as JSONObject).get("pdf_download")
                    val docUrl = (canvaDocsJSON.get("panda_push") as JSONObject).get("document_channel")
                    mCanvaDocId = extractDocId(docUrl as String)
                    //load the pdf
                    load(pdfDownloadUrl) { setupPSPDFKit(it) }
                    //extract the session id
                    mSessionId = extractSessionId(pdfDownloadUrl)
                } else {
                    //TODO: handle case where redirect url is empty, could be canvadoc failure case
                }
            } else {
                //keep things working if they don't have canvadocs
                load(pdfUrl) { setupPSPDFKit(it) }
            }
        } catch {
            // Show error
            toast(R.string.errorOccurred)
            it.printStackTrace()
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun load(url: String, onFinished: (Uri) -> Unit) {
        mFileJob?.cancel()
        mFileJob = weave {
            progressBar.isIndeterminate = true
            progressBar.setColor(ContextCompat.getColor(this@StudentSubmissionView.context, (R.color.defaultTextGray)))
            val studentRed = ContextCompat.getColor(this@StudentSubmissionView.context, (R.color.login_studentAppTheme))

            val jitterThreshold = 300L
            val showLoadingRunner = Runnable {
                loadingContainer.setVisible()
                progressBar.announceForAccessibility(getContext().getString(R.string.loading))
            }
            val startTime = System.currentTimeMillis()
            val handler = Handler()
            handler.postDelayed(showLoadingRunner, jitterThreshold)

            val tempFile: File? = com.instructure.annotations.FileCaching.FileCache.awaitFileDownload(url) {
                onUI {
                    progressBar.setColor(studentRed)
                    progressBar.setProgress(it)
                }
            }

            if (tempFile != null) {
                progressBar.isIndeterminate = true
                onFinished(Uri.fromFile(tempFile))
            } else {
                loadingView.setGone()
                retryLoadingContainer.setVisible()
                retryLoadingButton.onClick {
                    setLoading(true)
                    obtainSubmissionData()
                }
            }

            val passedTime = System.currentTimeMillis() - startTime
            val hideLoadingRunner = Runnable { loadingContainer.setGone() }
            when {
                passedTime < jitterThreshold -> {
                    handler.removeCallbacks(showLoadingRunner); hideLoadingRunner.run()
                }
                passedTime < jitterThreshold * 2 -> handler.postDelayed(hideLoadingRunner, (jitterThreshold * 2) - passedTime)
                else -> hideLoadingRunner.run()
            }
        }
    }

    private fun setupPSPDFKit(uri: Uri) {
        // Order here matters, be careful
        val newPdfFragment = PdfFragment.newInstance(uri, mPdfConfiguration)
        setFragment(newPdfFragment)
        mPdfFragment = newPdfFragment
        mPdfFragment?.addOnAnnotationCreationModeChangeListener(this)
        mPdfFragment?.addOnAnnotationEditingModeChangeListener(this)

        //we don't need to do annotations if there are anonymous peer reviews
        attachDocListener()
        mPdfFragment?.addInsets(0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, context.resources.displayMetrics).toInt(), 0, 0)
        setupPdfAnnotationDefaults()
    }

    private fun setupPdfAnnotationDefaults() {
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.INK, object: InkAnnotationDefaultsProvider(context) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = ContextCompat.getColor(context, R.color.blueAnnotation)
            override fun getSupportedProperties(): EnumSet<AnnotationProperty> = EnumSet.of(AnnotationProperty.COLOR)
            override fun getDefaultThickness(): Float = 2f
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.FREETEXT, object: FreeTextAnnotationDefaultsProvider(context) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = ContextCompat.getColor(context, R.color.darkGrayAnnotation)
            override fun getSupportedProperties(): EnumSet<AnnotationProperty> = EnumSet.of(AnnotationProperty.COLOR)
            override fun getDefaultTextSize(): Float = 12f
            override fun getDefaultFillColor(): Int = ContextCompat.getColor(context, R.color.white)
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.SQUARE, object: ShapeAnnotationDefaultsProvider(context, AnnotationType.SQUARE) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = ContextCompat.getColor(context, R.color.blueAnnotation)
            override fun getSupportedProperties(): EnumSet<AnnotationProperty> = EnumSet.of(AnnotationProperty.COLOR)
            override fun getDefaultThickness(): Float = 2f
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.STRIKEOUT, object: MarkupAnnotationDefaultsProvider(context, AnnotationType.STRIKEOUT) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = ContextCompat.getColor(context, R.color.redAnnotation)
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.HIGHLIGHT, object: MarkupAnnotationDefaultsProvider(context, AnnotationType.HIGHLIGHT) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.highlightAnnotationColors)
            override fun getDefaultColor(): Int = ContextCompat.getColor(context, R.color.yellowHighlightAnnotation)
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.NOTE, object: NoteAnnotationDefaultsProvider(context) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = ContextCompat.getColor(context, R.color.blueAnnotation)
            override fun getSupportedProperties(): EnumSet<AnnotationProperty> = EnumSet.of(AnnotationProperty.COLOR)
            override fun getAvailableIconNames(): Array<String> = arrayOf("")
        })
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun attachDocListener() {
        mPdfFragment?.addDocumentListener(object : DocumentListener by DocumentListenerSimpleDelegate() {
            override fun onDocumentLoaded(pdfDocument: PdfDocument) {
                mPdfFragment?.enterAnnotationCreationMode()
                if (mSessionId != null && mCanvaDocsDomain != null) {
                    mAnnotationsJob = tryWeave {
                        //snag them annotations with the session id
                        val annotations = awaitApi<CanvaDocAnnotationResponse> { CanvaDocsManager.getAnnotations(mSessionId as String, mCanvaDocsDomain as String, it) }
                        // We don't want to trigger the annotation events here, so unregister and re-register after
                        mPdfFragment?.document?.annotationProvider?.removeOnAnnotationUpdatedListener(mAnnotationUpdateListener)

                        // First we want to grab all of the comment replies
                        for (item in annotations.data) {
                            if (item.annotationType == CanvaDocAnnotation.AnnotationType.COMMENT_REPLY) {
                                //store it, to be displayed later when user selects annotation
                                if (mCommentRepliesHashMap.containsKey(item.inReplyTo)) {
                                    mCommentRepliesHashMap[item.inReplyTo]?.add(item)
                                } else {
                                    mCommentRepliesHashMap[item.inReplyTo!!] = arrayListOf(item)
                                }
                            }
                        }

                        // Next grab the regular annotations
                        for (item in annotations.data) {
                            //otherwise, display it to the user
                            val annotation = item.convertCanvaDocAnnotationToPDF(this@StudentSubmissionView.context)
                            if (annotation != null && item.annotationType != CanvaDocAnnotation.AnnotationType.COMMENT_REPLY) {
                                if(item.isEditable == false) {
                                    annotation.flags = EnumSet.of(AnnotationFlags.LOCKED, AnnotationFlags.LOCKEDCONTENTS, AnnotationFlags.NOZOOM)
                                }

                                // If the annotation is a note we need to add its contents back in from the head comment reply
                                if(item.annotationType == CanvaDocAnnotation.AnnotationType.TEXT) {
                                    annotation.contents = mCommentRepliesHashMap[item.annotationId]?.firstOrNull()?.contents ?: ""
                                }

                                mPdfFragment?.document?.annotationProvider?.addAnnotationToPage(annotation)
                                mPdfFragment?.notifyAnnotationHasChanged(annotation)
                            }
                        }
                        mPdfFragment?.document?.annotationProvider?.addOnAnnotationUpdatedListener(mAnnotationUpdateListener)
                        mPdfFragment?.addOnAnnotationSelectedListener(mAnnotationSelectedListener)
                        mPdfFragment?.addOnAnnotationDeselectedListener(mAnnotationDeselectedListener)
                    } catch {
                        // Show error
                        toast(R.string.annotationErrorOccurred)
                        it.printStackTrace()
                    }
                }
            }
        })
    }

    @SuppressLint("CommitTransaction")
    private fun setFragment(fragment: Fragment) {
        if(isAttachedToWindow) supportFragmentManager.beginTransaction().replace(content.id, fragment).commitNowAllowingStateLoss()
    }

    private val freeTextDialogCallback = object : (Boolean, String) -> Unit {
        override fun invoke(cancelled: Boolean, text: String) {

            val annotation = if (mPdfFragment?.selectedAnnotations?.size ?: 0 > 0) mPdfFragment?.selectedAnnotations?.get(0) ?: return else return
            if (cancelled && annotation.contents.isNullOrEmpty()) {
                // Remove the annotation
                mPdfFragment?.document?.annotationProvider?.removeAnnotationFromPage(annotation)
                mPdfFragment?.notifyAnnotationHasChanged(annotation)
                mPdfFragment?.clearSelectedAnnotations()
                mPdfFragment?.enterAnnotationCreationMode()
                return
            }

            //We need to force a create call here
            annotation.contents = text
            createNewAnnotation(annotation)
            // we need to update the UI so pspdfkit knows how to handle this
            mPdfFragment?.clearSelectedAnnotations()
            mPdfFragment?.enterAnnotationCreationMode()
        }
    }

    // region annotationModes
    override fun onEnterAnnotationCreationMode(controller: AnnotationCreationController) {
        mAnnotationCreationInspectorController?.bindAnnotationCreationController(controller)
        mAnnotationCreationToolbar.bindController(controller)
        annotationToolbarLayout.displayContextualToolbar(mAnnotationCreationToolbar, true)

        mCurrentAnnotationModeTool = controller.activeAnnotationTool
    }

    override fun onExitAnnotationCreationMode(p0: AnnotationCreationController) {
        annotationToolbarLayout.removeContextualToolbar(true)
        mAnnotationCreationToolbar.unbindController()
        mAnnotationCreationInspectorController?.unbindAnnotationCreationController()

        mCurrentAnnotationModeTool = AnnotationTool.NONE
    }

    override fun onEnterAnnotationEditingMode(controller: AnnotationEditingController) {
        mCurrentAnnotationModeType = controller.currentlySelectedAnnotation?.type
        mAnnotationEditingToolbar.bindController(controller)
        mAnnotationEditingInspectorController?.bindAnnotationEditingController(controller)
        annotationToolbarLayout.displayContextualToolbar(mAnnotationEditingToolbar, true)
    }

    override fun onExitAnnotationEditingMode(controller: AnnotationEditingController) {
        annotationToolbarLayout.removeContextualToolbar(true)
        mAnnotationEditingToolbar.unbindController()
        mAnnotationEditingInspectorController?.unbindAnnotationEditingController()

        mCurrentAnnotationModeType = AnnotationType.NONE

        //send them back to creating annotations
        mPdfFragment?.enterAnnotationCreationMode()
    }

    override fun onChangeAnnotationEditingMode(controller: AnnotationEditingController) {
        mCurrentAnnotationModeType = controller.currentlySelectedAnnotation?.type
    }

    override fun onChangeAnnotationCreationMode(controller: AnnotationCreationController) {
        mCurrentAnnotationModeTool = controller.activeAnnotationTool!!
    }

    private fun unregisterPdfFragmentListeners() {
        mPdfFragment?.removeOnAnnotationCreationModeChangeListener(this)
        mPdfFragment?.removeOnAnnotationEditingModeChangeListener(this)
        mPdfFragment?.document?.annotationProvider?.removeOnAnnotationUpdatedListener(mAnnotationUpdateListener)
        mPdfFragment?.removeOnAnnotationSelectedListener(mAnnotationSelectedListener)
        mPdfFragment?.removeOnAnnotationDeselectedListener(mAnnotationDeselectedListener)
    }
    //endregion

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAnnotationCommentAdded(event: AnnotationCommentAdded) {
        if(event.assigneeId == assignee.id) {
            //add the comment to the hashmap
            mCommentRepliesHashMap[event.annotation.inReplyTo]?.add(event.annotation)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAnnotationCommentEdited(event: AnnotationCommentEdited) {
        if(event.assigneeId == assignee.id) {
                //update the annotation in the hashmap
                mCommentRepliesHashMap[event.annotation.inReplyTo]?.
                        find { it.annotationId == event.annotation.annotationId }?.contents = event.annotation.contents
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAnnotationCommentDeleted(event: AnnotationCommentDeleted) {
        if(event.assigneeId == assignee.id) {
            if(event.isHeadAnnotation) {
                //we need to delete the entire list of comments from the hashmap
                mCommentRepliesHashMap.remove(event.annotation.inReplyTo)
            } else {
                //otherwise just remove the comment
                mCommentRepliesHashMap[event.annotation.inReplyTo]?.remove(event.annotation)
            }
        }
    }

    class AnnotationCommentAdded(val annotation: CanvaDocAnnotation, val assigneeId: Long)
    class AnnotationCommentEdited(val annotation: CanvaDocAnnotation, val assigneeId: Long)
    class AnnotationCommentDeleted(val annotation: CanvaDocAnnotation, val isHeadAnnotation: Boolean, val assigneeId: Long)
}