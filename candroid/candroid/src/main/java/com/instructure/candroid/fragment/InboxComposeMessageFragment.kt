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
import android.text.TextUtils
import android.text.util.Rfc822Tokenizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.Toast
import com.android.ex.chips.RecipientEntry
import com.instructure.candroid.R
import com.instructure.candroid.adapter.CanvasContextSpinnerAdapter
import com.instructure.candroid.adapter.NothingSelectedSpinnerAdapter
import com.instructure.candroid.adapter.RecipientAdapter
import com.instructure.candroid.dialog.UnsavedChangesExitDialog
import com.instructure.candroid.events.ChooseRecipientsEvent
import com.instructure.candroid.events.ConversationUpdatedEvent
import com.instructure.candroid.events.MessageAddedEvent
import com.instructure.candroid.util.FragUtils
import com.instructure.candroid.view.AttachmentView
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.managers.GroupManager
import com.instructure.canvasapi2.managers.InboxManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.isValid
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.pandautils.services.FileUploadService
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_inbox_compose_message.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class InboxComposeMessageFragment : ParentFragment() {

    private val conversation: Conversation? by lazy { arguments.getParcelable<Conversation>(Const.CONVERSATION) }
    private val participants by lazy { arguments.getParcelableArrayList<BasicUser>(PARTICIPANTS) }
    private val includedMessageIds by lazy { arguments.getLongArray(Const.MESSAGE) }
    private val isReply by lazy { arguments.getBoolean(IS_REPLY, false) }
    private val currentMessage: Message? by lazy { arguments.getParcelable<Message>(Const.MESSAGE_TO_USER) }

    private var selectedContext: CanvasContext? = null
    private val isNewMessage by lazy { conversation == null }
    private val attachments = mutableListOf<Attachment>()
    private val chipsAdapter: RecipientAdapter by lazy { RecipientAdapter(context) }

    private var sendIndividually = false
    private var shouldAllowExit = false

    private var coursesCall: WeaveJob? = null
    private var sendCall: WeaveJob? = null

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        coursesCall?.cancel()
        sendCall?.cancel()
        super.onDestroy()
    }

    override fun title(): String = getString(R.string.composeMessage)

    override fun allowBookmarking() = false

    override fun applyTheme() {
        ColorUtils.colorIt(ThemePrefs.buttonColor, contactsImageButton)
        ViewStyler.themeSwitch(context, sendIndividualSwitch, ThemePrefs.brandColor)
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.themeProgressBar(savingProgressBar, Color.BLACK)
    }

    private fun validateMessage() = when {
        isNewMessage && selectedContext == null -> {
            toast(R.string.noCourseSelected)
            false
        }
        recipientsView.selectedRecipients.size == 0 -> {
            toast(R.string.noRecipients)
            false
        }
        TextUtils.getTrimmedLength(message.text) == 0 -> {
            toast(R.string.emptyMessage)
            false
        }
        else -> true
    }

    private fun getRecipientsFromEntries() = recipientsView.selectedRecipients.map {
        Recipient().apply {
            avatarURL = it.avatarUrl
            name = it.name
            stringId = it.destination
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedContext = arguments.getParcelable(Const.CANVAS_CONTEXT)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_inbox_compose_message, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isNewMessage) {
            // Composing a new message
            spinnerWrapper.setVisible()
            recipientWrapper.setGone()
            subjectView.setGone()
            editSubject.setVisible()
            sendIndividualMessageWrapper.setVisible()
            sendIndividualSwitch.setOnCheckedChangeListener { _, isChecked -> sendIndividually = isChecked }
        } else {
            recipientsView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (isReply) {
                        addInitialRecipients(currentMessage?.participatingUserIds ?: conversation?.audience ?: emptyList())
                    }
                    recipientsView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }

        setupToolbar()
        setupViews()
    }

    private fun setupViews() {
        // Set conversation subject
        conversation?.let { subjectView.text = it.subject }

        // Set up recipients view
        recipientsView.setTokenizer(Rfc822Tokenizer())
        recipientsView.setAdapter<RecipientAdapter>(chipsAdapter)
        if (selectedContext != null) {
            courseWasSelected()
            chipsAdapter.canvasRecipientManager.canvasContext = selectedContext
        }

        // Don't show the contacts button if there is no selected course
        contactsImageButton.setVisible(selectedContext != null || conversation?.contextCode.isValid())
        contactsImageButton.onClick {
            if (selectedContext == null && conversation == null) {
                toast(R.string.noCourseSelected)
            } else {
                val canvasContext =
                    selectedContext ?: CanvasContext.fromContextCode(conversation?.contextCode) ?: return@onClick
                val args = InboxRecipientsFragment.createBundle(canvasContext, getRecipientsFromEntries())
                navigation?.addFragment(FragUtils.getFrag(InboxRecipientsFragment::class.java, args))
            }
        }

        // Ensure attachments are up to date
        refreshAttachments()

        // Get courses and groups if this is a new compose message
        if (isNewMessage) getAllCoursesAndGroups()
    }

    private fun getAllCoursesAndGroups() {
        coursesCall = weave {
            try {
                val (courses, groups) = awaitApis<List<Course>, List<Group>>(
                        { CourseManager.getAllFavoriteCourses(true, it) },
                        { GroupManager.getFavoriteGroups(it, true) }
                )
                addCoursesAndGroups(courses, groups)
            } catch (ignore: Throwable) {
            }
        }
    }

    private fun addCoursesAndGroups(courses: List<Course>, groups: List<Group>) {
        val adapter = CanvasContextSpinnerAdapter.newAdapterInstance(context, courses, groups)
        courseSpinner.adapter = NothingSelectedSpinnerAdapter(adapter, R.layout.spinner_item_nothing_selected, context)
        if (selectedContext != null) {
            courseSpinner.onItemSelectedListener = null // Prevent listener from firing the when selection is placed
            courseSpinner.setSelection(adapter.getPosition(selectedContext) + 1, false) // + 1 is for the nothingSelected position
            courseWasSelected()
        }
        courseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position != 0) { // Position zero is nothingSelected prompt
                    val canvasContext = adapter.getItem(position - 1) // -1 to account for nothingSelected item
                    if (selectedContext == null || selectedContext!!.id != canvasContext.id) {
                        recipientsView.removeAllRecipientEntry()
                        selectedContext = canvasContext
                        courseWasSelected()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>)  = Unit
        }
    }

    private fun courseWasSelected() {
        recipientWrapper.visibility = View.VISIBLE
        contactsImageButton.visibility = View.VISIBLE
        activity.invalidateOptionsMenu()
        chipsAdapter.canvasRecipientManager.canvasContext = selectedContext
    }

    private fun setupToolbar() {
        toolbar.setTitle(when {
            isNewMessage -> R.string.newMessage
            isReply -> R.string.reply
            else -> R.string.forwardMessage
        })

        if (toolbar.menu.size() == 0) toolbar.inflateMenu(R.menu.menu_add_message)
        toolbar.menu.findItem(R.id.menu_attachment).isVisible = true
        toolbar.setOnMenuItemClickListener { item ->
            when(item.itemId) {
                R.id.menu_send -> {
                    sendMessage()
                }
                R.id.menu_attachment -> {
                    val bundle = UploadFilesDialog.createMessageAttachmentsBundle(arrayListOf())
                    UploadFilesDialog.show(fragmentManager, bundle, { _ -> })
                }
                else -> return@setOnMenuItemClickListener false
            }
            true
        }

        toolbar.setupAsBackButton(this)
    }

    private fun handleExit() {
        // Check to see if the user has made any changes
        if (editSubject.text.isNotBlank() || message.text.isNotBlank() || attachments.isNotEmpty()) {
            shouldAllowExit = false
            UnsavedChangesExitDialog.Companion.show(activity.supportFragmentManager) {
                shouldAllowExit = true
                activity.onBackPressed()
            }
        } else {
            shouldAllowExit = true
            activity.onBackPressed()
        }
    }

    override fun handleBackPressed(): Boolean {
        // See if they have unsent changes
        if (!shouldAllowExit) {
            handleExit()
            return true
        }
        return super.handleBackPressed()
    }

    private fun messageSuccess(conversation: Conversation) {
        toast(R.string.messageSentSuccessfully)
        shouldAllowExit = true
        EventBus.getDefault().postSticky(MessageAddedEvent(true, null))
        EventBus.getDefault().postSticky(ConversationUpdatedEvent(conversation))
        activity.onBackPressed()
    }

    private fun messageFailure() {
        toolbar.menu.findItem(R.id.menu_send).isVisible = true
        toolbar.menu.findItem(R.id.menu_attachment).isVisible = true
        savingProgressBar.visibility = View.GONE
        toast(R.string.errorSendingMessage)
    }

    private fun refreshAttachments() {
        attachmentLayout.setPendingAttachments(attachments, true) { action, attachment ->
            if (action == AttachmentView.AttachmentAction.REMOVE) {
                attachments -= attachment
            }
        }
    }

    internal fun sendMessage() {
        // Validate inputs
        if (!validateMessage()) return

        // Ensure network is available
        if (!APIHelper.hasNetworkConnection()) {
            Toast.makeText(context, this@InboxComposeMessageFragment.getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show()
            return
        }

        // Can't send a message to yourself. Canvas throws a 400, but canvas lets you select yourself as part of a list of people
        if (recipientsView.selectedRecipients.size == 1 && recipientsView.selectedRecipients[0].id == ApiPrefs.user!!.id) {
            toast(R.string.addMorePeopleToMessage)
            return
        }

        // Make the progress bar visible and the other buttons not there so they can't try to re-send the message multiple times
        toolbar.menu.findItem(R.id.menu_send).isVisible = false
        toolbar.menu.findItem(R.id.menu_attachment).isVisible = false
        savingProgressBar.announceForAccessibility(getString(R.string.sending))
        savingProgressBar.visibility = View.VISIBLE

        // Send message
        if (isNewMessage) {
            val isBulk = (recipientsView.selectedRecipients.size > 1 && sendIndividually)
                    || recipientsView.selectedRecipients.any { it.userCount > 1 && sendIndividually }
            val contextId = selectedContext!!.contextId
            val subject = editSubject.text.toString()
            // isBulk controls the group vs individual messages, so group message flag is hardcoded to true at the api call
            createConversation(recipientsView.selectedRecipients, message.text.toString(), subject, contextId, isBulk)
        } else {
            sendMessage(recipientsView.selectedRecipients, message.text.toString())
        }
    }

    private fun sendMessage(selectedRecipients: List<RecipientEntry>, message: String) {
        sendCall = tryWeave {
            val attachmentIds = attachments.map { it.id }.toLongArray()
            val recipientIds = selectedRecipients.map { it.destination }
            val conversation = awaitApi<Conversation> {
                InboxManager.addMessage(conversation?.id ?: 0, message, recipientIds, includedMessageIds, attachmentIds, it)
            }
            messageSuccess(conversation)
        } catch {
            it.cause?.printStackTrace()
            messageFailure()
        }
    }

    private fun createConversation(selectedRecipients: List<RecipientEntry>, message: String, subject: String, contextId: String, isBulk: Boolean) {
        sendCall?.cancel()
        sendCall = tryWeave {
            val attachmentIds = attachments.map { it.id }.toLongArray()
            val recipientIds = selectedRecipients.map { it.destination }
            val conversation = awaitApi<List<Conversation>> {
                InboxManager.createConversation(recipientIds, message, subject, contextId, attachmentIds, isBulk, it)
            }.first()
            messageSuccess(conversation)
        } catch {
            it.cause?.printStackTrace()
            messageFailure()
        }
    }

    private fun addInitialRecipients(initialRecipientIds: List<Long>) {
        val selectedRecipients = recipientsView.selectedRecipients
        val myId = ApiPrefs.user!!.id
        initialRecipientIds
                // Map IDs to participants (excluding the current user)
                .mapNotNull { id -> participants.find { it.id == id && it.id != myId } }
                // Filter out already-added participants
                .filter { participant -> selectedRecipients.none { it.destination == participant.id.toString() } }
                // Add new recipients
                .forEach {
                    val recipientEntry = RecipientEntry(it.id, it.name, it.id.toString(), "", it.avatarUrl, 0, 0, true, null, null)
                    this.recipientsView.appendRecipientEntry(recipientEntry)
                }
    }

    private fun addRecipients(newRecipients: List<Recipient>) {
        val selectedRecipients = recipientsView.selectedRecipients
        recipientsView.setTokenizer(Rfc822Tokenizer())
        if (recipientsView.adapter == null) {
            recipientsView.setAdapter<RecipientAdapter>(chipsAdapter)
        }
        newRecipients
                // Filter out already-added participants
                .filter { recipient -> selectedRecipients.none { it.destination == recipient.stringId } }
                // Add new recipients
                .forEach {
                    val recipientEntry = RecipientEntry(it.idAsLong, it.name, it.stringId, "", it.avatarURL, 0, 0, true, it.commonCourses?.keys, it.commonGroups?.keys)
                    this.recipientsView.appendRecipientEntry(recipientEntry)
                }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onRecipientsChosen(event: ChooseRecipientsEvent) {
        event.once(javaClass.simpleName, { recipients ->
            // Need to have the TextView laid out first so that the chips view will have a width
            recipientsView.post { addRecipients(recipients) }
        })
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFileUploadedEvent(event: FileUploadEvent) {
        event.get {
            event.remove()
            if(it.intent?.action == FileUploadService.ALL_UPLOADS_COMPLETED) {
                it.attachments.forEach {
                    attachments += it
                }
                refreshAttachments()
            }
        }
    }

    companion object {

        private const val IS_REPLY = "is_reply"
        private const val PARTICIPANTS = "participants"

        @JvmStatic
        fun createBundleReplyForward(
            isReply: Boolean,
            conversation: Conversation,
            participants: List<BasicUser>,
            includedMessageIds: LongArray,
            currentMessage: Message?
        ) = Bundle().apply {
            putBoolean(IS_REPLY, isReply)
            putParcelable(Const.CONVERSATION, conversation)
            putParcelableArrayList(PARTICIPANTS, ArrayList(participants))
            putLongArray(Const.MESSAGE, includedMessageIds)
            putParcelable(Const.MESSAGE_TO_USER, currentMessage)
        }

        @JvmStatic
        fun createBundleNewConversation() = Bundle()

        @JvmStatic
        fun createBundleNewConversation(
            participants: ArrayList<BasicUser>,
            canvasContext: CanvasContext
        ) = Bundle().apply {
            putParcelableArrayList(PARTICIPANTS, participants)
            putParcelable(Const.CANVAS_CONTEXT, canvasContext)
        }

        @JvmStatic
        fun newInstance(bundle: Bundle): InboxComposeMessageFragment {
            val addMessageActivity = InboxComposeMessageFragment()
            addMessageActivity.arguments = bundle
            return addMessageActivity
        }
    }
}
