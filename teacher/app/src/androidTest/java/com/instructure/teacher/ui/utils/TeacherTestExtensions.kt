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
 *
 */

package com.instructure.teacher.ui.utils

import android.os.Environment
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import com.google.protobuf.ByteString
import com.instructure.canvasapi2.models.User
import com.instructure.dataseeding.InProcessServer
import com.instructure.dataseeding.model.EnrollmentTypes
import com.instructure.dataseeding.util.DataSeedingException
import com.instructure.dataseeding.util.Randomizer
import com.instructure.soseedy.*
import com.instructure.soseedy.FileUploadType.ASSIGNMENT_SUBMISSION
import com.instructure.soseedy.FileUploadType.COMMENT_ATTACHMENT
import java.io.*


fun TeacherTest.enterDomain(enrollmentType: String = EnrollmentTypes.TEACHER_ENROLLMENT): CanvasUser {
    val user = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    val course = InProcessServer.courseClient.createCourse(CreateCourseRequest.getDefaultInstance())
    val enrollment = InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder()
            .setCourseId(course.id)
            .setUserId(user.id)
            .setEnrollmentType(enrollmentType)
            .build())
    loginFindSchoolPage.enterDomain(user.domain)
    return user
}

fun TeacherTest.enterStudentDomain(): CanvasUser {
    val user = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    val course = InProcessServer.courseClient.createCourse(CreateCourseRequest.getDefaultInstance())
    // TODO: Enroll user as student
    val enrollment = InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder().setCourseId(course.id).setUserId(user.id).build())

    loginFindSchoolPage.enterDomain(user.domain)
    return user
}

fun TeacherTest.slowLogIn(enrollmentType: String = EnrollmentTypes.TEACHER_ENROLLMENT): CanvasUser {
    loginLandingPage.clickFindMySchoolButton()
    val user = enterDomain(enrollmentType)
    loginFindSchoolPage.clickToolbarNextMenuItem()
    loginSignInPage.loginAs(user)
    return user
}

fun TeacherTest.slowLogInAsStudent(): CanvasUser = slowLogIn(EnrollmentTypes.STUDENT_ENROLLMENT)

fun TeacherTest.logIn(skipSplash: Boolean = true, enrollmentType: String = EnrollmentTypes.TEACHER_ENROLLMENT): CanvasUser {
    val teacher = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    val course = InProcessServer.courseClient.createCourse(CreateCourseRequest.getDefaultInstance())
    val enrollment = InProcessServer.enrollmentClient.enrollUserInCourse(
            EnrollUserRequest.newBuilder()
                    .setCourseId(course.id)
                    .setUserId(teacher.id)
                    .setEnrollmentType(enrollmentType)
                    .build())

    activityRule.runOnUiThread {
        activityRule.activity.loginWithToken(
                teacher.token,
                teacher.domain,
                User().apply {
                    id = teacher.id
                    name = teacher.name
                    shortName = teacher.shortName
                    avatarUrl = teacher.avatarUrl
                },
                skipSplash
        )
    }

    return teacher
}

fun TeacherTest.seedData(
        teachers: Int = 0,
        courses: Int = 0,
        students: Int = 0,
        favoriteCourses: Int = 0,
        announcements: Int = 0,
        discussions: Int = 0,
        gradingPeriods: Boolean = false): SeededData {

    val request = SeedDataRequest.newBuilder()
            .setTeachers(teachers)
            .setCourses(courses)
            .setStudents(students)
            .setFavoriteCourses(favoriteCourses)
            .setAnnouncements(announcements)
            .setDiscussions(discussions)
            .setGradingPeriods(gradingPeriods)
            .build()

    return InProcessServer.generalClient.seedData(request)
}

fun TeacherTest.seedAssignments(
        courseId: Long,
        assignments: Int = 1,
        withDescription: Boolean = false,
        lockAt: String = "",
        unlockAt: String = "",
        dueAt: String = "",
        submissionTypes: List<SubmissionType> = emptyList(),
        teacherToken: String): Assignments {

    val request = SeedAssignmentRequest.newBuilder()
            .setCourseId(courseId)
            .setAssignments(assignments)
            .setWithDescription(withDescription)
            .setLockAt(lockAt)
            .setUnlockAt(unlockAt)
            .setDueAt(dueAt)
            .addAllSubmissionTypes(submissionTypes)
            .setTeacherToken(teacherToken)
            .build()

    return InProcessServer.assignmentClient.seedAssignments(request)
}

// Must publish quiz after creating a question for that question to appear.
fun TeacherTest.seedQuizQuestion(
        courseId: Long,
        quizId: Long,
        teacherToken: String
) {
    val createQuizQuestionRequest = CreateQuizQuestionRequest.newBuilder()
            .setCourseId(courseId)
            .setQuizId(quizId)
            .setTeacherToken(teacherToken)
            .build()

    InProcessServer.quizClient.createQuizQuestion(createQuizQuestionRequest)
}

fun TeacherTest.publishQuiz(courseId: Long,
                            quizId: Long,
                            teacherToken: String) {
    val publishQuizRequest = PublishQuizRequest.newBuilder()
            .setCourseId(courseId)
            .setQuizId(quizId)
            .setTeacherToken(teacherToken)
            .setPublished(true)
            .build()

    InProcessServer.quizClient.publishQuiz(publishQuizRequest)
}

fun TeacherTest.seedQuizzes(
        courseId: Long,
        quizzes: Int = 1,
        withDescription: Boolean = false,
        lockAt: String = "",
        unlockAt: String = "",
        dueAt: String = "",
        published: Boolean = true,
        teacherToken: String): Quizzes {

    val quizzesRequest = SeedQuizzesRequest.newBuilder()
            .setCourseId(courseId)
            .setQuizzes(quizzes)
            .setWithDescription(withDescription)
            .setLockAt(lockAt)
            .setUnlockAt(unlockAt)
            .setDueAt(dueAt)
            .setPublished(published)
            .setToken(teacherToken)
            .build()

    return InProcessServer.quizClient.seedQuizzes(quizzesRequest)
}

// "you are not allowed to participate in this quiz" = make sure the quiz isn't locked
fun TeacherTest.seedQuizSubmission(
        courseId: Long,
        quizId: Long,
        studentToken: String,
        complete: Boolean = true): QuizSubmission {

    val request = SeedQuizSubmissionRequest.newBuilder()
            .setCourseId(courseId)
            .setQuizId(quizId)
            .setStudentToken(studentToken)
            .setComplete(complete)
            .build()

    return InProcessServer.quizClient.seedQuizSubmission(request)
}

fun TeacherTest.seedAssignmentSubmission(
        submissionSeeds: List<SubmissionSeed>,
        assignmentId: Long,
        courseId: Long,
        studentToken: String,
        commentSeeds: List<CommentSeed> = emptyList()): SeededCourseAssignmentSubmissions {

    // Upload one submission file for each submission seed
    // TODO: Add ability to upload more than one submission
    val seedsWithAttachments = submissionSeeds.map {
        SubmissionSeed.newBuilder(it)
                .addAttachments(
                        when (it.submissionType) {
                            SubmissionType.ONLINE_UPLOAD -> uploadTextFile(courseId, assignmentId, studentToken, ASSIGNMENT_SUBMISSION)
                            else -> Attachment.getDefaultInstance() // Not handled right now
                        }
                )
                .build()
    }

    // Upload comment files
    // TODO: We could make this more granular, allowing multiple comments per seed with different file upload types
    val seedsWithComments = commentSeeds.map {
        val fileAttachments: MutableList<Attachment> = mutableListOf()

        for (i in 0..it.amount) {
            if (it.fileType != FileType.NONE) {
                fileAttachments.add(when (it.fileType) {
                    FileType.PDF -> TODO()
                    FileType.TEXT -> uploadTextFile(courseId, assignmentId, studentToken, COMMENT_ATTACHMENT)
                    else -> throw RuntimeException("Unknown file type passed into TeacherTest.seedAssignmentSubmission") // Unknown type
                })
            }
        }
        CommentSeed.newBuilder()
                .addAllAttachments(fileAttachments)
                .build()
    }

    // Seed the submissions
    val submissionRequest = SeedAssignmentSubmissionRequest.newBuilder()
            .setAssignmentId(assignmentId)
            .setCourseId(courseId)
            .setStudentToken(studentToken)
            .addAllSubmissionSeeds(seedsWithAttachments)
            .addAllCommentSeeds(seedsWithComments)
            .build()

    return InProcessServer.assignmentClient.seedAssignmentSubmission(submissionRequest)
}

fun uploadTextFile(courseId: Long, assignmentId: Long, token: String, fileUploadType: FileUploadType): Attachment {

    // Create the file
    val file = File(
            Randomizer.randomTextFileName(Environment.getExternalStorageDirectory().absolutePath))
            .apply { createNewFile() }

    // Add contents to file
    FileWriter(file, true).apply {
        write(Randomizer.randomTextFileContents())
        flush()
        close()
    }

    // Start the Canvas file upload process
    val uploadRequest = UploadFileRequest.newBuilder()
            .setCourseId(courseId)
            .setAssignmentId(assignmentId)
            .setToken(token)
            .setFileName(file.name)
            .setFile(ByteString.copyFrom(file.toByteArray()))
            .setUploadType(fileUploadType)
            .build()

    return InProcessServer.fileClient.uploadFile(uploadRequest)
}

fun TeacherTest.seedConversation(sender: CanvasUser, recipients: List<CanvasUser>): Conversation {
    val request = CreateConversationRequest.newBuilder()
            .setToken(sender.token)
            .addAllRecipients(recipients.map { r -> r.id.toString() })
            .build()

    return InProcessServer.conversationClient.createConversation(request)
}

fun TeacherTest.seedSection(course: Course): Section {
    val request = CreateSectionRequest.newBuilder()
    request.courseId = course.id

    return InProcessServer.sectionClient.createSection(request.build())
}

fun TeacherTest.seedSectionStudentEnrollment(section: Section, user: CanvasUser): Enrollment
        = seedSectionEnrollment(section, user, EnrollmentTypes.STUDENT_ENROLLMENT)

fun TeacherTest.seedSectionTeacherEnrollment(section: Section, user: CanvasUser): Enrollment
        = seedSectionEnrollment(section, user, EnrollmentTypes.TEACHER_ENROLLMENT)

fun TeacherTest.seedSectionTaEnrollment(section: Section, user: CanvasUser): Enrollment
        = seedSectionEnrollment(section, user, EnrollmentTypes.TA_ENROLLMENT)

fun TeacherTest.seedSectionObserverEnrollment(section: Section, user: CanvasUser): Enrollment
        = seedSectionEnrollment(section, user, EnrollmentTypes.OBSERVER_ENROLLMENT)

fun TeacherTest.seedSectionDesignerEnrollment(section: Section, user: CanvasUser): Enrollment
        = seedSectionEnrollment(section, user, EnrollmentTypes.DESIGNER_ENROLLMENT)

private fun seedSectionEnrollment(section: Section, user: CanvasUser, enrollmentType: String): Enrollment {
    val request = EnrollUserInSectionRequest.newBuilder()
            .setSectionId(section.id)
            .setUserId(user.id)
            .setEnrollmentType(enrollmentType)
            .build()

    return InProcessServer.enrollmentClient.enrollUserInSection(request)
}

fun TeacherTest.seedCoursePage(course: Course, published: Boolean = true, frontPage: Boolean = false, teacher: CanvasUser): Page {
    if (frontPage && !published) {
        throw DataSeedingException("Front Page must be Published")
    }
    val request = CreateCoursePageRequest.newBuilder()
            .setCourseId(course.id)
            .setPublished(published)
            .setFrontPage(frontPage)
            .setToken(teacher.token)
            .build()

    return InProcessServer.pageClient.createCoursePage(request)
}

fun TeacherTest.seedCourseGroupCategory(course: Course): GroupCategory {
    val request = CreateCourseGroupCategoryRequest.newBuilder()
            .setCourseId(course.id)
            .build()

    return InProcessServer.groupClient.createCourseGroupCategory(request)
}

fun TeacherTest.seedGroup(groupCategory: GroupCategory): Group {
    val request = CreateGroupRequest.newBuilder()
            .setGroupCategoryId(groupCategory.id)
            .build()

    return InProcessServer.groupClient.createGroup(request)
}

fun TeacherTest.seedGroupMembership(group: Group, user: CanvasUser): GroupMembership {
    val request = CreateGroupMembershipRequest.newBuilder()
            .setGroupId(group.id)
            .setUserId(user.id)
            .build()

    return InProcessServer.groupClient.createGroupMembership(request)
}

val SeededData.favoriteCourses: List<Course>
    get() =
        this.favoritesList.map { fav ->
            this.coursesList.first { course ->
                course.id == fav.contextId
            }
        }

val SeededData.announcements: List<Discussion>
    get() =
        this.discussionsList.filter {
            it.isAnnouncement
        }


fun TeacherTest.tokenLogin(teacher: CanvasUser, skipSplash: Boolean = true) {
    activityRule.runOnUiThread {
        activityRule.activity.loginWithToken(
                teacher.token,
                teacher.domain,
                User().apply {
                    id = teacher.id
                    name = teacher.name
                    shortName = teacher.shortName
                    avatarUrl = teacher.avatarUrl
                },
                skipSplash
        )
    }
}

fun TeacherTest.openOverflowMenu() {
    Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
}

fun TeacherTest.logInAsStudent(): CanvasUser {
    loginLandingPage.clickFindMySchoolButton()
    val student = enterStudentDomain()
    loginFindSchoolPage.clickToolbarNextMenuItem()
    loginSignInPage.loginAs(student)
    return student
}

fun File.toByteArray(): ByteArray {
    val size = this.length().toInt()
    val bytes: ByteArray = ByteArray(size)

    val bufferInputStream = BufferedInputStream(FileInputStream(this))
    val dataInputStream = DataInputStream(bufferInputStream)
    dataInputStream.readFully(bytes)

    return bytes
}
