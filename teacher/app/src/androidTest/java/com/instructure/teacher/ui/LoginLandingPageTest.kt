package com.instructure.teacher.ui

import com.instructure.espresso.filters.P1
import com.instructure.teacher.ui.utils.TeacherTest
import com.instructure.teacher.ui.utils.TestRail
import org.junit.Test

class LoginLandingPageTest: TeacherTest() {

    @Test
    @TestRail(ID = "C3108891")
    @P1
    override fun displaysPageObjects() {
        loginLandingPage.assertPageObjects()
    }

    @Test
    @TestRail(ID = "C3108893")
    fun opensCanvasNetworksSignInPage() {
        loginLandingPage.clickCanvasNetworkButton()
        loginSignInPage.assertPageObjects()
    }
}