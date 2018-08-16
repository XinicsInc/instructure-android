/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
package com.instructure.pandautils.views

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.instructure.pandautils.R
import com.instructure.pandautils.utils.DP
import com.instructure.pandautils.utils.obtainFor

class AttachmentDogEarLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var dogEarSize = 0f

    private val rtlFlipMatrix: Matrix by lazy {
        Matrix().apply { postScale(-1f, 1f, width / 2f, 0f) }
    }

    private val dogEarPaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.lightgray) }
    }

    private val dogEarShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000 }

    private val clipPath: Path by lazy {
        Path().apply {
            moveTo(0f, 0f)
            lineTo(dogEarPoint.x, 0f)
            lineTo(width.toFloat(), dogEarPoint.y)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
            flipForRtlIfNecessary(this)
        }
    }

    private val dogEarPath: Path by lazy {
        Path().apply {
            moveTo(dogEarPoint.x, -1f)
            lineTo(width + 1f, dogEarPoint.y)
            lineTo(dogEarPoint.x, dogEarPoint.y)
            close()
            flipForRtlIfNecessary(this)
        }
    }

    private val dogEarPoint: PointF by lazy {
        PointF(width - dogEarSize, dogEarSize)
    }

    private val dogEarShadowPath: Path by lazy {
        Path().apply {
            moveTo(dogEarPoint.x, -1f)
            lineTo(width + 1f, dogEarPoint.y + 1)
            lineTo(dogEarPoint.x + dogEarPoint.y * DOG_EAR_SHADOW_OFFSET_MULTIPLIER_X,
                    dogEarPoint.y + dogEarPoint.y * DOG_EAR_SHADOW_OFFSET_MULTIPLIER_Y)
            close()
            flipForRtlIfNecessary(this)
        }
    }

    init {
        // In edit mode, only the software layer type supports non-rectangular path clipping
        if (isInEditMode) setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // Set defaults and get any XML attributes
        dogEarSize = context.DP(DOG_EAR_DIMEN_DP)
        attrs?.obtainFor(this, R.styleable.AttachmentDogEarLayout) { a, idx ->
            when (idx) {
                R.styleable.AttachmentDogEarLayout_adl_dogear_size -> dogEarSize = a.getDimension(idx, dogEarSize)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Perform clip, draw children
        canvas.save()
        canvas.clipPath(clipPath)
        super.dispatchDraw(canvas)
        canvas.restore()

        // Draw dog-ear
        canvas.drawPath(dogEarShadowPath, dogEarShadowPaint)
        canvas.drawPath(dogEarPath, dogEarPaint)
    }

    private fun flipForRtlIfNecessary(path: Path) {
        if (ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            path.transform(rtlFlipMatrix)
        }
    }

    companion object {
        private const val DOG_EAR_DIMEN_DP = 20f
        private const val DOG_EAR_SHADOW_OFFSET_MULTIPLIER_X = 0.08f
        private const val DOG_EAR_SHADOW_OFFSET_MULTIPLIER_Y = 0.15f
    }
}