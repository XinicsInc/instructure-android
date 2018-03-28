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
package com.instructure.candroid.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.AppCompatEditText
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.fragment.ToDoListFragment
import com.instructure.candroid.util.Analytics
import com.instructure.candroid.util.CacheControlFlags
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.managers.BookmarkManager
import com.instructure.canvasapi2.models.Bookmark
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Group
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.color
import kotlinx.coroutines.experimental.Job

class BookmarkCreationDialog : AppCompatDialogFragment() {

    private var bookmarkJob: Job? = null
    private var bookmarkEditText: AppCompatEditText? = null

    init {
        retainInstance = true
    }

    companion object {
        private val BOOKMARK_CANVAS_CONTEXT = "bookmarkCanvasContext"
        private val BOOKMARK_URL = "bookmarkUrl"
        private val BOOKMARK_LABEL = "bookmarkLabel"

        private fun newInstance(canvasContext: CanvasContext, bookmarkUrl: String, label: String? = ""): BookmarkCreationDialog {
            val dialog = BookmarkCreationDialog()
            val args = Bundle()
            args.putParcelable(BOOKMARK_CANVAS_CONTEXT, canvasContext)
            args.putString(BOOKMARK_URL, bookmarkUrl)
            args.putString(BOOKMARK_LABEL, label)
            dialog.arguments = args
            return dialog
        }

        @JvmStatic
        fun <F> newInstance(context: Activity, topFragment: F?, peakingFragment: F?): BookmarkCreationDialog? where F : FragmentInteractions {
            //A to-do route doesn't actually exist so we will create a single fragment route by making the peeking null
            val isTodoList = peakingFragment is ToDoListFragment
            val label: String? = null
            var bookmarkUrl: String? = null

            val canvasContext = topFragment?.canvasContext

            if (!isTodoList && peakingFragment != null && topFragment != null
                    && peakingFragment.getFragmentPlacement() == FragmentInteractions.Placement.MASTER
                    && (topFragment.getFragmentPlacement() == FragmentInteractions.Placement.DETAIL
                    || topFragment.getFragmentPlacement() == FragmentInteractions.Placement.DIALOG)) {

                //Master & Detail
                if (canvasContext is Course || canvasContext is Group) {
                    bookmarkUrl = RouterUtils.createUrl(canvasContext.type, peakingFragment::class.java, topFragment::class.java, topFragment.getParamForBookmark(), topFragment.getQueryParamForBookmark())
                    Analytics.trackBookmarkSelected(context, peakingFragment::class.java.simpleName + " " + topFragment::class.java.simpleName)
                }
            } else if (topFragment != null) {
                //Master || Detail

                if (canvasContext is Course || canvasContext is Group) {
                    bookmarkUrl = RouterUtils.createUrl(canvasContext.type, topFragment::class.java, topFragment.getParamForBookmark())
                    Analytics.trackBookmarkSelected(context, topFragment::class.java.simpleName)
                }
            }

            if(canvasContext != null && bookmarkUrl != null) {
                Analytics.trackButtonPressed(context, "Add bookmark to fragment", null)
                return newInstance(canvasContext, bookmarkUrl, label)
            }

            return null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val view = View.inflate(ContextThemeWrapper(activity, 0), R.layout.dialog_bookmark, null)
        setupViews(view)
        builder.setView(view)
        builder.setTitle(R.string.addBookmark)
        builder.setCancelable(true)
        builder.setPositiveButton(R.string.save, null)
        builder.setNegativeButton(android.R.string.cancel, null)
        val buttonColor = arguments.getParcelable<CanvasContext>(BOOKMARK_CANVAS_CONTEXT).color
        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton.setTextColor(buttonColor)
            positiveButton.setOnClickListener {
                closeKeyboard()
                saveBookmark()
            }
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(buttonColor)
        }
        return dialog
    }

    private fun setupViews(view: View) {
        bookmarkEditText = view.findViewById(R.id.bookmarkEditText)
        bookmarkEditText?.let {
            ViewStyler.themeEditText(context, it, arguments.getParcelable<CanvasContext>(BOOKMARK_CANVAS_CONTEXT).color)
            it.setText(arguments.getString(BOOKMARK_LABEL, ""))
            it.setSelection(it.text.length)
        }
    }

    private fun saveBookmark() {
        val label = bookmarkEditText!!.text.toString()
        if(label.isBlank()) {
            Toast.makeText(activity, R.string.bookmarkTitleRequired, Toast.LENGTH_SHORT).show()
            return
        }

        bookmarkJob = tryWeave {
            awaitApi<Bookmark> { BookmarkManager.createBookmark(Bookmark(label, arguments.getString(BOOKMARK_URL), 0), it) }
            Analytics.trackBookmarkCreated(activity)
            Toast.makeText(activity, R.string.bookmarkAddedSuccess, Toast.LENGTH_SHORT).show()
            CacheControlFlags.forceRefreshBookmarks = true
            dismiss()
        } catch {
            Toast.makeText(context, R.string.bookmarkAddedFailure, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun closeKeyboard() {
        //close the keyboard
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if(bookmarkEditText != null) {
            inputManager?.hideSoftInputFromWindow(bookmarkEditText!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    override fun onDestroyView() {
        val dialog = dialog
        // handles https://code.google.com/p/android/issues/detail?id=17423
        if (dialog != null && retainInstance) dialog.setDismissMessage(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        bookmarkJob?.cancel()
    }
}