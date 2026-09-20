package com.example.todoapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.todoapplication.feature.todo.edit.EDIT_TITLE_TAG
import com.example.todoapplication.feature.todo.edit.EDIT_DATE_TAG
import com.example.todoapplication.feature.todo.edit.EDIT_TIME_TAG
import com.example.todoapplication.feature.todo.edit.SAVE_TODO_TAG
import com.example.todoapplication.feature.todo.edit.CONFIRM_DELETE_TAG
import com.example.todoapplication.feature.todo.edit.DELETE_DIALOG_TAG
import com.example.todoapplication.feature.todo.edit.DELETE_TODO_TAG
import com.example.todoapplication.feature.todo.list.ADD_TODO_TAG
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TodoEditNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createThenOpenAndUpdateTodoNavigatesBackToList() {
        val initialTitle = "UI 생성 ${System.currentTimeMillis()}"
        val updatedTitle = "$initialTitle 수정"

        composeRule.onNodeWithTag(ADD_TODO_TAG).performClick()
        composeRule.onNodeWithText("TODO 등록").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(EDIT_TITLE_TAG) and
                    androidx.compose.ui.test.isEnabled(),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(EDIT_TITLE_TAG).performTextInput(initialTitle)
        composeRule.onNodeWithTag(EDIT_DATE_TAG).performClick()
        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNodeWithTag(EDIT_TIME_TAG).performClick()
        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNodeWithText("12:00").assertIsDisplayed()
        composeRule.onNodeWithText("일반").performClick()
        composeRule.onNodeWithTag(SAVE_TODO_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(
            androidx.compose.ui.test.hasText(initialTitle),
            timeoutMillis = 5_000,
        )

        composeRule.onNodeWithText(initialTitle).performClick()
        composeRule.onNodeWithText("TODO 수정").assertIsDisplayed()
        composeRule.onNodeWithTag(EDIT_TITLE_TAG).performTextClearance()
        composeRule.onNodeWithTag(EDIT_TITLE_TAG).performTextInput(updatedTitle)
        composeRule.onNodeWithTag(SAVE_TODO_TAG).performClick()

        composeRule.waitUntilAtLeastOneExists(
            androidx.compose.ui.test.hasText(updatedTitle),
            timeoutMillis = 5_000,
        )
        composeRule.onNodeWithText(updatedTitle).assertIsDisplayed()
    }

    @Test
    fun createThenDeleteTodoReturnsToListAndRemovesItem() {
        val title = "UI 삭제 ${System.currentTimeMillis()}"

        composeRule.onNodeWithTag(ADD_TODO_TAG).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag(EDIT_TITLE_TAG) and
                    androidx.compose.ui.test.isEnabled(),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(EDIT_TITLE_TAG).performTextInput(title)
        composeRule.onNodeWithTag(SAVE_TODO_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(
            androidx.compose.ui.test.hasText(title),
            timeoutMillis = 5_000,
        )

        composeRule.onNodeWithText(title).performClick()
        composeRule.onNodeWithText("TODO 수정").assertIsDisplayed()
        composeRule.onNodeWithTag(DELETE_TODO_TAG).performClick()
        composeRule.onNodeWithTag(DELETE_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CONFIRM_DELETE_TAG).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText(title))
                .fetchSemanticsNodes().isEmpty()
        }
    }
}
