package com.example.todoapplication.feature.todo.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.ui.theme.ToDOApplicationTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoEditScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createScreenRendersInputsAndEmitsTitleCategoryAndSaveEvents() {
        val events = mutableListOf<TodoEditEvent>()
        setScreen(state(), events::add)

        composeRule.onNodeWithText("TODO 등록").assertIsDisplayed()
        composeRule.onNodeWithTag(EDIT_TITLE_TAG).performTextInput("새 TODO")
        composeRule.onNodeWithTag(CATEGORY_OPTION_TAG_PREFIX + 2).performClick()
        composeRule.onNodeWithTag(SAVE_TODO_TAG).assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertTrue(TodoEditEvent.TitleChanged("새 TODO") in events)
            assertTrue(TodoEditEvent.CategoryChanged(2) in events)
            assertTrue(TodoEditEvent.Save in events)
        }
    }

    @Test
    fun editScreenRendersExistingValuesAndCanClearTime() {
        val events = mutableListOf<TodoEditEvent>()
        val date = LocalDate.of(2026, 9, 7)
        setScreen(
            state(
                mode = TodoEditMode.EDIT,
                title = "기존 TODO",
                date = date,
                time = LocalTime.of(14, 20),
                selectedCategoryId = 2,
            ),
            events::add,
        )

        composeRule.onNodeWithText("TODO 수정").assertIsDisplayed()
        composeRule.onNodeWithText("기존 TODO").assertIsDisplayed()
        composeRule.onNodeWithText(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
            .assertIsDisplayed()
        composeRule.onNodeWithText("14:20").assertIsDisplayed()
        composeRule.onNodeWithText("업무").assertIsDisplayed()
        composeRule.onNodeWithTag(CLEAR_TIME_TAG).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(TodoEditEvent.TimeCleared), events)
        }
    }

    @Test
    fun dateAndTimePickerConfirmationsEmitEventsAtUiBoundary() {
        val events = mutableListOf<TodoEditEvent>()
        val date = LocalDate.of(2026, 9, 6)
        setScreen(state(date = date), events::add)

        composeRule.onNodeWithTag(EDIT_DATE_TAG).performClick()
        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNodeWithTag(EDIT_TIME_TAG).performClick()
        composeRule.onNodeWithText("선택").performClick()

        composeRule.runOnIdle {
            assertEquals(TodoEditEvent.DateChanged(date), events[0])
            assertEquals(TodoEditEvent.TimeChanged(LocalTime.of(12, 0)), events[1])
        }
    }

    @Test
    fun savingDisablesSaveAndValidationErrorsAreVisible() {
        setScreen(
            state(
                isSaving = true,
                validationErrors = setOf(
                    TodoEditValidationError.TITLE_REQUIRED,
                    TodoEditValidationError.CATEGORY_REQUIRED,
                ),
            ),
        )

        composeRule.onNodeWithTag(SAVE_TODO_TAG).assertIsNotEnabled()
        composeRule.onNodeWithText("제목을 입력해주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("카테고리를 선택해주세요.").assertIsDisplayed()
    }

    @Test
    fun createModeDoesNotShowDeleteButton() {
        setScreen(state())

        composeRule.onAllNodesWithTag(DELETE_TODO_TAG).assertCountEquals(0)
    }

    @Test
    fun editDeleteRequestShowsDialogAndCancelClosesIt() {
        val events = mutableListOf<TodoEditEvent>()
        var currentState by mutableStateOf(state(mode = TodoEditMode.EDIT, title = "기존"))
        composeRule.setContent {
            ToDOApplicationTheme {
                TodoEditScreen(
                    state = currentState,
                    onEvent = { event ->
                        events += event
                        currentState = when (event) {
                            TodoEditEvent.RequestDelete -> {
                                currentState.copy(showDeleteConfirmation = true)
                            }
                            TodoEditEvent.CancelDelete -> {
                                currentState.copy(showDeleteConfirmation = false)
                            }
                            else -> currentState
                        }
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(DELETE_TODO_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(DELETE_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("삭제한 TODO는 복구할 수 없습니다.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(CANCEL_DELETE_TAG).performClick()
        composeRule.onAllNodesWithTag(DELETE_DIALOG_TAG).assertCountEquals(0)

        composeRule.runOnIdle {
            assertTrue(TodoEditEvent.RequestDelete in events)
            assertTrue(TodoEditEvent.CancelDelete in events)
        }
    }

    @Test
    fun deleteConfirmationEmitsConfirmEvent() {
        val events = mutableListOf<TodoEditEvent>()
        setScreen(
            state(
                mode = TodoEditMode.EDIT,
                title = "기존",
                showDeleteConfirmation = true,
            ),
            events::add,
        )

        composeRule.onNodeWithTag(CONFIRM_DELETE_TAG).performClick()

        composeRule.runOnIdle {
            assertTrue(TodoEditEvent.ConfirmDelete in events)
        }
    }

    @Test
    fun deletingDisablesSaveDeleteAndDialogActions() {
        setScreen(
            state(
                mode = TodoEditMode.EDIT,
                title = "기존",
                showDeleteConfirmation = true,
                isDeleting = true,
            ),
        )

        composeRule.onNodeWithTag(SAVE_TODO_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(DELETE_TODO_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(CANCEL_DELETE_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(CONFIRM_DELETE_TAG).assertIsNotEnabled()
    }

    private fun setScreen(
        state: TodoEditUiState,
        onEvent: (TodoEditEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            ToDOApplicationTheme {
                TodoEditScreen(state = state, onEvent = onEvent, onBack = {})
            }
        }
    }

    private fun state(
        mode: TodoEditMode = TodoEditMode.CREATE,
        title: String = "",
        date: LocalDate = LocalDate.of(2026, 9, 6),
        time: LocalTime? = null,
        selectedCategoryId: Long? = 1,
        isSaving: Boolean = false,
        showDeleteConfirmation: Boolean = false,
        isDeleting: Boolean = false,
        validationErrors: Set<TodoEditValidationError> = emptySet(),
    ) = TodoEditUiState(
        mode = mode,
        todoId = if (mode == TodoEditMode.EDIT) 10 else null,
        title = title,
        date = date,
        time = time,
        selectedCategoryId = selectedCategoryId,
        categories = listOf(
            TodoEditCategoryUiModel(1, "일반", CategoryColor.NEUTRAL),
            TodoEditCategoryUiModel(2, "업무", CategoryColor.BLUE),
        ),
        isLoading = false,
        isSaving = isSaving,
        showDeleteConfirmation = showDeleteConfirmation,
        isDeleting = isDeleting,
        validationErrors = validationErrors,
    )
}
