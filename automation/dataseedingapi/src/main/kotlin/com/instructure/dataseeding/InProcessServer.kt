package com.instructure.dataseeding

import com.instructure.dataseeding.seedyimpls.*
import com.instructure.soseedy.*
import com.instructure.soseedy.SeedyAssignmentsGrpc.SeedyAssignmentsBlockingStub
import com.instructure.soseedy.SeedyConversationsGrpc.SeedyConversationsBlockingStub
import com.instructure.soseedy.SeedyCoursesGrpc.SeedyCoursesBlockingStub
import com.instructure.soseedy.SeedyDiscussionsGrpc.SeedyDiscussionsBlockingStub
import com.instructure.soseedy.SeedyEnrollmentsGrpc.SeedyEnrollmentsBlockingStub
import com.instructure.soseedy.SeedyFilesGrpc.SeedyFilesBlockingStub
import com.instructure.soseedy.SeedyGeneralGrpc.SeedyGeneralBlockingStub
import com.instructure.soseedy.SeedyGradingPeriodsGrpc.SeedyGradingPeriodsBlockingStub
import com.instructure.soseedy.SeedyGroupsGrpc.SeedyGroupsBlockingStub
import com.instructure.soseedy.SeedyPagesGrpc.SeedyPagesBlockingStub
import com.instructure.soseedy.SeedyQuizzesGrpc.SeedyQuizzesBlockingStub
import com.instructure.soseedy.SeedySectionsGrpc.SeedySectionsBlockingStub
import com.instructure.soseedy.SeedyUsersGrpc.SeedyUsersBlockingStub
import com.instructure.soseedy.SeedyFeatureFlagsGrpc.SeedyFeatureFlagsBlockingStub
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import java.util.concurrent.TimeUnit

object InProcessServer {
    private val UNIQUE_SERVER_NAME = "android-soseedy"
    private val server = InProcessServerBuilder.forName(UNIQUE_SERVER_NAME)
            .directExecutor()
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
            .addService(SeedyFeatureFlagsImpl())
            .build()

    private val channel = InProcessChannelBuilder.forName(UNIQUE_SERVER_NAME).directExecutor().build()

    val generalClient: SeedyGeneralBlockingStub = SeedyGeneralGrpc.newBlockingStub(channel)
    val assignmentClient: SeedyAssignmentsBlockingStub = SeedyAssignmentsGrpc.newBlockingStub(channel)
    val conversationClient: SeedyConversationsBlockingStub = SeedyConversationsGrpc.newBlockingStub(channel)
    val courseClient: SeedyCoursesBlockingStub = SeedyCoursesGrpc.newBlockingStub(channel)
    val discussionClient: SeedyDiscussionsBlockingStub = SeedyDiscussionsGrpc.newBlockingStub(channel)
    val enrollmentClient: SeedyEnrollmentsBlockingStub = SeedyEnrollmentsGrpc.newBlockingStub(channel)
    val fileClient: SeedyFilesBlockingStub = SeedyFilesGrpc.newBlockingStub(channel)
    val gradingClient: SeedyGradingPeriodsBlockingStub = SeedyGradingPeriodsGrpc.newBlockingStub(channel)
    val groupClient: SeedyGroupsBlockingStub = SeedyGroupsGrpc.newBlockingStub(channel)
    val pageClient: SeedyPagesBlockingStub = SeedyPagesGrpc.newBlockingStub(channel)
    val quizClient: SeedyQuizzesBlockingStub = SeedyQuizzesGrpc.newBlockingStub(channel)
    val sectionClient: SeedySectionsBlockingStub = SeedySectionsGrpc.newBlockingStub(channel)
    val userClient: SeedyUsersBlockingStub = SeedyUsersGrpc.newBlockingStub(channel)
    val featureFlagClient: SeedyFeatureFlagsBlockingStub = SeedyFeatureFlagsGrpc.newBlockingStub(channel)

    init {
        server.start()
    }

    fun stop() {
        channel.shutdown()
        server.shutdown()
        channel.awaitTermination(1, TimeUnit.MINUTES)
        server.awaitTermination(1, TimeUnit.MINUTES)
    }
}
