package com.instructure.espresso

import android.support.test.espresso.util.HumanReadables
import android.view.View

object ViewUtils {

    fun toString(view: View): String {
        return HumanReadables.getViewHierarchyErrorMessage(view, null, "", null)
    }
}
