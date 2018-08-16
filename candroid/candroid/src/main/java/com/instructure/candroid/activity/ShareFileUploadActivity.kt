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

package com.instructure.candroid.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.ViewTreeObserver
import android.widget.Toast

import com.instructure.candroid.R
import com.instructure.candroid.dialog.ShareFileDestinationDialog
import com.instructure.candroid.util.Analytics
import com.instructure.candroid.util.AnimationHelpers
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.activity_share_file.*
import kotlinx.coroutines.experimental.Job

import java.util.ArrayList

class ShareFileUploadActivity : AppCompatActivity(), ShareFileDestinationDialog.DialogCloseListener {

    private var loadCoursesJob: Job? = null
    private var uploadFileSourceFragment: DialogFragment? = null
    private var courses: ArrayList<Course>? = null

    private var sharedURI: Uri? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_file)
        ViewStyler.setStatusBarDark(this, ContextCompat.getColor(this, R.color.login_studentAppTheme))
        if (checkLoggedIn()) {
            revealBackground()
            Analytics.trackAppFlow(this)
            sharedURI = parseIntentType()
            getCourses()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UploadFilesDialog.CAMERA_PIC_REQUEST ||
                requestCode == UploadFilesDialog.PICK_FILE_FROM_DEVICE ||
                requestCode == UploadFilesDialog.PICK_IMAGE_GALLERY) {
            //File Dialog Fragment will not be notified of onActivityResult(), alert manually
            OnActivityResults(ActivityResult(requestCode, resultCode, data), null).postSticky()
        }
    }

    private fun getCourses() {
        loadCoursesJob = tryWeave {
            val courses = awaitApi<List<Course>> { CourseManager.getCourses(true, it) }
            if (courses.isNotEmpty()) {
                this@ShareFileUploadActivity.courses = ArrayList(courses)
                if (uploadFileSourceFragment == null) showDestinationDialog()
            } else {
                Toast.makeText(applicationContext, R.string.uploadingFromSourceFailed, Toast.LENGTH_LONG).show()
                exitActivity()
            }
        } catch {
            Toast.makeText(this@ShareFileUploadActivity, R.string.uploadingFromSourceFailed, Toast.LENGTH_LONG).show()
            exitActivity()
        }
    }

    private fun revealBackground() {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                AnimationHelpers.removeGlobalLayoutListeners(rootView, this)
                AnimationHelpers.createRevealAnimator(rootView).start()
            }
        })
    }

    private fun checkLoggedIn(): Boolean {
        if (TextUtils.isEmpty(ApiPrefs.token)) {
            exitActivity()
            return false
        } else {
            return true
        }
    }

    private fun exitActivity() {
        val intent = LoginActivity.createIntent(this)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        uploadFileSourceFragment?.dismissAllowingStateLoss()
        super.onBackPressed()
    }

    override fun onDestroy() {
        uploadFileSourceFragment?.dismissAllowingStateLoss()
        loadCoursesJob?.cancel()
        super.onDestroy()
    }

    private fun showDestinationDialog() {
        if (sharedURI == null) {
            Toast.makeText(applicationContext, R.string.uploadingFromSourceFailed, Toast.LENGTH_LONG).show()
        } else {
            uploadFileSourceFragment = ShareFileDestinationDialog.newInstance(ShareFileDestinationDialog.createBundle(sharedURI, courses))
            uploadFileSourceFragment!!.show(supportFragmentManager, ShareFileDestinationDialog.TAG)
        }
    }

    private fun parseIntentType(): Uri? {
        // Get intent, action and MIME type
        val intent = intent
        val action = intent.action
        val type = intent.type

        return if (Intent.ACTION_SEND == action && type != null) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } else null

    }

    override fun onDismiss(dialog: DialogInterface) {
        finish()
    }

    override fun onCancel(dialog: DialogInterface) {
        finish()
    }

    private fun getColor(bundle: Bundle?): Int {
        if(bundle != null && bundle.containsKey(Const.CANVAS_CONTEXT)) {
            val color = ColorKeeper.getOrGenerateColor(bundle.getParcelable<Parcelable>(Const.CANVAS_CONTEXT) as CanvasContext)
            ViewStyler.setStatusBarDark(this, color)
            return color
        } else {
            val color = ContextCompat.getColor(this, R.color.login_studentAppTheme)
            ViewStyler.setStatusBarDark(this, color)
            return color
        }
    }

    override fun onNext(bundle: Bundle) {
        ValueAnimator.ofObject(ArgbEvaluator(), ContextCompat.getColor(this, R.color.login_studentAppTheme), getColor(bundle)).let {
            it.addUpdateListener { animation -> rootView!!.setBackgroundColor(animation.animatedValue as Int) }
            it.duration = 500
            it.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    UploadFilesDialog.show(supportFragmentManager, bundle, { event ->
                        if(event == UploadFilesDialog.EVENT_ON_UPLOAD_BEGIN) {
                            finish()
                        }
                    })
                }
            })
            it.start()
        }
    }
}
