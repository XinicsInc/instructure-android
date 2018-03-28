package com.instructure.teacher.ui.pages

import com.instructure.soseedy.Discussion
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.OnViewWithId
import com.instructure.teacher.ui.utils.assertDisplayed
import com.instructure.teacher.ui.utils.click
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert
import com.instructure.teacher.ui.utils.waitForViewWithText

class DiscussionsListPage : BasePage(), PageAssert by SimplePageAssert() {

    private val discussionListToolbar by OnViewWithId(R.id.discussionListToolbar)
    private val discussionsFAB by OnViewWithId(R.id.createNewDiscussion)
    private val discussionsRecyclerView by OnViewWithId(R.id.discussionRecyclerView)

    fun clickDiscussion(discussion: Discussion) {
        waitForViewWithText(discussion.title).click()
    }

    fun assertHasDiscussion(discussion: Discussion) {
        waitForViewWithText(discussion.title).assertDisplayed()
    }
}
