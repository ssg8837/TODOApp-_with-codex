package com.example.todoapplication

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.feature.todo.detail.DETAIL_CONFIRM_BUTTON_TAG
import com.example.todoapplication.feature.todo.edit.EDIT_TITLE_TAG
import com.example.todoapplication.feature.todo.edit.SAVE_TODO_TAG
import com.example.todoapplication.feature.todo.list.ADD_TODO_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TodoDetailNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun selectingTodoFromListNavigatesToDetailAndConfirmReturnsToList() {
        val title = "확인 화면 테스트 ${System.currentTimeMillis()}"

        // TODO 생성
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

        // 목록에서 항목 선택 -> Detail로 이동
        composeRule.onNodeWithText(title).performClick()
        composeRule.waitUntilAtLeastOneExists(
            androidx.compose.ui.test.hasText("TODO 확인"),
            timeoutMillis = 5_000,
        )
        composeRule.onNodeWithText("TODO 확인").assertIsDisplayed()
        composeRule.onNodeWithText(title).assertIsDisplayed()

        // 확인 버튼 클릭 -> 목록으로 복귀
        composeRule.onNodeWithTag(DETAIL_CONFIRM_BUTTON_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(
            androidx.compose.ui.test.hasText("TODO"),
            timeoutMillis = 5_000,
        )
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }
}

