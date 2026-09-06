package com.example.todoapplication.feature.todo.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.ui.theme.ToDOApplicationTheme
import com.example.todoapplication.ui.theme.toComposeColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersSelectedDateTodosTimeAndCategory() {
        val selectedDate = LocalDate.of(2026, 9, 6)

        setScreen(
            state = state(
                selectedDate = selectedDate,
                todos = listOf(
                    item(id = 1, title = "프로젝트 회의", time = LocalTime.of(10, 0)),
                    item(id = 2, title = "시간 없는 할 일", time = null),
                ),
            ),
        )

        composeRule.onNodeWithText(
            selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
        ).assertIsDisplayed()
        composeRule.onNodeWithText("프로젝트 회의").assertIsDisplayed()
        composeRule.onNodeWithText("10:00").assertIsDisplayed()
        composeRule.onNodeWithText("시간 없는 할 일").assertIsDisplayed()
        composeRule.onNodeWithText("00:00").assertIsNotDisplayed()
        composeRule.onAllNodesWithText("업무", useUnmergedTree = true).assertCountEquals(2)
        composeRule.onAllNodesWithContentDescription("업무 카테고리 색상")
            .assertCountEquals(2)
    }

    @Test
    fun rendersEmptyState() {
        setScreen(state = state(todos = emptyList()))
        composeRule.onNodeWithText("등록된 TODO가 없습니다.").assertIsDisplayed()
    }

    @Test
    fun rendersLoadingState() {
        setScreen(state = state(isLoading = true))
        composeRule.onNodeWithTag(LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun rendersErrorState() {
        setScreen(state = state(error = TodoListError.PERSISTENCE_FAILURE))
        composeRule.onNodeWithTag(ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("목록을 불러오거나 변경하지 못했습니다.").assertIsDisplayed()
    }

    @Test
    fun previousNextAndCompletionActionsEmitEvents() {
        val events = mutableListOf<TodoListEvent>()
        setScreen(
            state = state(todos = listOf(item(isCompleted = true))),
            onEvent = events::add,
        )

        composeRule.onNodeWithTag(PREVIOUS_DATE_TAG).performClick()
        composeRule.onNodeWithTag(NEXT_DATE_TAG).performClick()
        composeRule.onNodeWithContentDescription("프로젝트 회의 완료 상태")
            .assertIsOn()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    TodoListEvent.PreviousDate,
                    TodoListEvent.NextDate,
                    TodoListEvent.SetCompleted(1, false),
                ),
                events,
            )
        }
    }

    @Test
    fun addAndTodoItemClicksUseSeparateNavigationCallbacks() {
        var addClicks = 0
        val editedIds = mutableListOf<Long>()
        setScreen(
            state = state(todos = listOf(item(id = 7))),
            onAddTodo = { addClicks++ },
            onEditTodo = editedIds::add,
        )

        composeRule.onNodeWithTag(ADD_TODO_TAG).performClick()
        composeRule.onNodeWithTag(TODO_ITEM_TAG_PREFIX + 7).performClick()

        composeRule.runOnIdle {
            assertEquals(1, addClicks)
            assertEquals(listOf(7L), editedIds)
        }
    }

    @Test
    fun datePickerConfirmationEmitsSelectedDateEvent() {
        val selectedDate = LocalDate.of(2026, 9, 6)
        val events = mutableListOf<TodoListEvent>()
        setScreen(state = state(selectedDate = selectedDate), onEvent = events::add)

        composeRule.onNodeWithTag(SELECT_DATE_TAG).performClick()
        composeRule.onNodeWithText("선택").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(TodoListEvent.SelectDate(selectedDate)), events)
        }
    }

    @Test
    fun categoryColorMappingStaysInUiLayerAndUsesExpectedPalette() {
        assertEquals(Color(0xFF1565C0), CategoryColor.BLUE.toComposeColor())
        assertEquals(Color(0xFF616161), CategoryColor.NEUTRAL.toComposeColor())
    }

    private fun setScreen(
        state: TodoListUiState,
        onEvent: (TodoListEvent) -> Unit = {},
        onAddTodo: () -> Unit = {},
        onEditTodo: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            ToDOApplicationTheme {
                TodoListScreen(
                    state = state,
                    onEvent = onEvent,
                    onAddTodo = onAddTodo,
                    onEditTodo = onEditTodo,
                )
            }
        }
    }

    private fun state(
        selectedDate: LocalDate = LocalDate.of(2026, 9, 6),
        todos: List<TodoListItemUiModel> = emptyList(),
        isLoading: Boolean = false,
        error: TodoListError? = null,
    ) = TodoListUiState(
        selectedDate = selectedDate,
        todos = todos,
        isLoading = isLoading,
        error = error,
    )

    private fun item(
        id: Long = 1,
        title: String = "프로젝트 회의",
        time: LocalTime? = LocalTime.of(10, 0),
        isCompleted: Boolean = false,
    ) = TodoListItemUiModel(
        id = id,
        title = title,
        time = time,
        isCompleted = isCompleted,
        categoryName = "업무",
        categoryColor = CategoryColor.BLUE,
    )
}
