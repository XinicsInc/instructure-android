package com.instructure.teacher.ui.pages

import com.instructure.soseedy.Conversation
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert

class InboxPage: BasePage(), PageAssert by SimplePageAssert() {

    private val toolbarTitle by WaitForToolbarTitle(R.string.tab_inbox)

    private val inboxRecyclerView by WaitForViewWithId(R.id.inboxRecyclerView)

    private val addMessageFAB by WaitForViewWithId(R.id.addMessage)

    //Only displayed when inbox is empty
    private val emptyPandaView by WaitForViewWithId(R.id.emptyPandaView)
    private val filterText by OnViewWithId(R.id.filterText)

    override fun assertPageObjects() {
        toolbarTitle.assertDisplayed()
    }

    fun assertHasConversation() {
        inboxRecyclerView.check(RecyclerViewItemCountAssertion(1))
    }

    fun clickConversation(conversation: Conversation) {
        waitForViewWithText(conversation.subject).click()
    }

    fun clickAddMessageFAB() {
        addMessageFAB.click()
    }
}
