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
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.domain.model.Reminder
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
    fun reminderOptionsAreIndependentWhenTimeExists() {
        val events = mutableListOf<TodoEditEvent>()
        setScreen(state(time = LocalTime.NOON), events::add)

        composeRule.onNodeWithTag(FIFTEEN_MINUTE_REMINDER_TAG).assertIsEnabled().assertIsOff().performClick()
        composeRule.onNodeWithTag(ONE_HOUR_REMINDER_TAG).assertIsEnabled().assertIsOff().performClick()
        composeRule.onNodeWithTag(ONE_DAY_REMINDER_TAG).assertIsEnabled().assertIsOff().performClick()

        composeRule.runOnIdle {
            assertTrue(TodoEditEvent.ToggleReminder(Reminder.FIFTEEN_MINUTES_BEFORE) in events)
            assertTrue(TodoEditEvent.ToggleReminder(Reminder.ONE_HOUR_BEFORE) in events)
            assertTrue(TodoEditEvent.ToggleReminder(Reminder.ONE_DAY_BEFORE) in events)
        }
    }

    @Test
    fun reminderOptionsAreDisabledWithoutTime() {
        setScreen(state(time = null))

        composeRule.onNodeWithTag(FIFTEEN_MINUTE_REMINDER_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(ONE_HOUR_REMINDER_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(ONE_DAY_REMINDER_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(ADD_CUSTOM_REMINDER_TAG).assertIsNotEnabled()
    }

    @Test
    fun editScreenShowsPersistedPresetAndCustomReminderSelections() {
        setScreen(
            state(
                mode = TodoEditMode.EDIT,
                time = LocalTime.NOON,
                selectedReminderOffsets = setOf(
                    Reminder.FIFTEEN_MINUTES_BEFORE,
                    Reminder.ONE_HOUR_BEFORE,
                    Reminder.ONE_DAY_BEFORE,
                    150,
                    4_320,
                ),
            ),
        )

        composeRule.onNodeWithTag(FIFTEEN_MINUTE_REMINDER_TAG).assertIsOn()
        composeRule.onNodeWithTag(ONE_HOUR_REMINDER_TAG).assertIsOn()
        composeRule.onNodeWithTag(ONE_DAY_REMINDER_TAG).assertIsOn()
        composeRule.onNodeWithText("• 3일 전").assertIsDisplayed()
        composeRule.onNodeWithText("• 2시간 30분 전").assertIsDisplayed()
    }

    @Test
    fun customReminderDialogValidatesAddsEditsAndDeletesCustomReminders() {
        val events = mutableListOf<TodoEditEvent>()
        var currentState by mutableStateOf(
            state(
                mode = TodoEditMode.EDIT,
                time = LocalTime.NOON,
                selectedReminderOffsets = setOf(150),
            ),
        )
        composeRule.setContent {
            ToDOApplicationTheme {
                TodoEditScreen(
                    state = currentState,
                    onEvent = { event ->
                        events += event
                        currentState = when (event) {
                            is TodoEditEvent.AddCustomReminder ->
                                currentState.copy(
                                    selectedReminderOffsets = currentState.selectedReminderOffsets + event.offsetMinutes,
                                )
                            is TodoEditEvent.UpdateCustomReminder ->
                                currentState.copy(
                                    selectedReminderOffsets = (currentState.selectedReminderOffsets - event.oldOffsetMinutes) + event.newOffsetMinutes,
                                )
                            is TodoEditEvent.RemoveReminder ->
                                currentState.copy(
                                    selectedReminderOffsets = currentState.selectedReminderOffsets - event.offsetMinutes,
                                )
                            else -> currentState
                        }
                    },
                    onBack = {},
                )
            }
        }

        // 1. 직접 설정 다이얼로그 열기 후 0 입력 시 검증 오류 확인
        composeRule.onNodeWithTag(ADD_CUSTOM_REMINDER_TAG).performClick()
        composeRule.onNodeWithTag(CUSTOM_REMINDER_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CUSTOM_REMINDER_CONFIRM_TAG).performClick()
        composeRule.onNodeWithTag(CUSTOM_REMINDER_ERROR_TAG).assertIsDisplayed()

        // 2. 3일(4320분) 입력 후 추가
        composeRule.onNodeWithTag(CUSTOM_REMINDER_DAYS_INPUT_TAG).performTextInput("3")
        composeRule.onNodeWithTag(CUSTOM_REMINDER_CONFIRM_TAG).performClick()
        composeRule.onNodeWithText("• 3일 전").assertIsDisplayed()

        // 3. 기존 2시간 30분(150분) 알림을 1시간 45분(105분)으로 수정
        composeRule.onNodeWithTag(CUSTOM_REMINDER_EDIT_TAG_PREFIX + 150).performClick()
        composeRule.onNodeWithTag(CUSTOM_REMINDER_HOURS_INPUT_TAG).performTextClearance()
        composeRule.onNodeWithTag(CUSTOM_REMINDER_HOURS_INPUT_TAG).performTextInput("1")
        composeRule.onNodeWithTag(CUSTOM_REMINDER_MINUTES_INPUT_TAG).performTextClearance()
        composeRule.onNodeWithTag(CUSTOM_REMINDER_MINUTES_INPUT_TAG).performTextInput("45")
        composeRule.onNodeWithTag(CUSTOM_REMINDER_CONFIRM_TAG).performClick()
        composeRule.onNodeWithText("• 1시간 45분 전").assertIsDisplayed()

        // 4. 3일 전(4320분) 삭제
        composeRule.onNodeWithTag(CUSTOM_REMINDER_DELETE_TAG_PREFIX + 4_320).performClick()
        composeRule.onAllNodesWithTag(CUSTOM_REMINDER_ITEM_TAG_PREFIX + 4_320).assertCountEquals(0)

        composeRule.runOnIdle {
            assertTrue(TodoEditEvent.AddCustomReminder(4_320) in events)
            assertTrue(TodoEditEvent.UpdateCustomReminder(150, 105) in events)
            assertTrue(TodoEditEvent.RemoveReminder(4_320) in events)
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
        selectedReminderOffsets: Set<Int> = emptySet(),
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
        selectedReminderOffsets = selectedReminderOffsets,
    )
}
