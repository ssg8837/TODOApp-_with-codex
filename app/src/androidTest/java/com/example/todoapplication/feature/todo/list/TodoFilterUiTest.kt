package com.example.todoapplication.feature.todo.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.ui.theme.ToDOApplicationTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TodoFilterUiTest {
    @get:Rule val rule = createComposeRule()
    private val initial = TodoListUiState(
        selectedDate = LocalDate.of(2026, 9, 20),
        isLoading = false,
        categories = listOf(
            CategoryFilterUiModel(1, "일반", CategoryColor.NEUTRAL),
            CategoryFilterUiModel(3, "개인", CategoryColor.GREEN),
            CategoryFilterUiModel(2, "업무", CategoryColor.BLUE),
        ),
    )

    @Test
    fun categoryDropdownUsesSavedOrderWithNamesColorsAndSelectionEvents() {
        var state by mutableStateOf(initial)
        val events = mutableListOf<TodoListEvent>()
        rule.setContent {
            ToDOApplicationTheme {
                TodoListScreen(state, {
                    events += it
                    if (it is TodoListEvent.SelectCategory) state = state.copy(selectedCategoryId = it.categoryId)
                })
            }
        }
        rule.onNodeWithTag(CATEGORY_FILTER_TAG).performClick()
        val tops = listOf("all", "1", "3", "2").map {
            rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + it)
                .assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
        }
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
        rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + "all").assertIsSelected()
        rule.onNodeWithTag("filter-color-2-BLUE", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("업무").assertIsDisplayed()
        rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + "2").performClick()
        rule.onNodeWithTag(CATEGORY_FILTER_TAG).performClick()
        rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + "2").assertIsSelected()
        rule.onNodeWithTag(CATEGORY_FILTER_OPTION_PREFIX + "all").performClick()
        rule.runOnIdle {
            assertEquals(listOf(TodoListEvent.SelectCategory(2), TodoListEvent.SelectCategory(null)), events)
        }
    }

    @Test
    fun completionChipsExposeSelectedStateAndEmitBothChoices() {
        var state by mutableStateOf(initial)
        val events = mutableListOf<TodoListEvent>()
        rule.setContent {
            ToDOApplicationTheme {
                TodoListScreen(state, {
                    events += it
                    if (it is TodoListEvent.SetIncompleteOnly) state = state.copy(incompleteOnly = it.enabled)
                })
            }
        }
        rule.onNodeWithTag(ALL_COMPLETION_FILTER_TAG).assertIsSelected()
        rule.onNodeWithTag(INCOMPLETE_FILTER_TAG).performClick().assertIsSelected()
        rule.onNodeWithTag(ALL_COMPLETION_FILTER_TAG).performClick().assertIsSelected()
        rule.runOnIdle {
            assertEquals(listOf(TodoListEvent.SetIncompleteOnly(true), TodoListEvent.SetIncompleteOnly(false)), events)
        }
    }

    @Test
    fun filteredEmptyResultHasDistinctMessage() {
        rule.setContent {
            ToDOApplicationTheme {
                TodoListScreen(initial.copy(
                    selectedCategoryId = 2,
                    emptyState = TodoListEmptyState.NO_MATCHES,
                ), {})
            }
        }
        rule.onNodeWithText("조건에 해당하는 TODO가 없습니다.").assertIsDisplayed()
        rule.onNodeWithText("등록된 TODO가 없습니다.").assertDoesNotExist()
    }
}
