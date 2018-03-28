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

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.instructure.candroid.R
import com.instructure.candroid.util.FragUtils
import com.instructure.candroid.util.LockInfoHTMLHelper
import com.instructure.candroid.util.Param
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.QuizManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Quiz
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.Const
import retrofit2.Call
import retrofit2.Response
import java.util.*

class BasicQuizViewFragment : InternalWebviewFragment() {

    private var baseURL: String? = null
    private var apiURL: String? = null
    private var quiz: Quiz? = null
    private var quizId: Long = -1L

    private var canvasCallback: StatusCallback<Quiz>? = null
    private var getDetailedQuizCallback: StatusCallback<Quiz>? = null

    override fun getFragmentPlacement() = FragmentInteractions.Placement.DETAIL
    override fun title(): String = getString(R.string.quizzes)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //we need to set the webviewclient before we get the quiz so it doesn't try to open the
        //quiz in a different browser
        if (baseURL == null) {
            //if the baseURL is null something went wrong, nothing will show here
            //but at least it won't crash
            return
        }
        val uri = Uri.parse(baseURL)
        val host = uri.host
        getCanvasWebView()?.settings?.javaScriptCanOpenWindowsAutomatically = true
        getCanvasWebView()?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val currentUri = Uri.parse(url)

                if (url.contains(host)) { //we need to handle it.
                    return if (currentUri != null && currentUri.pathSegments.size >= 3 && currentUri.pathSegments[2] == "quizzes") {  //if it's a quiz, stay here.
                        view.loadUrl(url, APIHelper.getReferrer())
                        true
                    } else if (currentUri != null && currentUri.pathSegments.size >= 1 && currentUri.pathSegments[0].equals("login", ignoreCase = true)) {
                        view.loadUrl(url, APIHelper.getReferrer())
                        true
                    } else { //It's content but not a quiz. Could link to a discussion (or whatever) in a quiz. Route
                        RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, true)
                    }//might need to log in to take the quiz -- the url would say domain/login. If we just use the AppRouter it will take the user
                    //back to the dashboard. This check will keep them here and let them log in and take the quiz
                }

                return false
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                getCanvasLoading()?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                getCanvasLoading()?.visibility = View.GONE
            }
        }
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpCallback()
        // anything that relies on intent data belongs here
        if (apiURL != null) {
            QuizManager.getDetailedQuizByUrl(apiURL, true, canvasCallback)
        } else if (quiz != null && quiz?.lockInfo != null && CanvasContext.Type.isCourse(canvasContext) && !(canvasContext as Course).isTeacher) {
            //if the quiz is locked we don't care if they're a teacher
            populateWebView(LockInfoHTMLHelper.getLockedInfoHTML(quiz?.lockInfo, activity, R.string.lockedQuizDesc, R.string.lockedAssignmentDescLine2))
        } else if (quizId != -1L) {
            QuizManager.getDetailedQuiz(canvasContext, quizId, true, getDetailedQuizCallback)
        } else {
            loadUrl(baseURL)
        }
    }

    override fun handleBackPressed() = getCanvasWebView()?.handleGoBack() ?: false

    private fun setUpCallback() {
        getDetailedQuizCallback = object : StatusCallback<Quiz>() {
            internal var cacheQuiz: Quiz? = null

            override fun onResponse(response: Response<Quiz>, linkHeaders: LinkHeaders, type: ApiType) {
                if (!apiCheck()) return

                if (type == ApiType.CACHE) cacheQuiz = response.body()
                loadQuiz(response.body())
            }

            override fun onFail(call: Call<Quiz>?, error: Throwable, response: Response<*>?) {
                if (!apiCheck()) return

                loadQuiz(cacheQuiz)
            }

            private fun loadQuiz(quiz: Quiz?) {
                if (quiz == null) return

                this@BasicQuizViewFragment.quiz = quiz
                this@BasicQuizViewFragment.baseURL = quiz.url

                if (shouldShowNatively(quiz)) {
                    return
                }

                if (quiz.lockInfo != null) {
                    populateWebView(LockInfoHTMLHelper.getLockedInfoHTML(quiz.lockInfo, activity, R.string.lockedQuizDesc, R.string.lockedAssignmentDescLine2))
                } else {
                    getCanvasWebView()?.loadUrl(quiz.url, APIHelper.getReferrer())
                }
            }
        }
        canvasCallback = object : StatusCallback<Quiz>() {

            override fun onResponse(response: Response<Quiz>, linkHeaders: LinkHeaders, type: ApiType) {
                if (!apiCheck()) return

                this@BasicQuizViewFragment.quiz = quiz
                if (shouldShowNatively(quiz)) return

                if (quiz?.lockInfo != null) {
                    populateWebView(LockInfoHTMLHelper.getLockedInfoHTML(quiz?.lockInfo, activity, R.string.lockedQuizDesc, R.string.lockedAssignmentDescLine2))
                } else {
                    var url: String? = quiz?.url
                    if (TextUtils.isEmpty(url)) {
                        url = baseURL
                    }
                    getCanvasWebView()?.loadUrl(url, APIHelper.getReferrer())
                }
            }
        }
    }

    /**
     * When we route we always route quizzes here, so this checks to see if we support
     * native quizzes and if we do then we'll show it natively
     * @param quiz
     * @return
     */
    private fun shouldShowNatively(quiz: Quiz?): Boolean {
        if (QuizListFragment.isNativeQuiz(canvasContext, quiz)) {
            //take them to the quiz start fragment instead, let them take it natively
            navigation?.popCurrentFragment()
            val bundle = QuizStartFragment.createBundle(canvasContext, quiz)
            navigation?.addFragment(FragUtils.getFrag(QuizStartFragment::class.java, bundle), true)
            return true
        }
        return false
    }

    ///////////////////////////////////////////////////////////////////////////
    // Intent
    ///////////////////////////////////////////////////////////////////////////

    override fun handleIntentExtras(extras: Bundle?) {
        if (urlParams != null) {
            quizId = parseLong(urlParams[Param.QUIZ_ID], -1)
        } else {
            baseURL = extras?.getString(Const.URL)
            apiURL = extras?.getString(Const.API_URL)
            quiz = extras?.getParcelable(Const.QUIZ)
        }
        super.handleIntentExtras(extras)
    }

    //Currently there isn't a way to know how to decide if we want to route
    //to this fragment or the QuizStartFragment.
    override fun allowBookmarking(): Boolean {
        return false
    }

    override fun getParamForBookmark(): HashMap<String, String> {
        val map = super.getParamForBookmark()
        if (quiz != null) {
            map.put(Param.QUIZ_ID, java.lang.Long.toString(quiz!!.id))
        } else if (quizId != -1L) {
            map.put(Param.QUIZ_ID, java.lang.Long.toString(quizId))
        }
        return map
    }

    companion object {

        fun createBundle(canvasContext: CanvasContext, url: String): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.URL, url)
            return extras
        }

        fun createBundle(canvasContext: CanvasContext, url: String, quiz: Quiz): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putParcelable(Const.QUIZ, quiz)
            extras.putString(Const.URL, url)
            return extras
        }

        //for module progression we need the api url too
        fun createBundle(canvasContext: CanvasContext, url: String, apiURL: String): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putString(Const.URL, url)
            extras.putString(Const.API_URL, apiURL)
            return extras
        }
    }
}
