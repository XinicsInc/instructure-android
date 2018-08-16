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

package com.instructure.candroid.fragment

import android.annotation.TargetApi
import android.app.DialogFragment
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.instructure.candroid.R
import com.instructure.candroid.activity.BookmarkShortcutActivity
import com.instructure.candroid.adapter.BookmarkRecyclerAdapter
import com.instructure.candroid.decorations.DividerDecoration
import com.instructure.candroid.interfaces.BookmarkAdapterToFragmentCallback
import com.instructure.candroid.util.Analytics
import com.instructure.candroid.util.CacheControlFlags
import com.instructure.candroid.util.ShortcutUtils
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.BookmarkManager
import com.instructure.canvasapi2.models.Bookmark
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandarecycler.PandaRecyclerView
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_bookmarks_fragment.*
import kotlin.properties.Delegates

class BookmarksFragment : ParentFragment() {

    private var bookmarkSelectedCallback: (Bookmark) -> Unit by Delegates.notNull()

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        return FragmentInteractions.Placement.FULLSCREEN
    }

    override fun title(): String {
        return getString(R.string.bookmarks)
    }

    private var mRecyclerAdapter: BookmarkRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.LightStatusBarDialog)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater?.inflate(R.layout.fragment_bookmarks_fragment, container, false)
        val recyclerView = rootView?.findViewById<View>(R.id.listView) as PandaRecyclerView
        configureRecyclerAdapter()
        configureRecyclerView(rootView, context, mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView, R.string.no_bookmarks)
        recyclerView.addItemDecoration(DividerDecoration(context))
        recyclerView.isSelectionEnabled = false
        return rootView
    }

    private fun configureRecyclerAdapter() {
        if (mRecyclerAdapter == null) {
            val isShortcutActivity = activity is BookmarkShortcutActivity
            mRecyclerAdapter = BookmarkRecyclerAdapter(context, isShortcutActivity, object : BookmarkAdapterToFragmentCallback<Bookmark> {
                override fun onRowClicked(bookmark: Bookmark, position: Int, isOpenDetail: Boolean) {
                    bookmarkSelectedCallback(bookmark)
                    dismiss()
                }

                override fun onRefreshFinished() {
                    setRefreshing(false)
                }

                override fun onOverflowClicked(bookmark: Bookmark, position: Int, v: View) {
                    //Log to GA
                    val popup = PopupMenu(context, v)
                    //This is not done via menu-v26 because support for adding shortcuts in this way may not be supported by the Launcher.
                    val menuId = if(isShortcutAddingSupported()) R.menu.bookmark_add_shortcut_edit_delete else R.menu.bookmark_edit_delete
                    popup.menuInflater.inflate(menuId, popup.menu)

                    popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_add_to_homescreen -> {
                                Analytics.trackButtonPressed(activity, "Bookmark shortcut creation", null)
                                ShortcutUtils.generateShortcut(context, bookmark)
                                return@OnMenuItemClickListener true
                            }
                            R.id.menu_edit -> {
                                //Log to GA
                                editBookmark(bookmark)
                                return@OnMenuItemClickListener true
                            }
                            R.id.menu_delete -> {
                                //Log to GA
                                deleteBookmark(bookmark)
                                return@OnMenuItemClickListener true
                            }
                        }
                        false
                    })
                    popup.show()
                }
            })
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        applyTheme()
    }

    override fun applyTheme() {
        val isShortcutActivity = activity is BookmarkShortcutActivity
        if(isShortcutActivity) toolbar.title = getString(R.string.bookmarkShortcut) else title()
        if(isShortcutActivity) toolbar.setupAsCloseButton { activity?.finish() }
        else toolbar.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK, false)
    }

    override fun allowBookmarking(): Boolean {
        return false
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun isShortcutAddingSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            return shortcutManager?.isRequestPinShortcutSupported == true
        }
        return false
    }

    private fun editBookmark(bookmark: Bookmark) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_bookmark, null)
        val editText = view.findViewById<AppCompatEditText>(R.id.bookmarkEditText)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.bookmarkEdit)
        builder.setView(view)
        builder.setPositiveButton(R.string.done, { _, _ ->
            val text = editText.text.toString()
            if (text.isNotBlank()) {
                val bookmarkCopy = copyBookmark(bookmark)
                bookmarkCopy.name = text
                BookmarkManager.updateBookmark(bookmarkCopy, object : StatusCallback<Bookmark>() {
                    override fun onResponse(response: retrofit2.Response<Bookmark>, linkHeaders: LinkHeaders, type: ApiType) {
                        if (response.code() == 200 && apiCheck()) {
                            CacheControlFlags.forceRefreshBookmarks = true
                            val newBookmark = response.body()
                            newBookmark?.courseId = bookmark.courseId
                            mRecyclerAdapter?.add(newBookmark)
                            showToast(R.string.bookmarkUpdated)
                        }
                    }
                })
                view.hideKeyboard()
            } else {
                showToast(R.string.bookmarkTitleRequired)
            }
        })
        builder.setNegativeButton(android.R.string.cancel, { _, _ -> view.hideKeyboard() })
        val dialog = builder.create()

        ViewStyler.themeEditText(context, editText, ThemePrefs.brandColor)
        editText.setText(bookmark.name)
        editText.setSelection(editText.text.length)

        dialog.show()
    }

    private fun copyBookmark(bookmark: Bookmark): Bookmark {
        val bookmarkCopy = Bookmark()
        bookmarkCopy.courseId = bookmark.courseId
        bookmarkCopy.name = bookmark.name
        bookmarkCopy.url = bookmark.url
        bookmarkCopy.id = bookmark.id
        bookmarkCopy.position = bookmark.position
        return bookmarkCopy
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.bookmarkDelete)
        builder.setMessage(bookmark.name)
        builder.setPositiveButton(android.R.string.yes, { _, _ ->
            BookmarkManager.deleteBookmark(bookmark.id, object : StatusCallback<Bookmark>() {
                override fun onResponse(response: retrofit2.Response<Bookmark>, linkHeaders: LinkHeaders, type: ApiType) {
                    if (apiCheck() && response.code() == 200) {
                        CacheControlFlags.forceRefreshBookmarks = true
                        mRecyclerAdapter?.remove(bookmark)
                        showToast(R.string.bookmarkDeleted)
                    }
                }

                override fun onFinished(type: ApiType) {
                    mRecyclerAdapter?.onCallbackFinished()
                }
            })
        })

        builder.setNegativeButton(android.R.string.no, null)
        val dialog = builder.create()
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        if(!isTablet) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    companion object {
        fun newInstance(callback: (Bookmark) -> Unit) : BookmarksFragment {
            val fragment = BookmarksFragment()
            fragment.apply {
                bookmarkSelectedCallback = callback
            }
            return fragment
        }
    }
}
