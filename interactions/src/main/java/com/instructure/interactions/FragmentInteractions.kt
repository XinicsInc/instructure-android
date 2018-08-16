package com.instructure.interactions

import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Tab
import java.util.HashMap

interface FragmentInteractions {

    /**
     * MASTER shows in the left hand side (except for RTL languages) on tablets.
     * DETAILS shows in the right hand side (except for RTL languages) on tablets.
     * DIALOG refers to showing as fullscreen on phones and a dialog on tablets.
     * FULLSCREEN refers to showing as a fullscreen dialog fragment regardless of the device (phone v. tablet).
     */
    enum class Placement { MASTER, DETAIL, DIALOG, FULLSCREEN }

    var canvasContext: CanvasContext
    val navigation: Navigation?

    val tab: Tab?

    fun title(): String
    fun getFragmentPlacement(): Placement
    fun allowBookmarking(): Boolean
    fun getParamForBookmark(): HashMap<String, String>
    fun getQueryParamForBookmark(): HashMap<String, String>
    fun applyTheme()
}