package com.instructure.dataseeding

import com.instructure.dataseeding.seedyimpls.*
import io.grpc.Server
import io.grpc.ServerBuilder
import java.io.IOException

private object OutOfProcessServer {
    private val port = 50051
    private val server: Server = ServerBuilder.forPort(port)
            .addService(GeneralSeedyImpl())
            .addService(SeedyAssignmentsImpl())
            .addService(SeedyConversationsImpl())
            .addService(SeedyCoursesImpl())
            .addService(SeedyDiscussionsImpl())
            .addService(SeedyEnrollmentsImpl())
            .addService(SeedyFilesImpl())
            .addService(SeedyGradingPeriodsImpl())
            .addService(SeedyGroupsImpl())
            .addService(SeedyPagesImpl())
            .addService(SeedyQuizzesImpl())
            .addService(SeedySectionsImpl())
            .addService(SeedyUsersImpl())
            .build()

    @Throws(IOException::class)
    private fun start() {
        server.start()
        println("Server started on port $port")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                System.err.println("JVM shutdown hook activated. Shutting down...")
                OutOfProcessServer.stop()
                System.err.println("Server shut down.")
            }
        })
    }

    private fun stop() {
        server.shutdown()
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    @Throws(InterruptedException::class)
    private fun blockUntilShutdown() {
        server.awaitTermination()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        OutOfProcessServer.start()
        OutOfProcessServer.blockUntilShutdown()
    }
}
