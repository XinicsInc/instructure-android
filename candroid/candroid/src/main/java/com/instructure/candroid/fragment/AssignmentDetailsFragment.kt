/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.widget.TextView
import com.instructure.candroid.R
import com.instructure.interactions.Navigation
import com.instructure.candroid.util.LockInfoHTMLHelper
import com.instructure.candroid.util.Param
import com.instructure.candroid.util.RouterUtils
import com.instructure.candroid.view.ViewUtils
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.interactions.FragmentInteractions
import com.instructure.canvasapi2.utils.pageview.BeforePageView
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.pageview.PageViewUrlParam
import com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery
import com.instructure.pandautils.utils.NullableParcelableArg
import com.instructure.pandautils.utils.OnBackStackChangedEvent
import com.instructure.pandautils.utils.getModuleItemId
import com.instructure.pandautils.views.CanvasWebView
import kotlinx.android.synthetic.main.fragment_assignment_details.*
import kotlinx.android.synthetic.main.assignment_details_header.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

@PageView(url = "{canvasContext}/assignments/{assignmentId}")
class AssignmentDetailsFragment : ParentFragment() {

    // keep assignment logic within populateAssignmentDetails method, otherwise assignment could be null
    private var assignment by NullableParcelableArg<Assignment>()

    @PageViewUrlParam("assignmentId")
    private fun getAssignmentId() = assignment?.id ?: 0

    @PageViewUrlQuery("module_item_id")
    private fun pageViewModuleItemId() = getModuleItemId()

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        return FragmentInteractions.Placement.DETAIL
    }

    override fun title(): String {
        return if (assignment != null) assignment!!.name else getString(R.string.assignments)
    }

    /**
     * @param assignment The assignment
     * @param isWithinAnotherCallback See note above
     * @param isCached See note above
     */
    @BeforePageView
    fun setAssignment(assignment: Assignment, isWithinAnotherCallback: Boolean, isCached: Boolean) {
        this.assignment = assignment
        populateAssignmentDetails(assignment, isWithinAnotherCallback, isCached)
    }

    fun updateSubmissionDate(submissionDate: Date?) {
        var submitDate = getString(R.string.assignmentLastSubmission) + ": " + getString(R.string.assignmentNoSubmission)
        if (submissionDate != null) {
            submitDate = DateHelper.createPrefixedDateTimeString(context, R.string.assignmentLastSubmission, submissionDate)
        }
        textViewSubmissionDate.text = submitDate
    }

    override fun onPause() {
        super.onPause()
        canvasWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        canvasWebView.onResume()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackStackChangedEvent(event: OnBackStackChangedEvent) {
        event.get { clazz ->
            if (clazz?.isAssignableFrom(AssignmentDetailsFragment::class.java) == true) {
                canvasWebView.onResume()
            } else {
                canvasWebView.onPause()
            }
        }
    }

    override fun handleBackPressed(): Boolean {
        return canvasWebView.handleGoBack()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_assignment_details, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        canvasWebView.addVideoClient(activity)
        setListeners()
    }

    override fun applyTheme() {}

    private fun setListeners() {
        notificationTextDismiss.setOnClickListener {
            val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            fadeOut.fillAfter = true
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    notificationTextContainer.visibility = View.GONE
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            notificationTextContainer.startAnimation(fadeOut)
        }

        canvasWebView.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
            override fun openMediaFromWebView(mime: String, url: String, filename: String) {
                openMedia(mime, url, filename)
            }

            override fun onPageFinishedCallback(webView: WebView, url: String) {}

            override fun onPageStartedCallback(webView: WebView, url: String) {}

            override fun canRouteInternallyDelegate(url: String): Boolean {
                return RouterUtils.canRouteInternally(null, url, ApiPrefs.domain, false)
            }

            override fun routeInternallyCallback(url: String) {
                RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, true)
            }
        }

        canvasWebView.canvasEmbeddedWebViewCallback = object : CanvasWebView.CanvasEmbeddedWebViewCallback {
            override fun launchInternalWebViewFragment(url: String) {
                InternalWebviewFragment.loadInternalWebView(activity, activity as Navigation, InternalWebviewFragment.createBundle(canvasContext, url, false))
            }

            override fun shouldLaunchInternalWebViewFragment(url: String): Boolean {
                return true
            }
        }
    }

    override fun getParamForBookmark(): HashMap<String, String> {
        if (assignment == null) {
            return super.getParamForBookmark()
        }
        val map = super.getParamForBookmark()
        map.put(Param.ASSIGNMENT_ID, java.lang.Long.toString(assignment!!.id))
        return map
    }


    /**
     * Updates each view with its corresponding assignment data.
     * @param assignment
     */
    private fun populateAssignmentDetails(assignment: Assignment?, isWithinAnotherCallback: Boolean, isCached: Boolean) {
        //Make sure we have all of the data.
        if (assignment == null) {
            return
        }

        textViewAssignmentTitle.text = assignment.name

        // Due Date
        if (assignment.dueAt != null) {
            val dueDate = DateHelper.createPrefixedDateTimeString(context, R.string.assignmentDue, assignment.dueAt)
            textViewDueDate.visibility = View.VISIBLE
            textViewDueDate.setTypeface(null, Typeface.ITALIC)
            textViewDueDate.text = dueDate

        } else {
            textViewDueDate.visibility = View.GONE
        }

        // Submission Type
        if (assignment.submissionTypes.contains(Assignment.SUBMISSION_TYPE.NONE)) {
            textViewSubmissionDate.visibility = View.INVISIBLE
        } else {
            textViewSubmissionDate.visibility = View.VISIBLE
            if (assignment.submission != null) {
                updateSubmissionDate(assignment.submission.submittedAt)
            }
        }
        pointsPossible.text = "" + assignment.pointsPossible

        populateWebView(assignment)

        //This check is to prevent the context from becoming null when assignment items are
        //clicked rapidly in the notification list.
        if (context != null) {
            if (assignment.gradingType != null) {
                gradingType!!.text = Assignment.gradingTypeToPrettyPrintString(assignment.gradingType, context)
            } else {
                gradingType!!.visibility = View.INVISIBLE
            }

            val assignmentTurnInType = assignment.turnInType

            if (assignmentTurnInType != null) {
                submissionTypeSelected.text = Assignment.turnInTypeToPrettyPrintString(assignmentTurnInType, context)
            }


            //Make sure there are no children views
            onlineSubmissionTypes.removeAllViews()

            if (assignmentTurnInType == Assignment.TURN_IN_TYPE.ONLINE) {
                for (submissionType in assignment.submissionTypes) {
                    val submissionTypeTextView = TextView(context)
                    submissionTypeTextView.setPadding(0, ViewUtils.convertDipsToPixels(5f, context).toInt(), 0, 0)

                    submissionTypeTextView.text = Assignment.submissionTypeToPrettyPrintString(submissionType, context)

                    onlineSubmissionTypes.addView(submissionTypeTextView)
                }
            }
        }
    }

    private fun populateWebView(assignment: Assignment) {
        var description: String?
        if (assignment.isLocked) {
            description = LockInfoHTMLHelper.getLockedInfoHTML(assignment.lockInfo, activity, R.string.lockedAssignmentDesc, R.string.lockedAssignmentDescLine2)
        } else if (assignment.lockAt != null && assignment.lockAt!!.before(Calendar.getInstance(Locale.getDefault()).time)) {
            //if an assignment has an available from and until field and it has expired (the current date is after "until" it will have a lock explanation,
            //but no lock info because it isn't locked as part of a module
            description = assignment.lockExplanation
        } else {
            description = assignment.description
        }

        if (description == null || description == "null" || description == "") {
            description = "<p>" + getString(R.string.noDescription) + "</p>"
        }
        canvasWebView.formatHTML(description, assignment.name)
    }

    fun setAssignmentWithNotification(assignment: Assignment?, message: String?, isWithinAnotherCallback: Boolean, isCached: Boolean) {
        var message = message
        if (assignment == null) {
            return
        }

        if (message != null) {
            message = message.trim { it <= ' ' }
        }

        if (!TextUtils.isEmpty(message)) {

            // get rid of "________________________________________ You received this email..." text
            val index = message!!.indexOf("________________________________________")
            if (index > 0) {
                message = message.substring(0, index)
            }

            notificationText.text = message.trim { it <= ' ' }
            notificationText.movementMethod = LinkMovementMethod.getInstance()
            notificationText.visibility = View.VISIBLE
            notificationTextContainer.visibility = View.VISIBLE
        }
    }

    override fun allowBookmarking(): Boolean {
        return false
    }

    companion object {



        val tabTitle: Int
            get() = R.string.assignmentTabDetails
    }
}
