/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.instructure.canvasapi2.managers

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.apis.PageAPI
import com.instructure.canvasapi2.builders.RestBuilder
import com.instructure.canvasapi2.builders.RestParams
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Page
import com.instructure.canvasapi2.models.post_models.PagePostBody
import com.instructure.canvasapi2.models.post_models.PagePostBodyWrapper
import com.instructure.canvasapi2.utils.ExhaustiveListCallback

object PageManager : BaseManager() {

    private val mTesting = false

    @JvmStatic
    fun getFirstPagePages(canvasContext: CanvasContext, callback: StatusCallback<List<Page>>, forceNetwork: Boolean) {
        if (BaseManager.isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()

            PageAPI.getFirstPagePages(adapter, params, canvasContext, callback)
        }
    }

    fun getAllPages(canvasContext: CanvasContext, forceNetwork: Boolean, callback: StatusCallback<List<Page>>) {
        if (BaseManager.isTesting() || mTesting) {
            //TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()

            val depaginatedParams = RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()


            val depaginatedCallback = object : ExhaustiveListCallback<Page>(callback) {
                override fun getNextPage(callback: StatusCallback<List<Page>>, nextUrl: String, isCached: Boolean) {
                    PageAPI.getNextPagePages(adapter, depaginatedParams, nextUrl, callback)
                }
            }
            adapter.statusCallback = depaginatedCallback
            PageAPI.getFirstPagePages(adapter, params, canvasContext, depaginatedCallback)
        }
    }
    @JvmStatic
    fun getNextPagePages(nextPage: String, callback: StatusCallback<List<Page>>, forceNetwork: Boolean) {
        if (BaseManager.isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()

            PageAPI.getNextPagePages(adapter, params, nextPage, callback)
        }
    }

    @JvmStatic
    fun getPageDetails(canvasContext: CanvasContext, pageId: String, forceNetwork: Boolean, callback: StatusCallback<Page>) {
        if (BaseManager.isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()

            PageAPI.getDetailedPage(adapter, params, canvasContext, pageId, callback)
        }
    }

    @JvmStatic
    fun getFrontPage(canvasContext: CanvasContext, forceNetwork: Boolean, callback: StatusCallback<Page>) {
        if (BaseManager.isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()

            PageAPI.getFrontPage(adapter, params, canvasContext, callback)
        }
    }

    @JvmStatic
    fun editPage(canvasContext: CanvasContext, pageUrl: String, pagePostBody: PagePostBody, callback: StatusCallback<Page>) {
        if (BaseManager.isTesting() || mTesting) {

        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build()

            val pageWrapper = PagePostBodyWrapper()
            pageWrapper.wiki_page = pagePostBody
            PageAPI.editPage(adapter, params, canvasContext, pageUrl, pageWrapper, callback)
        }
    }

    @JvmStatic
    fun createPage(canvasContext: CanvasContext, page: Page, callback: StatusCallback<Page>) {
        if (BaseManager.isTesting() || mTesting) {

        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build()

            PageAPI.createPage(adapter, params, canvasContext, page, callback)
        }
    }

    @JvmStatic
    fun deletePage(canvasContext: CanvasContext, pageUrl: String, callback: StatusCallback<Page>) {
        if (BaseManager.isTesting() || mTesting) {

        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build()

            PageAPI.deletePage(adapter, params, canvasContext, pageUrl, callback)
        }
    }
}
