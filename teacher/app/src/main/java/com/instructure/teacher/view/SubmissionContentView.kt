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
package com.instructure.teacher.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.ListPopupWindow
import android.util.TypedValue
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import com.instructure.annotations.*
import com.instructure.annotations.AnnotationDialogs.AnnotationCommentDialog
import com.instructure.annotations.AnnotationDialogs.AnnotationErrorDialog
import com.instructure.annotations.AnnotationDialogs.FreeTextDialog
import com.instructure.annotations.FileCaching.DocumentListenerSimpleDelegate
import com.instructure.annotations.FileCaching.FileCache
import com.instructure.canvasapi2.managers.CanvaDocsManager
import com.instructure.canvasapi2.managers.SubmissionManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotation
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotationResponse
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.pandautils.dialogs.UnsavedChangesExitDialog
import com.instructure.pandautils.utils.*
import com.instructure.teacher.PSPDFKit.AnnotationComments.AnnotationCommentListFragment
import com.instructure.teacher.R
import com.instructure.teacher.activities.SpeedGraderActivity
import com.instructure.teacher.activities.ViewMediaActivity
import com.instructure.teacher.adapters.StudentContextFragment
import com.instructure.teacher.dialog.*
import com.instructure.teacher.events.RationedBusEvent
import com.instructure.teacher.fragments.*
import com.instructure.teacher.interfaces.ShareableFile
import com.instructure.teacher.interfaces.SpeedGraderWebNavigator
import com.instructure.interactions.router.Route
import com.instructure.interactions.router.RouteContext
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.pandautils.utils.ProfileUtils
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
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.android.synthetic.main.adapter_speed_grader_group_member.view.*
import kotlinx.android.synthetic.main.view_submission_content.view.*
import kotlinx.coroutines.experimental.Job
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.HashMap

@SuppressLint("ViewConstructor")
class SubmissionContentView(
        context: Context,
        private val mStudentSubmission: GradeableStudentSubmission,
        private val mAssignment: Assignment,
        private val mCourse: Course,
        var initialTabIndex: Int = 0
) : FrameLayout(context), AnnotationManager.OnAnnotationCreationModeChangeListener, AnnotationManager.OnAnnotationEditingModeChangeListener {

    private var mContainerId: Int = 0
    private val mAssignee: Assignee get() = mStudentSubmission.assignee
    private val mRootSubmission: Submission? get() = mStudentSubmission.submission
    private val mBottomViewPager: ViewPagerNoSwipe

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
    private var mIsCleanedUp = false
    //endregion

    private val activity: SpeedGraderActivity get() = context as SpeedGraderActivity
    private var isUpdatingWithNoNetwork = false
    private val mGradeFragment by lazy { SpeedGraderGradeFragment.newInstance(mRootSubmission, mAssignment, mCourse, mAssignee) }
    private val supportFragmentManager = (context as AppCompatActivity).supportFragmentManager

    val hasUnsavedChanges: Boolean
        get() = mGradeFragment.hasUnsavedChanges

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
            }
            return true
        }
    }

    val mAnnotationDeselectedListener = AnnotationManager.OnAnnotationDeselectedListener { _, _->
        commentsButton.setGone()
    }

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

    private fun setLoading(isLoading: Boolean) {
        retryLoadingContainer.setGone()
        loadingView?.setVisible(isLoading)
        slidingUpPanelLayout?.setVisible(!isLoading)
        panelContent?.setVisible(!isLoading)
        contentRoot?.setVisible(!isLoading)
        divider?.setVisible(!isLoading)
    }

    //region view lifecycle
    init {
        View.inflate(context, R.layout.view_submission_content, this)

        setLoading(true)

        //if we have anonymous peer reviews we don't want the teacher to be able to annotate
        if(mAssignment.isPeerReviews && mAssignment.isAnonymousPeerReviews) {
            annotationToolbarLayout.setGone()
            inspectorCoordinatorLayout.setGone()
        }
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

        mContainerId = View.generateViewId()
        content.id = mContainerId
        mBottomViewPager = bottomViewPager.apply { id = View.generateViewId() }
        configureCommentView()
    }

    fun configureCommentView() {
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
                        activity.isCurrentlyAnnotating = true //don't want the sliding panel getting in the way
                        // Create new comment reply for this annotation.
                        if(text.isValid()) {
                            createCommentAnnotation(currentAnnotation.annotationId, currentAnnotation.page, text)
                        }
                    }.show(supportFragmentManager, AnnotationCommentDialog::class.java.simpleName)
                } else {
                    //otherwise, show the comment list fragment
                    mCommentRepliesHashMap[currentAnnotation.annotationId]?.let {
                        if(!it.isEmpty()) {
                            //otherwise, show the comment list fragment
                            val bundle = AnnotationCommentListFragment.makeBundle(it, canvaDocId, sessionId, canvaDocsDomain, mAssignee.id)
                            //if isTablet, we need to prevent the sliding panel from moving opening all the way with the keyboard
                            if(context.isTablet) {
                                activity.isCurrentlyAnnotating = true
                            }
                            RouteMatcher.route(context, Route(AnnotationCommentListFragment::class.java, null, bundle))
                        }
                    }
                }
            }
        }
    }

    fun configureCreationMenuItemGrouping(toolbarMenuItems: MutableList<ContextualToolbarMenuItem>, capacity: Int) : MutableList<ContextualToolbarMenuItem> {
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupToolbar(mAssignee)
        obtainSubmissionData()
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun obtainSubmissionData() {
        initJob = tryWeave {
            if (!mStudentSubmission.isCached) {
                mStudentSubmission.submission = awaitApi { SubmissionManager.getSingleSubmission(mCourse.id, mAssignment.id, mStudentSubmission.assigneeId, it, true) }
                mStudentSubmission.isCached = true
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
        setupToolbar(mAssignee)
        setupSubmissionVersions(mRootSubmission?.submissionHistory?.filterNotNull()?.filter { it.attempt > 0 })
        setSubmission(mRootSubmission)
        setupBottomSheetViewPager(mCourse)
        setupSlidingPanel()
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
        mBottomViewPager.adapter = null
        EventBus.getDefault().unregister(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

        if (context.isTablet) return

        // Resize sliding panel and content, don't if keyboard based annotations are active
        // we only do this if the oldw == w so we won't be resizing on rotation
        if (oldh > 0 && oldh != h && oldw == w && !activity.isCurrentlyAnnotating) {
            val newState = if (h < oldh) SlidingUpPanelLayout.PanelState.EXPANDED else SlidingUpPanelLayout.PanelState.ANCHORED
            slidingUpPanelLayout?.panelState = newState

            // Have to post here as we wait for contentRoot height to settle
            contentRoot.post {
                val slideOffset = when (newState) {
                    SlidingUpPanelLayout.PanelState.EXPANDED -> 1f
                    SlidingUpPanelLayout.PanelState.ANCHORED -> 0.5f
                    else -> 0f
                }

                val maxHeight = contentRoot.height
                val adjustedHeight = Math.abs(maxHeight * slideOffset).toInt()

                if (slideOffset >= 0.50F) { //Prevents resizing views when not necessary
                    mBottomViewPager.layoutParams?.height = adjustedHeight
                    mBottomViewPager.requestLayout()
                } else if (slideOffset <= 0.50F) {
                    contentWrapper?.layoutParams?.height = Math.abs(maxHeight - adjustedHeight)
                    contentWrapper?.requestLayout()
                }
            }
        }
    }

    @SuppressLint("CommitTransaction")
    fun performCleanup() {
        mIsCleanedUp = true
        getCurrentFragment()?.let { supportFragmentManager.beginTransaction().remove(it).commit() }
    }
    //endregion

    //region private helpers
    private fun setSubmission(submission: Submission?) {
        if (submission != null) submissionVersionsButton.text = submission.submittedAt.getSubmissionFormattedDate(context)
        val content = when {
            Assignment.SUBMISSION_TYPE.NONE in mAssignment.submissionTypes -> NoneContent()
            Assignment.SUBMISSION_TYPE.ON_PAPER in mAssignment.submissionTypes -> OnPaperContent()
            submission == null || submission.submissionType == null -> NoSubmissionContent()
            mAssignment.getState(submission) == AssignmentUtils2.ASSIGNMENT_STATE_MISSING -> NoSubmissionContent()
            else -> when (Assignment.getSubmissionTypeFromAPIString(submission.submissionType)) {

            // LTI submission
                Assignment.SUBMISSION_TYPE.BASIC_LTI_LAUNCH -> ExternalToolContent(
                        mCourse,
                        submission.previewUrl.validOrNull() ?: mAssignment.url.validOrNull() ?: mAssignment.htmlUrl
                )

            // Text submission
                Assignment.SUBMISSION_TYPE.ONLINE_TEXT_ENTRY -> TextContent(submission.body ?: "")

            // Media submission
                Assignment.SUBMISSION_TYPE.MEDIA_RECORDING -> submission.mediaComment?.let {
                    MediaContent(
                            uri = Uri.parse(it.url),
                            contentType = it.contentType ?: "",
                            displayName = it.displayName
                    )
                } ?: UnsupportedContent()

            // File uploads
                Assignment.SUBMISSION_TYPE.ONLINE_UPLOAD -> getAttachmentContent(submission.attachments[0])

            // URL Submission
                Assignment.SUBMISSION_TYPE.ONLINE_URL -> UrlContent(submission.url!!, submission.attachments.firstOrNull()?.url)

            // Quiz Submission
                Assignment.SUBMISSION_TYPE.ONLINE_QUIZ -> QuizContent(
                        mCourse.id,
                        mAssignment.id,
                        submission.userId,
                        submission.previewUrl ?: "",
                        QuizSubmission.parseWorkflowState(submission.workflowState) == QuizSubmission.WORKFLOW_STATE.PENDING_REVIEW
                )

            // Discussion Submission
                Assignment.SUBMISSION_TYPE.DISCUSSION_TOPIC -> DiscussionContent(submission.previewUrl)

            // Unsupported type
                else -> UnsupportedContent()
            }
        }
        setGradeableContent(content)
    }

    private fun getAttachmentContent(attachment: Attachment): GradeableContent {
        var type = attachment.contentType
        if (type == "*/*") {
            val fileExtension = attachment.filename.substringAfterLast(".")
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
                    ?: MimeTypeMap.getFileExtensionFromUrl(attachment.url)
                    ?: type
        }
        return when {
            type == "application/pdf" || (attachment.previewUrl?.contains(CANVADOC) ?: false) -> {
                if(attachment.previewUrl?.contains(CANVADOC) ?: false) {
                    PdfContent(attachment.previewUrl ?: "")
                } else {
                    PdfContent(attachment.url ?: "")
                }
            }
            type.startsWith("audio") || type.startsWith("video") -> with(attachment) {
                MediaContent(
                        uri = Uri.parse(url),
                        thumbnailUrl = thumbnailUrl,
                        contentType = contentType,
                        displayName = displayName
                )
            }
            type.startsWith("image") -> ImageContent(attachment.url ?: "", attachment.contentType)
            else -> OtherAttachmentContent(attachment)
        }
    }

    private fun setAttachmentContent(attachment: Attachment) {
        setGradeableContent(getAttachmentContent(attachment))
    }

    private fun setupSubmissionVersions(unsortedSubmissions: List<Submission>?) {
        if (unsortedSubmissions == null) return
        when (unsortedSubmissions.size) {
            0 -> submissionVersionsButton.setGone()
            1 -> submissionVersionsButton.setVisible().background = ColorDrawable(Color.TRANSPARENT)
            else -> unsortedSubmissions.sortedByDescending { it.submittedAt }.let { submissions ->
                val submissionDates = submissions.map { it.submittedAt.getSubmissionFormattedDate(context) }
                submissionVersionsButton.onClickWithRequireNetwork {
                    val dialog = RadioButtonDialog.getInstance(supportFragmentManager, resources.getString(R.string.submission_versions), submissionDates as ArrayList,
                            submissionDates.indexOf(submissionVersionsButton.text.toString())) { selectedIdx ->
                        EventBus.getDefault().post(SubmissionSelectedEvent(submissions[selectedIdx]))
                    }
                    dialog.show(supportFragmentManager, RadioButtonDialog::class.java.simpleName)
                }
                submissionVersionsButton.setVisible()
            }
        }
    }

    private fun setupToolbar(assignee: Assignee) {
        speedGraderToolbar.setupBackButton {
            // Use back button for WebView if applicable
            (getCurrentFragment() as? SpeedGraderWebNavigator)?.let {
                if (it.canGoBack()) {
                    it.goBack()
                    return@setupBackButton
                }
            }

            // Notify of unsaved changes
            if (hasUnsavedChanges) {
                UnsavedChangesExitDialog.show(supportFragmentManager) { (context as? Activity)?.finish() }
            } else {
                (context as? Activity)?.finish()
            }
        }

        val assigneeName = if (TeacherPrefs.shouldGradeAnonymously(mCourse.id, mAssignment.id)) resources.getString(R.string.anonymousStudentLabel) else assignee.name
        titleTextView.text = assigneeName

        if (mStudentSubmission.isCached) {
            // get string/color resources for assignment status
            val (stringRes, colorRes) = mAssignment.getResForSubmission(mRootSubmission)
            if (stringRes == -1 || colorRes == -1) {
                contentDescription = titleTextView.text
                subtitleTextView.setGone()
            } else {
                contentDescription = "${titleTextView.text}, ${resources.getString(stringRes)}"
                subtitleTextView.setText(stringRes)
                subtitleTextView.setTextColor(ContextCompat.getColor(context, colorRes))
            }
        }

        speedGraderToolbar.setupMenu(R.menu.menu_share_file, menuItemCallback)
        ViewStyler.colorToolbarIconsAndText(context as Activity, speedGraderToolbar, Color.BLACK)
        ViewStyler.setStatusBarLight(context as Activity)
        ViewStyler.setToolbarElevationSmall(context, speedGraderToolbar)

        when {
            TeacherPrefs.shouldGradeAnonymously(mCourse.id, mAssignment.id) -> userImageView.setAnonymousAvatar()
            assignee is GroupAssignee -> userImageView.setImageResource(assignee.iconRes)
            assignee is StudentAssignee -> {
                ProfileUtils.loadAvatarForUser(userImageView, assignee.student.name, assignee.student.avatarUrl)
                userImageView.setupAvatarA11y(assignee.name)
                userImageView.onClick {
                    val bundle = StudentContextFragment.makeBundle(assignee.id, mCourse.id)
                    RouteMatcher.route(context, Route(StudentContextFragment::class.java, null, bundle))
                }
            }
        }

        if (assignee is GroupAssignee && !TeacherPrefs.shouldGradeAnonymously(mCourse.id, mAssignment.id)) setupGroupMemberList(assignee)
    }

    val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menu_share -> {
                (getCurrentFragment() as? ShareableFile)?.viewExternally()

                //pdfs are a different type of fragment
                if(mPdfFragment != null) {
                    mPdfFragment?.document?.documentSource?.fileUri?.viewExternally(context, "application/pdf")
                }
            }
        }
    }


    private fun setupGroupMemberList(assignee: GroupAssignee) {
        assigneeWrapperView.onClick {
            val popup = ListPopupWindow(context)
            popup.anchorView = it
            popup.setAdapter(object : ArrayAdapter<User>(context, 0, assignee.students) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                    val user = getItem(position)
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.adapter_speed_grader_group_member, parent, false)
                    ProfileUtils.loadAvatarForUser(view.memberAvatarView, user.name, user.avatarUrl)
                    view.memberNameView.text = user.name
                    return view
                }
            })
            popup.setContentWidth(resources.getDimensionPixelSize(R.dimen.speedgraderGroupMemberListWidth))
            popup.verticalOffset = -assigneeWrapperView.height
            popup.isModal = true // For a11y
            popup.setOnItemClickListener { _, _, position, _ ->
                val bundle = StudentContextFragment.makeBundle(assignee.students[position].id, mCourse.id)
                RouteMatcher.route(context, Route(StudentContextFragment::class.java, null, bundle))
                popup.dismiss()
            }
            popup.show()
        }
    }

    private fun setGradeableContent(content: GradeableContent) {
        // Handle the existing PdfFragment if there is one
        val currentFragment = getCurrentFragment()
        if (currentFragment is PdfFragment) {
            // Unregister listeners for the existing fragment
            unregisterPdfFragmentListeners()
        }

        when (content) {
            is PdfContent -> handlePdfContent(content)
            is NoSubmissionContent -> when (mAssignee) {
                is StudentAssignee -> showMessageFragment(R.string.speedgrader_student_no_submissions)
                is GroupAssignee -> showMessageFragment(R.string.speedgrader_group_no_submissions)
            }
            is UnsupportedContent -> showMessageFragment(R.string.speedgrader_unsupported_type)
            is UrlContent -> setFragment(SpeedGraderUrlSubmissionFragment.newInstance(content.url, content.previewUrl))
            is QuizContent -> setFragment(SpeedGraderQuizSubmissionFragment.newInstance(content))
            is OtherAttachmentContent -> with(content.attachment) {
                setFragment(ViewUnsupportedFileFragment.newInstance(
                        uri = Uri.parse(url),
                        displayName = displayName ?: filename,
                        contentType = contentType,
                        previewUri = thumbnailUrl?.let { Uri.parse(it) },
                        fallbackIcon = iconRes
                ))
            }
            is TextContent -> setFragment(SpeedGraderTextSubmissionFragment.newInstance(content.text))
            is MediaContent -> setFragment(ViewMediaFragment.newInstance(content))
            is ImageContent -> load(content.url) { setFragment(ViewImageFragment.newInstance(content.url, it, content.contentType, false)) }
            is NoneContent -> showMessageFragment(R.string.speedGraderNoneMessage)
            is ExternalToolContent -> setFragment(SpeedGraderLtiSubmissionFragment.newInstance(content))
            is OnPaperContent -> showMessageFragment(R.string.speedGraderOnPaperMessage)
            is DiscussionContent -> setFragment(SimpleWebViewFragment.newInstance(content.previewUrl!!))
        }.exhaustive
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun handlePdfContent(pdfContent: PdfContent) {
        mPdfContentJob = tryWeave {
            if(pdfContent.url.contains(CANVADOC)) {
                val redirectUrl = getCanvaDocsRedirect(pdfContent.url)
                //extract the domain for API use
                mCanvaDocsDomain = extractCanvaDocsDomain(redirectUrl)
                if (redirectUrl.isNotEmpty()) {
                    val responseBody = awaitApi<ResponseBody> { CanvaDocsManager.getCanvaDoc(redirectUrl, it) }
                    val canvaDocsJSON = JSONObject(responseBody.string())
                    val pdfUrl = mCanvaDocsDomain + (canvaDocsJSON.get("urls") as JSONObject).get("pdf_download")
                    val docUrl = (canvaDocsJSON.get("panda_push") as JSONObject).get("document_channel")
                    mCanvaDocId = extractDocId(docUrl as String)
                    //load the pdf
                    load(pdfUrl) { setupPSPDFKit(it) }
                    //extract the session id
                    mSessionId = extractSessionId(pdfUrl)
                } else {
                    //TODO: handle case where redirect url is empty, could be canvadoc failure case
                }
            } else {
                //keep things working if they don't have canvadocs
                load(pdfContent.url) { setupPSPDFKit(it) }
            }
        } catch {
            // Show error
            toast(R.string.errorOccurred)
            it.printStackTrace()
        }
    }

    private fun showMessageFragment(@StringRes stringRes: Int) = showMessageFragment(resources.getString(stringRes))

    private fun showMessageFragment(message: String) {
        val fragment = SpeedGraderEmptyFragment.newInstance(message = message)
        setFragment(fragment)
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(mContainerId)
    }

    private fun setupPSPDFKit(uri: Uri) {
        // Order here matters, be careful
        val newPdfFragment = PdfFragment.newInstance(uri, mPdfConfiguration)
        setFragment(newPdfFragment)
        mPdfFragment = newPdfFragment
        mPdfFragment?.addOnAnnotationCreationModeChangeListener(this)
        mPdfFragment?.addOnAnnotationEditingModeChangeListener(this)

        // push the pdf viewing screen under the toolbar
        if(!(mAssignment.isAnonymousPeerReviews && mAssignment.isPeerReviews)) {
            //we don't need to do annotations if there are anonymous peer reviews
            attachDocListener()

            if ((context as Activity).isTablet)
                mPdfFragment?.addInsets(0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 68f, context.resources.displayMetrics).toInt(), 0, 0)
            else
                mPdfFragment?.addInsets(0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, context.resources.displayMetrics).toInt(), 0, 0)
        } else {
            mPdfFragment?.addInsets(0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, context.resources.displayMetrics).toInt(), 0, 0)

        }
        setupPdfAnnotationDefaults()
    }

    private fun setupPdfAnnotationDefaults() {
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.INK, object: InkAnnotationDefaultsProvider(context) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = context.getColorCompat(R.color.blueAnnotation)
            override fun getSupportedProperties(): EnumSet<AnnotationProperty> = EnumSet.of(AnnotationProperty.COLOR)
            override fun getDefaultThickness(): Float = 2f
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.FREETEXT, object: FreeTextAnnotationDefaultsProvider(context) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = context.getColorCompat(R.color.darkGrayAnnotation)
            override fun getSupportedProperties(): EnumSet<AnnotationProperty> = EnumSet.of(AnnotationProperty.COLOR)
            override fun getDefaultTextSize(): Float = 12f
            override fun getDefaultFillColor(): Int = context.getColorCompat(R.color.white)
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.SQUARE, object: ShapeAnnotationDefaultsProvider(context, AnnotationType.SQUARE) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = context.getColorCompat(R.color.blueAnnotation)
            override fun getSupportedProperties(): EnumSet<AnnotationProperty> = EnumSet.of(AnnotationProperty.COLOR)
            override fun getDefaultThickness(): Float = 2f
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.STRIKEOUT, object: MarkupAnnotationDefaultsProvider(context, AnnotationType.STRIKEOUT) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = context.getColorCompat(R.color.redAnnotation)
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.HIGHLIGHT, object: MarkupAnnotationDefaultsProvider(context, AnnotationType.HIGHLIGHT) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.highlightAnnotationColors)
            override fun getDefaultColor(): Int = context.getColorCompat(R.color.yellowHighlightAnnotation)
        })
        mPdfFragment?.setAnnotationDefaultsProvider(AnnotationType.NOTE, object: NoteAnnotationDefaultsProvider(context) {
            override fun getAvailableColors(): IntArray = context.resources.getIntArray(R.array.standardAnnotationColors)
            override fun getDefaultColor(): Int = context.getColorCompat(R.color.blueAnnotation)
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
                            val annotation = item.convertCanvaDocAnnotationToPDF(this@SubmissionContentView.context)
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
        if(!mIsCleanedUp && isAttachedToWindow) supportFragmentManager.beginTransaction().replace(mContainerId, fragment).commitNowAllowingStateLoss()

        //if we can share the content with another app, show the share icon
        speedGraderToolbar.menu.findItem(R.id.menu_share)?.isVisible = fragment is ShareableFile || fragment is PdfFragment
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun load(url: String, onFinished: (Uri) -> Unit) {
        mFileJob?.cancel()
        mFileJob = weave {
            speedGraderProgressBar.isIndeterminate = true
            speedGraderProgressBar.setColor(this@SubmissionContentView.context.getColorCompat(R.color.defaultTextGray))
            val teacherYellow = this@SubmissionContentView.context.getColorCompat(R.color.login_teacherAppTheme)

            val jitterThreshold = 300L
            val showLoadingRunner = Runnable {
                loadingContainer.setVisible()
                speedGraderProgressBar.announceForAccessibility(getContext().getString(R.string.loading))
            }
            val startTime = System.currentTimeMillis()
            val handler = Handler()
            handler.postDelayed(showLoadingRunner, jitterThreshold)

            val tempFile: File? = FileCache.awaitFileDownload(url) {
                onUI {
                    speedGraderProgressBar.setColor(teacherYellow)
                    speedGraderProgressBar.setProgress(it)
                }
            }

            if (tempFile != null) {
                speedGraderProgressBar.isIndeterminate = true
                onFinished(Uri.fromFile(tempFile))
            } else {
                showMessageFragment(R.string.error_loading_files)
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

    private fun setupSlidingPanel() {

        slidingUpPanelLayout?.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {

            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                adjustPanelHeights(slideOffset)
            }

            override fun onPanelStateChanged(panel: View?,
                    previousState: SlidingUpPanelLayout.PanelState?,
                    newState: SlidingUpPanelLayout.PanelState?) {
                if(newState != previousState) {
                    @Suppress("NON_EXHAUSTIVE_WHEN") //we don't want to update for all states, just these three
                    when(newState) {
                        SlidingUpPanelLayout.PanelState.ANCHORED -> {
                            postPanelEvent(newState, 0.5f)
                        }
                        SlidingUpPanelLayout.PanelState.EXPANDED ->
                            postPanelEvent(newState, 1.0f)
                        SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                            //fix for rotating when the panel is collapsed
                            mPdfFragment?.notifyLayoutChanged()
                            postPanelEvent(newState, 0.0f)
                        }
                    }
                }
            }
        })
    }

    private fun postPanelEvent(panelState: SlidingUpPanelLayout.PanelState, offset: Float) {
        val event = SlidingPanelAnchorEvent(panelState, offset)
        EventBus.getDefault().postSticky(event)
    }

    private fun adjustPanelHeights(offset: Float){
        //Adjusts the panel content sizes based on the position of the sliding portion of the view
        val maxHeight = contentRoot.height
        if (offset < 0 || maxHeight == 0) return

        val adjustedHeight = Math.abs(maxHeight * offset)

        if (offset >= 0.50F) { //Prevents resizing views when not necessary
            mBottomViewPager.layoutParams?.height = adjustedHeight.toInt()
            mBottomViewPager.requestLayout()
        }
        if (offset <= 0.50F) {
            contentWrapper?.layoutParams?.height = Math.abs(maxHeight - adjustedHeight).toInt()
            contentWrapper?.requestLayout()
        }
    }

    private fun setupBottomSheetViewPager(course: Course) {
        mBottomViewPager.offscreenPageLimit = 2
        mBottomViewPager.adapter = BottomSheetPagerAdapter.Holder(supportFragmentManager)
                .add(mGradeFragment)
                .add(SpeedGraderCommentsFragment.newInstance(
                        mRootSubmission,
                        mAssignee,
                        mCourse.id,
                        mAssignment.id,
                        mAssignment.groupCategoryId > 0 && mAssignee is GroupAssignee,
                        TeacherPrefs.shouldGradeAnonymously(mCourse.id, mAssignment.id)
                ))
                .add(SpeedGraderFilesFragment.newInstance(mRootSubmission))
                .set()

        mBottomViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {

                }
            }
        })

        bottomTabLayout.setupWithViewPager(mBottomViewPager)
        bottomTabLayout.setSelectedTabIndicatorColor(course.color)
        bottomTabLayout.setTabTextColors(Color.BLACK, course.color)
        bottomTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (slidingUpPanelLayout?.panelState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    slidingUpPanelLayout?.panelState = SlidingUpPanelLayout.PanelState.ANCHORED
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                EventBus.getDefault().post(TabSelectedEvent(tab?.position ?: 0))
                if (slidingUpPanelLayout?.panelState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    slidingUpPanelLayout?.panelState = SlidingUpPanelLayout.PanelState.ANCHORED
                }
            }
        })

        val spacing = resources.getDimensionPixelOffset(R.dimen.speedgrader_tab_spacing)

        for (i in 0 until bottomTabLayout.tabCount) {
            val tab = (bottomTabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
            val params = tab.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(spacing, 0, spacing, 0)
            tab.requestLayout()
        }

        mBottomViewPager.currentItem = initialTabIndex
    }

    private fun showVideoCommentDialog() {
        activity.disableViewPager()
        floatingRecordingView.setContentType(SGMediaCommentType.Video())
        floatingRecordingView.startVideoView()
        floatingRecordingView.recordingCallback = {
            it?.let {
                EventBus.getDefault().post(UploadMediaCommentEvent(it, mAssignment.id, mAssignment.courseId, mAssignee.id))
            }
        }
        floatingRecordingView.stoppedCallback = {
            activity.enableViewPager()
            EventBus.getDefault().post(MediaCommentDialogClosedEvent())
        }
        floatingRecordingView.replayCallback = {
            val bundle = ViewMediaActivity.makeBundle(it, "video", context.getString(R.string.videoCommentReplay), true)
            RouteMatcher.route(context, Route(bundle, RouteContext.MEDIA))
        }
    }

    private fun showAudioCommentDialog() {
        activity.disableViewPager()
        floatingRecordingView.setContentType(SGMediaCommentType.Audio())
        floatingRecordingView.setVisible()
        floatingRecordingView.stoppedCallback = {
            activity.enableViewPager()
            EventBus.getDefault().post(MediaCommentDialogClosedEvent())
        }
        floatingRecordingView.recordingCallback = {
            it?.let {
                EventBus.getDefault().post(UploadMediaCommentEvent(it, mAssignment.id, mAssignment.courseId, mAssignee.id))
            }
        }
    }

    private fun getAudioPermission() {
        ActivityCompat.requestPermissions((context as SpeedGraderActivity), arrayOf(PermissionUtils.RECORD_AUDIO), PermissionUtils.PERMISSION_REQUEST_CODE)
    }

    private fun getVideoPermission() {
        ActivityCompat.requestPermissions((context as SpeedGraderActivity), arrayOf(PermissionUtils.CAMERA, PermissionUtils.RECORD_AUDIO), PermissionUtils.PERMISSION_REQUEST_CODE)
    }

    private fun setupAnnotationCreationList(): MutableList<AnnotationTool> {
        return listOf(AnnotationTool.INK, AnnotationTool.HIGHLIGHT, AnnotationTool.STRIKEOUT, AnnotationTool.SQUARE, AnnotationTool.NOTE, AnnotationTool.FREETEXT).toMutableList()
    }

    private fun setupAnnotationEditList(): MutableList<AnnotationType> {
        return listOf(AnnotationType.INK, AnnotationType.HIGHLIGHT, AnnotationType.STRIKEOUT, AnnotationType.SQUARE, AnnotationType.NOTE, AnnotationType.FREETEXT).toMutableList()
    }
    //endregion

    private class BottomSheetPagerAdapter internal constructor(fm: FragmentManager, fragments: ArrayList<Fragment>) : FragmentPagerAdapter(fm) {

        private var fragments = ArrayList<Fragment>()

        init {
            this.fragments = fragments
        }

        override fun getItem(position: Int) = fragments[position]

        override fun getCount() = fragments.size

        override fun getPageTitle(position: Int) = when (position) {
            0 -> ContextKeeper.appContext.getString(R.string.sg_tab_grade).toUpperCase(Locale.getDefault())
            1 -> ContextKeeper.appContext.getString(R.string.sg_tab_comments).toUpperCase(Locale.getDefault())
            2 -> ContextKeeper.appContext.getString(R.string.sg_tab_files).toUpperCase(Locale.getDefault())
            else -> ""
        }

        internal class Holder(private val manager: FragmentManager) {

            private val fragments = ArrayList<Fragment>()

            fun add(f: Fragment): Holder {
                fragments.add(f)
                return this
            }

            fun set() = BottomSheetPagerAdapter(manager, fragments)
        }

        override fun finishUpdate(container: ViewGroup?) {
            // Workaround for known issue in the support library
            try { super.finishUpdate(container) } catch (nullPointerException: NullPointerException) { }
        }
    }

    //region annotation listeners
    override fun onEnterAnnotationCreationMode(controller: AnnotationCreationController) {
        //we only want to disable the viewpager if they are actively annotating
        if(controller.activeAnnotationTool != AnnotationTool.NONE) (context as SpeedGraderActivity).disableViewPager()
        mAnnotationCreationInspectorController?.bindAnnotationCreationController(controller)
        mAnnotationCreationToolbar.bindController(controller)
        annotationToolbarLayout.displayContextualToolbar(mAnnotationCreationToolbar, true)

        mCurrentAnnotationModeTool = controller.activeAnnotationTool
    }

    override fun onExitAnnotationCreationMode(p0: AnnotationCreationController) {
        (context as SpeedGraderActivity).enableViewPager()
        annotationToolbarLayout.removeContextualToolbar(true)
        mAnnotationCreationToolbar.unbindController()
        mAnnotationCreationInspectorController?.unbindAnnotationCreationController()

        mCurrentAnnotationModeTool = AnnotationTool.NONE
    }

    override fun onEnterAnnotationEditingMode(controller: AnnotationEditingController) {
        mCurrentAnnotationModeType = controller.currentlySelectedAnnotation?.type
        //we only want to disable the viewpager if they are actively annotating
        if(controller.currentlySelectedAnnotation != null) (context as SpeedGraderActivity).disableViewPager()
        mAnnotationEditingToolbar.bindController(controller)
        mAnnotationEditingInspectorController?.bindAnnotationEditingController(controller)
        annotationToolbarLayout.displayContextualToolbar(mAnnotationEditingToolbar, true)
    }

    override fun onExitAnnotationEditingMode(controller: AnnotationEditingController) {
        (context as SpeedGraderActivity).enableViewPager()
        activity.unlockOrientation()
        annotationToolbarLayout.removeContextualToolbar(true)
        mAnnotationEditingToolbar.unbindController()
        mAnnotationEditingInspectorController?.unbindAnnotationEditingController()

        mCurrentAnnotationModeType = AnnotationType.NONE

        //send them back to creating annotations
        mPdfFragment?.enterAnnotationCreationMode()
    }

    override fun onChangeAnnotationEditingMode(controller: AnnotationEditingController) {
        mCurrentAnnotationModeType = controller.currentlySelectedAnnotation?.type

        //we only want to disable the viewpager if they are actively annotating
        if(controller.currentlySelectedAnnotation != null) (context as SpeedGraderActivity).disableViewPager()
        else (context as SpeedGraderActivity).enableViewPager()
    }

    override fun onChangeAnnotationCreationMode(controller: AnnotationCreationController) {
        //we only want to disable the viewpager if they are actively annotating
        if(controller.activeAnnotationTool != AnnotationTool.NONE) (context as SpeedGraderActivity).disableViewPager()
        else (context as SpeedGraderActivity).enableViewPager()

        //we want to make sure that the keyboard doesn't mess up the view if they are using these annotations
        activity.isCurrentlyAnnotating = controller.activeAnnotationTool != AnnotationTool.NONE

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

    //region event bus subscriptions
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchSubmission(event: SubmissionSelectedEvent) {
        //close the annotations toolbar so it can be associated with new document
        mPdfFragment?.exitCurrentlyActiveMode()
        if (event.submission?.id == mRootSubmission?.id) setSubmission(event.submission)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchAttachment(event: SubmissionFileSelectedEvent) {
        if (event.submissionId == mRootSubmission?.id) {
            //close the annotations toolbar so it can be associated with new document
            mPdfFragment?.exitCurrentlyActiveMode()
            setAttachmentContent(event.attachment)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAnchorChanged(event: SlidingPanelAnchorEvent) {
        slidingUpPanelLayout?.panelState = event.anchorPosition
        //If we try to adjust the panels before contentRoot's height is determined, things don't work
        //This post works because we setup the panel before registering to the event
        contentRoot.post { adjustPanelHeights(event.offset) }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCommentTextFocused(event: CommentTextFocusedEvent) {
        if(event.assigneeId == mAssignee.id) {
            mPdfFragment?.exitCurrentlyActiveMode()
            activity.isCurrentlyAnnotating = false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAnnotationCommentAdded(event: AnnotationCommentAdded) {
        if(event.assigneeId == mAssignee.id) {
            //add the comment to the hashmap
            mCommentRepliesHashMap[event.annotation.inReplyTo]?.add(event.annotation)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAnnotationCommentEdited(event: AnnotationCommentEdited) {
        if(event.assigneeId == mAssignee.id) {
            //update the annotation in the hashmap
            mCommentRepliesHashMap[event.annotation.inReplyTo]?.
                    find { it.annotationId == event.annotation.annotationId }?.contents = event.annotation.contents
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAnnotationCommentDeleted(event: AnnotationCommentDeleted) {
        if(event.assigneeId == mAssignee.id) {
            if(event.isHeadAnnotation) {
                //we need to delete the entire list of comments from the hashmap
                mCommentRepliesHashMap.remove(event.annotation.inReplyTo)
            } else {
                //otherwise just remove the comment
                mCommentRepliesHashMap[event.annotation.inReplyTo]?.remove(event.annotation)
            }
        }
    }

    @Subscribe
    fun onTabSelected(event: TabSelectedEvent) {
        mBottomViewPager.currentItem = event.selectedTabIdx
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAudioPermissionGranted(event: AudioPermissionGrantedEvent) {
        if(event.assigneeId == mAssignee.id)
            showAudioCommentDialog()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onVideoPermissionGranted(event: VideoPermissionGrantedEvent) {
        if(event.assigneeId == mAssignee.id)
            showVideoCommentDialog()
    }

    //endregion

    fun configureEditMenuItemGrouping(toolbarMenuItems: MutableList<ContextualToolbarMenuItem>): MutableList<ContextualToolbarMenuItem> {
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

    val freeTextDialogCallback = object : (Boolean, String) -> Unit {
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
}

class SubmissionSelectedEvent(val submission: Submission?)
class SubmissionFileSelectedEvent(val submissionId: Long, val attachment: Attachment)
class QuizSubmissionGradedEvent(submission: Submission) : RationedBusEvent<Submission>(submission)
class SlidingPanelAnchorEvent(val anchorPosition: SlidingUpPanelLayout.PanelState, val offset: Float)
class CommentTextFocusedEvent(val assigneeId: Long)
class AnnotationCommentAdded(val annotation: CanvaDocAnnotation, val assigneeId: Long)
class AnnotationCommentEdited(val annotation: CanvaDocAnnotation, val assigneeId: Long)
class AnnotationCommentDeleted(val annotation: CanvaDocAnnotation, val isHeadAnnotation: Boolean, val assigneeId: Long)
class TabSelectedEvent(val selectedTabIdx: Int)
class UploadMediaCommentEvent(val file: File, val assignmentId: Long, val courseId: Long, val assigneeId: Long)


sealed class GradeableContent
class NoSubmissionContent : GradeableContent()
class NoneContent : GradeableContent()
class ExternalToolContent(val canvasContext: CanvasContext, val url: String) : GradeableContent()
class OnPaperContent : GradeableContent()
class UnsupportedContent : GradeableContent()
class OtherAttachmentContent(val attachment: Attachment) : GradeableContent()
class PdfContent(val url: String) : GradeableContent()
class TextContent(val text: String) : GradeableContent()
class ImageContent(val url: String, val contentType: String) : GradeableContent()
class UrlContent(val url: String, val previewUrl: String?) : GradeableContent()
class DiscussionContent(val previewUrl: String?) : GradeableContent()
class MediaCommentDialogClosedEvent
class AudioPermissionGrantedEvent(val assigneeId: Long)
class VideoPermissionGrantedEvent(val assigneeId: Long)


class QuizContent(
        val courseId: Long,
        val assignmentId: Long,
        val studentId: Long,
        val url: String,
        val pendingReview: Boolean) : GradeableContent()

class MediaContent(
        val uri: Uri,
        val contentType: String,
        val thumbnailUrl: String? = null,
        val displayName: String? = null
) : GradeableContent()
