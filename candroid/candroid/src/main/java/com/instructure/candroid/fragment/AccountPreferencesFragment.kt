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

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import com.instructure.candroid.R
import com.instructure.candroid.activity.LoginActivity
import com.instructure.candroid.util.StudentPrefs
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.pandautils.utils.*
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_account_preferences.*
import kotlinx.android.synthetic.main.settings_spinner.view.*

@PageView(url = "profile/settings")
class AccountPreferencesFragment : ParentFragment() {

    private var mLanguageListener: AdapterView.OnItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>) = Unit
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            restartAppForLocale(position)
        }
    }

    override fun allowBookmarking(): Boolean = false

    override fun getFragmentPlacement() = FragmentInteractions.Placement.DETAIL

    override fun title(): String = getString(R.string.accountPreferences)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_account_preferences, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyTheme()
        setupViews()
    }

    override fun applyTheme() {
        toolbar.title = title()
        toolbar.setupAsBackButton(this)
        ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK, false)
        ViewStyler.themeSwitch(context, mondayToggle, ThemePrefs.brandColor)
    }

    private fun setupViews() {
        if(Build.VERSION.SDK_INT >= 26) {
            languageContainer.setGone()
        } else {
            languageSpinner.post {
                var language = StudentPrefs.languageIndex
                if (language >= resources.getStringArray(R.array.supported_languages).size) {
                    language = 0
                    StudentPrefs.languageIndex = 0
                }
                languageSpinner.setSelection(language, false)
                languageSpinner.post { languageSpinner.onItemSelectedListener = mLanguageListener }
            }
            languageSpinner.adapter = SettingsSpinnerAdapter(activity, resources.getStringArray(R.array.supported_languages))
        }

        mondayToggle.isChecked = StudentPrefs.weekStartsOnMonday
        mondayToggle.setOnCheckedChangeListener { _, isChecked -> StudentPrefs.weekStartsOnMonday = isChecked }
    }

    private inner class SettingsSpinnerAdapter<String>(context: Context, val items: Array<String>) : BaseAdapter() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): String = items[position]

        override fun getItemId(position: Int): Long = 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View = convertView ?: inflater.inflate(R.layout.settings_spinner, parent, false)
            view.indicator.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.vd_expand, Color.WHITE))
            view.indicator.setColorFilter(ContextCompat.getColor(context, R.color.defaultTextGray))
            view.title.text = getItem(position).toString()
            return view
        }

        override fun getDropDownView(position: Int, view: View?, parent: ViewGroup): View {
            val v = inflater.inflate(R.layout.settings_spinner_item, parent, false)
            v.title.text = getItem(position).toString()
            return v
        }
    }

    private fun restartAppForLocale(position: Int) {
        AlertDialog.Builder(context)
                .setTitle(R.string.restartingCanvas)
                .setMessage(if (position == 0) R.string.defaultLanguageWarning else R.string.languageDialogText)
                .setPositiveButton(R.string.yes) { _, _ ->
                    // Set the language
                    StudentPrefs.languageIndex = position
                    // Restart the App to apply language after a short delay to guarantee shared prefs is saved
                    Handler().postDelayed({ restartApp() }, 500)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    // If they select no, we want to reselect the language they had originally
                    val language = StudentPrefs.languageIndex
                    // Null out listener before re-selecting original
                    languageSpinner.onItemSelectedListener = null
                    languageSpinner.setSelection(language, false)
                    languageSpinner.post { languageSpinner.onItemSelectedListener = mLanguageListener }
                }
                .setCancelable(false)
                .show()
    }

    private fun restartApp() {
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(Const.LANGUAGES_PENDING_INTENT_KEY, Const.LANGUAGES_PENDING_INTENT_ID)
        val mPendingIntent = PendingIntent.getActivity(context, Const.LANGUAGES_PENDING_INTENT_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent)
        System.exit(0)
    }
}
