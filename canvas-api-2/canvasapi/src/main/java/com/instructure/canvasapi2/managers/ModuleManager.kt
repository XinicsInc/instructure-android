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
import com.instructure.canvasapi2.apis.ModuleAPI
import com.instructure.canvasapi2.builders.RestBuilder
import com.instructure.canvasapi2.builders.RestParams
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.ExhaustiveListCallback
import okhttp3.ResponseBody

object ModuleManager : BaseManager() {

    const val MODULE_ASSET_ASSIGNMENT = "Assignment"
    const val MODULE_ASSET_PAGE = "Page"
    const val MODULE_ASSET_MODULE_ITEM = "ModuleItem"
    const val MODULE_ASSET_QUIZ = "Quiz"
    const val MODULE_ASSET_FILE = "File"
    const val MODULE_ASSET_DISCUSSION = "Discussion"
    const val MODULE_ASSET_EXTERNAL_TOOL = "ExternalTool"

    private val mTesting = false

    @JvmStatic
    fun getFirstPageModuleObjects(canvasContext: CanvasContext, callback: StatusCallback<List<ModuleObject>>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            ModuleAPI.getFirstPageModuleObjects(adapter, params, canvasContext.id, callback)
        }
    }

    @JvmStatic
    fun getNextPageModuleObjects(nextUrl: String, callback: StatusCallback<List<ModuleObject>>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            ModuleAPI.getNextPageModuleObjects(adapter, params, nextUrl, callback)
        }
    }

    @JvmStatic
    fun getFirstPageModuleItems(canvasContext: CanvasContext, moduleId: Long, callback: StatusCallback<List<ModuleItem>>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            ModuleAPI.getFirstPageModuleItems(adapter, params, canvasContext.id, moduleId, callback)
        }
    }

    @JvmStatic
    fun getNextPageModuleItems(nextUrl: String, callback: StatusCallback<List<ModuleItem>>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            ModuleAPI.getNextPageModuleItems(adapter, params, nextUrl, callback)
        }
    }

    @JvmStatic
    fun getAllModuleItems(canvasContext: CanvasContext, moduleId: Long, callback: StatusCallback<List<ModuleItem>>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            val depaginatedCallback = object : ExhaustiveListCallback<ModuleItem>(callback) {
                override fun getNextPage(callback: StatusCallback<List<ModuleItem>>, nextUrl: String, isCached: Boolean) {
                    ModuleAPI.getAllModuleItems(adapter, params, canvasContext.id, moduleId, callback)
                }
            }
            adapter.statusCallback = depaginatedCallback
            ModuleAPI.getAllModuleItems(adapter, params, canvasContext.id, moduleId, depaginatedCallback)
        }
    }

    @JvmStatic
    fun markAsDone(canvasContext: CanvasContext, moduleId: Long, itemId: Long, callback: StatusCallback<ResponseBody>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .build()
            ModuleAPI.markModuleAsDone(adapter, params, canvasContext, moduleId, itemId, callback)
        }
    }

    @JvmStatic
    fun markAsNotDone(canvasContext: CanvasContext, moduleId: Long, itemId: Long, callback: StatusCallback<ResponseBody>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .build()
            ModuleAPI.markModuleAsNotDone(adapter, params, canvasContext, moduleId, itemId, callback)
        }
    }

    @JvmStatic
    fun markModuleItemAsRead(canvasContext: CanvasContext, moduleId: Long, itemId: Long, callback: StatusCallback<ResponseBody>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .build()
            ModuleAPI.markModuleItemAsRead(adapter, params, canvasContext, moduleId, itemId, callback)
        }
    }

    @JvmStatic
    fun selectMasteryPath(canvasContext: CanvasContext, moduleId: Long, itemId: Long, assignmentSetId: Long, callback: StatusCallback<MasteryPathSelectResponse>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .build()
            ModuleAPI.selectMasteryPath(adapter, params, canvasContext, moduleId, itemId, assignmentSetId, callback)
        }
    }

    @JvmStatic
    fun getModuleItemSequence(canvasContext: CanvasContext, assetType: String, assetId: String, callback: StatusCallback<ModuleItemSequence>, forceNetwork: Boolean) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            ModuleAPI.getModuleItemSequence(adapter, params, canvasContext, assetType, assetId, callback)
        }

    }
}