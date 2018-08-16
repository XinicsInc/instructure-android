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
package com.instructure.candroid.activity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.instructure.candroid.R
import com.instructure.candroid.fragment.BookmarksFragment
import com.instructure.candroid.util.Analytics
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.models.Bookmark
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.pandautils.utils.ColorUtils
import com.instructure.pandautils.utils.Const
import com.instructure.candroid.util.ShortcutUtils

class BookmarkShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_shortcuts)

        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.container, BookmarksFragment.newInstance { bookmarkSelected(it) }, BookmarksFragment::class.java.simpleName)
        ft.commitAllowingStateLoss()
    }

    @Suppress("DEPRECATION")
    private fun bookmarkSelected(bookmark: Bookmark) {

        Analytics.trackButtonPressed(this, "Bookmark Selected", null)

        val successful = ShortcutUtils.generateShortcut(this, bookmark)
        if(successful) {
            finish()
            return
        }

        val launchIntent = Intent(this, LoginActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        launchIntent.putExtra(Const.BOOKMARK, bookmark.name)
        launchIntent.putExtra(Const.URL, bookmark.url)

        val color = ColorKeeper.getOrGenerateColor(RouterUtils.getContextIdFromURL(bookmark.url))
        val options = BitmapFactory.Options()
        options.inMutable = true
        val bitIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_bookmark, options)


        val shortcutIntent = Intent()

        //Deprecated values are handled above with ShortcutUtils.generateShortcut.
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, bookmark.name)
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, ColorUtils.colorIt(color, bitIcon))
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)
        setResult(RESULT_OK, shortcutIntent)
        finish()
    }
}
