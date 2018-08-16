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
@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "unused")

package com.instructure.canvasapi2.utils.weave

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Response


suspend fun inParallel(block: ParallelWaiter.() -> Unit) {
    val originStackTrace = Thread.currentThread().stackTrace
    return suspendCancellableCoroutine { continuation ->
        val waiter = ParallelWaiter(continuation, originStackTrace)
        waiter.block()
        waiter.start()
    }
}

class ParallelCallback<T>(private val managerCall: ManagerCall<T>) : StatusCallback<T>() {

    private var succeededOrFailed = false

    var onSuccess: SuccessCall<T> = {}

    var onError: ErrorCall = {}

    override fun onResponse(response: Response<T>, linkHeaders: LinkHeaders, type: ApiType) {
        succeededOrFailed = true
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            onSuccess(response.body() as T)
        } else {
            onError(StatusCallbackError(response = response))
        }
    }

    override fun onFinished(type: ApiType?) {
        if (!succeededOrFailed && type != ApiType.CACHE) onError(StatusCallbackError())
    }

    override fun onFail(call: Call<T>?, error: Throwable, response: Response<*>?) {
        succeededOrFailed = true
        onError(StatusCallbackError(call, error, response))
    }

    fun startCall() {
        if (!isCallInProgress) managerCall(this)
    }

}

class ParallelWaiter(private val continuation: CancellableContinuation<Unit>, private val originStackTrace: Array<StackTraceElement>) {

    private val computations = mutableListOf<ParallelCallback<*>>()

    init {
        continuation.invokeOnCompletion( { if (continuation.isCancelled) cancelAll() }, true)
    }

    fun <T> await(managerCall: ManagerCall<T>, errorCall: ((error: StatusCallbackError) -> Boolean)? = null, onComplete: SuccessCall<T>) {
        val callback = ParallelCallback(managerCall)
        callback.onSuccess = { payload ->
            if (!continuation.isCancelled) {
                synchronized(continuation) {
                    onComplete(payload)
                    checkForCompletion(callback)
                }
            }
        }
        callback.onError = { error ->
            if (!continuation.isCancelled) {
                synchronized(continuation) {
                    if (errorCall != null && errorCall(error)) {
                        checkForCompletion(callback)
                    } else {
                        this@ParallelWaiter.onError(error)
                    }
                }
            }
        }
        computations.add(callback)
    }

    private fun checkForCompletion(callback: ParallelCallback<*>) {
        if (continuation.isCompleted || continuation.isCancelled) return
        computations -= callback
        if (computations.isEmpty()) continuation.resumeSafely(Unit)
    }

    private fun onError(error: StatusCallbackError) {
        cancelAll()
        if (continuation.isActive) continuation.resumeSafelyWithException(error, originStackTrace)
    }

    private fun cancelAll() {
        computations.forEach { it.cancel() }
        computations.clear()
    }

    fun start() {
        if (computations.isEmpty()) {
            continuation.resumeSafely(Unit)
        } else {
            computations.forEach { it.startCall() }
        }
    }

}
