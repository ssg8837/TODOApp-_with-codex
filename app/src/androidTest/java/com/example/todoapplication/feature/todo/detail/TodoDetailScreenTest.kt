package com.example.todoapplication.feature.todo.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.ui.theme.ToDOApplicationTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysAllDetailFieldsWhenLoaded() {
        var editClicked = false
        var confirmClicked = false

        val state = TodoDetailUiState(
            todoId = 1L,
            title = "운동하기",
            date = LocalDate.of(2026, 10, 24),
            time = LocalTime.of(18, 0),
            categoryName = "건강",
            categoryColor = CategoryColor.GREEN,
            isCompleted = false,
            reminders = listOf(Reminder.ONE_DAY_BEFORE, 150, Reminder.ONE_HOUR_BEFORE, Reminder.FIFTEEN_MINUTES_BEFORE),
            isLoading = false,
            error = null,
        )

        composeRule.setContent {
            ToDOApplicationTheme {
                TodoDetailScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    onEdit = { editClicked = true },
                    onConfirm = { confirmClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("TODO 확인").assertIsDisplayed()
        composeRule.onNodeWithText("운동하기").assertIsDisplayed()
        composeRule.onNodeWithText("18:00").assertIsDisplayed()
        composeRule.onNodeWithText("건강").assertIsDisplayed()
        composeRule.onNodeWithText("미완료").assertIsDisplayed()
        composeRule.onNodeWithText("- 1일 전").assertIsDisplayed()
        composeRule.onNodeWithText("- 2시간 30분 전").assertIsDisplayed()
        composeRule.onNodeWithText("- 1시간 전").assertIsDisplayed()
        composeRule.onNodeWithText("- 15분 전").assertIsDisplayed()

        // 수정 버튼 클릭
        composeRule.onNodeWithTag(DETAIL_EDIT_BUTTON_TAG).assertIsEnabled().performClick()
        assertTrue(editClicked)

        // 확인 버튼 클릭
        composeRule.onNodeWithTag(DETAIL_CONFIRM_BUTTON_TAG).assertIsEnabled().performClick()
        assertTrue(confirmClicked)
    }

    @Test
    fun displaysNoTimeAndNoReminderWhenAbsent() {
        val state = TodoDetailUiState(
            todoId = 2L,
            title = "책 읽기",
            date = LocalDate.of(2026, 10, 24),
            time = null,
            categoryName = "일반",
            categoryColor = CategoryColor.NEUTRAL,
            isCompleted = true,
            reminders = emptyList(),
            isLoading = false,
            error = null,
        )

        composeRule.setContent {
            ToDOApplicationTheme {
                TodoDetailScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    onEdit = {},
                    onConfirm = {},
                )
            }
        }

        composeRule.onNodeWithText("시간 없음").assertIsDisplayed()
        composeRule.onNodeWithText("알림 없음").assertIsDisplayed()
        composeRule.onNodeWithText("완료").assertIsDisplayed()
    }

    @Test
    fun displaysErrorAndDisablesEditWhenTodoNotFound() {
        var confirmClicked = false

        val state = TodoDetailUiState(
            todoId = 99L,
            isLoading = false,
            error = TodoDetailError.TODO_NOT_FOUND,
        )

        composeRule.setContent {
            ToDOApplicationTheme {
                TodoDetailScreen(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    onEdit = {},
                    onConfirm = { confirmClicked = true },
                )
            }
        }

        composeRule.onNodeWithTag(DETAIL_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("TODO를 찾을 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithTag(DETAIL_EDIT_BUTTON_TAG).assertIsNotEnabled()

        // 사용자는 확인 버튼으로 나갈 수 있어야 함
        composeRule.onNodeWithTag(DETAIL_CONFIRM_BUTTON_TAG).assertIsEnabled().performClick()
        assertTrue(confirmClicked)
    }
}

