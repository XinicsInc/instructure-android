package com.instructure.dataseeding

import io.grpc.Status
import io.grpc.stub.StreamObserver

interface Reaper {
    fun <V> onError(responseObserver: StreamObserver<V>?, e: Exception)
    fun <V> onSuccess(responseObserver: StreamObserver<V>?, reply: V)
}

object SeedyReaper : Reaper {

    override fun <V> onError(responseObserver: StreamObserver<V>?, e: Exception) {
        if (responseObserver == null) return

        responseObserver.onError(Status.INTERNAL
                .withDescription(e.toString())
                .withCause(e)
                .asRuntimeException())
    }

    override fun <V> onSuccess(responseObserver: StreamObserver<V>?, reply: V) {
        if (responseObserver == null) return

        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}
