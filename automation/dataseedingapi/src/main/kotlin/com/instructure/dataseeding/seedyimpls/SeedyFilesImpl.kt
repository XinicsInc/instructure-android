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
package com.instructure.dataseeding.seedyimpls

import com.google.protobuf.ByteString
import com.instructure.dataseeding.Reaper
import com.instructure.dataseeding.SeedyReaper
import com.instructure.dataseeding.api.FileUploadsApi
import com.instructure.soseedy.Attachment
import com.instructure.soseedy.FileUploadType
import com.instructure.soseedy.SeedyFilesGrpc.SeedyFilesImplBase
import com.instructure.soseedy.UploadFileRequest
import io.grpc.stub.StreamObserver

class SeedyFilesImpl : SeedyFilesImplBase(), Reaper by SeedyReaper {
    //region API Calls
    private fun uploadFile(courseId: Long, assignmentId: Long, file: ByteString, fileName: String, fileUploadType: FileUploadType, token: String) =
            FileUploadsApi.uploadFile(courseId, assignmentId, file, fileName, token, fileUploadType)
    //endregion

    override fun uploadFile(request: UploadFileRequest, responseObserver: StreamObserver<Attachment>) {
        try {
            val uploadedFile = uploadFile(
                    request.courseId,
                    request.assignmentId,
                    request.file,
                    request.fileName,
                    request.uploadType,
                    request.token
            )

            val reply = Attachment.newBuilder()
                    .setDisplayName(uploadedFile.displayName)
                    .setFileName(uploadedFile.fileName)
                    .setId(uploadedFile.id)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
