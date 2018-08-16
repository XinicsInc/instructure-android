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
@file:JvmName("CanvasContextExtensions")
package com.instructure.pandautils.utils

import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course

val CanvasContext.color: Int get() = ColorKeeper.getOrGenerateColor(this)

val CanvasContext.isCourse: Boolean get() = this.type == CanvasContext.Type.COURSE
val CanvasContext.isGroup: Boolean get() = this.type == CanvasContext.Type.GROUP
val CanvasContext.isCourseOrGroup: Boolean get() = this.type == CanvasContext.Type.GROUP || this.type == CanvasContext.Type.COURSE
val CanvasContext.isNotUser: Boolean get() = this.type != CanvasContext.Type.USER
val CanvasContext.isCourseContext: Boolean get() = this.type != CanvasContext.Type.USER
val CanvasContext.isUser: Boolean get() = this.type  == CanvasContext.Type.USER

fun CanvasContext.isDesigner(): Boolean = this.isCourseContext && (this as Course).isDesigner

