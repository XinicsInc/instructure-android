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

package com.instructure.candroid.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.instructure.candroid.R
import com.instructure.candroid.dialog.UnsavedChangesExitDialog
import com.instructure.candroid.events.PageUpdatedEvent
import com.instructure.interactions.FragmentInteractions
import com.instructure.canvasapi2.managers.PageManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Page
import com.instructure.canvasapi2.models.post_models.PagePostBody
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_edit_page.*
import org.greenrobot.eventbus.EventBus

class EditPageDetailsFragment : ParentFragment() {

    private var apiJob: WeaveJob? = null

    /* The page to be edited */
    private var page by NullableParcelableArg<Page>()

    private val mSaveMenuButton get() = toolbar.menu.findItem(R.id.menuSavePage)
    private val mSaveButtonTextView: TextView? get() = view?.findViewById(R.id.menuSavePage)


    override fun allowBookmarking() = false

    override fun applyTheme() {

    }

    override fun getFragmentPlacement(): FragmentInteractions.Placement = FragmentInteractions.Placement.DIALOG

    override fun title(): String = getString(R.string.editPage)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater?.inflate(R.layout.fragment_edit_page, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupToolbar()
        setupViews()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuSavePage -> { savePage() }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shouldAllowExit() : Boolean {
        // Check if edited page has changes
        if(page?.pageId != 0L &&
                page?.body ?: "" == pageRCEView?.html) {
            return true
        }
        return false
    }

    private fun setupViews() {
        setupDescription()
    }

    private fun setupDescription() {
        pageRCEView.setHtml(
                page?.body,
                getString(R.string.pageDetails),
                getString(R.string.rce_empty_description),
                ThemePrefs.brandColor, ThemePrefs.buttonColor
        )
        // when the RCE editor has focus we want the label to be darker so it matches the title's functionality
        pageRCEView.setLabel(pageDescLabel, R.color.defaultTextDark, R.color.defaultTextGray)
    }

    private fun setupToolbar() {
        toolbar.setupAsCloseButton {
            if(shouldAllowExit()) {
                activity?.onBackPressed()
            } else {
                UnsavedChangesExitDialog.show(fragmentManager, {
                    activity?.onBackPressed()
                })
            }
        }
        toolbar.title = page?.title
        setupToolbarMenu(toolbar, R.menu.menu_edit_page)

        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
        with(mSaveMenuButton) {
            setIcon(0)
            setTitle(R.string.save)
        }
        mSaveButtonTextView?.setTextColor(ThemePrefs.buttonColor)
    }

    private fun savePage() {

        val description = pageRCEView.html

        onSaveStarted()
        apiJob = tryWeave {
            val postBody = PagePostBody(description, page?.title, page?.isFrontPage == true, page?.editingRoles, page?.isPublished == true)

            val updatedPage = awaitApi<Page> { PageManager.editPage(canvasContext, page?.url ?: "", postBody, it) }
            EventBus.getDefault().post(PageUpdatedEvent(updatedPage))

            onSaveSuccess()
        } catch {
            onSaveError()

        }
    }

    private fun onSaveStarted() {
        mSaveMenuButton.isVisible = false
        savingProgressBar.announceForAccessibility(getString(R.string.saving))
        savingProgressBar.setVisible()
    }


    private fun onSaveError() {
        mSaveMenuButton.isVisible = true
        savingProgressBar.setGone()
        toast(R.string.errorSavingPage)
    }


    private fun onSaveSuccess() {
        toast(R.string.pageSuccessfullyUpdated)

        activity.onBackPressed() // close this fragment
    }

    override fun handleIntentExtras(extras: Bundle?) {
        super.handleIntentExtras(extras)

        page = extras?.getSerializable(Const.PAGE) as Page
    }

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle) = EditPageDetailsFragment().apply {
            arguments = bundle
        }

        @JvmStatic
        fun createBundle(page: Page, canvasContext: CanvasContext): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putSerializable(Const.PAGE, page)
            return extras
        }
    }
}
