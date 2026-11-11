package com.andyer.layoutflow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun editorLaunchesAndOpensCatalog() {
        rule.waitForIdle()
        rule.onNodeWithText("元件库").assertIsDisplayed()
        rule.onNodeWithText("登录页").assertIsDisplayed()
        rule.onNodeWithText("全部元件").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("全部元件").assertCountEquals(2)
    }
}
