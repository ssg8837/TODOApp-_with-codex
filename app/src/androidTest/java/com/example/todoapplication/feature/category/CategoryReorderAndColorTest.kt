package com.example.todoapplication.feature.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.ui.theme.ToDOApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryReorderAndColorTest {
    @get:Rule val rule = createComposeRule()
    private val categories = listOf(
        CategoryManagementItemUiModel(1, "일반", CategoryColor.NEUTRAL, true),
        CategoryManagementItemUiModel(2, "업무", CategoryColor.RED, false),
        CategoryManagementItemUiModel(3, "개인", CategoryColor.GREEN, false),
        CategoryManagementItemUiModel(4, "공부", CategoryColor.BLUE, false),
    )

    @Test
    fun longPressDragPreviewsOrderAndOnlyDropEmitsOnce() {
        val events = mutableListOf<CategoryManagementEvent>()
        screen(events::add)
        rule.onNodeWithTag("category-drag-1").assertDoesNotExist()
        val first = rule.onNodeWithTag("category-row-2").fetchSemanticsNode().boundsInRoot
        val last = rule.onNodeWithTag("category-row-4").fetchSemanticsNode().boundsInRoot
        assertTrue(first.top < last.top)
        rule.onNodeWithTag("category-drag-4").performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, first.center.y - last.center.y), delayMillis = 200)
        }
        rule.runOnIdle { assertTrue(events.isEmpty()) }
        assertTrue(
            rule.onNodeWithTag("category-row-4").fetchSemanticsNode().boundsInRoot.top <
                rule.onNodeWithTag("category-row-2").fetchSemanticsNode().boundsInRoot.top,
        )
        rule.onNodeWithTag("category-drag-4").performTouchInput { up() }
        rule.runOnIdle {
            assertEquals(listOf(CategoryManagementEvent.ReorderCategories(listOf(4, 2, 3))), events)
        }
    }

    @Test
    fun canceledDragDoesNotSaveAndRestoresOrder() {
        val events = mutableListOf<CategoryManagementEvent>()
        screen(events::add)
        rule.onNodeWithTag("category-drag-4").performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, -100f))
            cancel()
        }
        rule.runOnIdle { assertTrue(events.isEmpty()) }
        assertTrue(
            rule.onNodeWithTag("category-row-2").fetchSemanticsNode().boundsInRoot.top <
                rule.onNodeWithTag("category-row-4").fetchSemanticsNode().boundsInRoot.top,
        )
    }

    @Test
    fun accessibilityMoveUsesSameReorderEvent() {
        val events = mutableListOf<CategoryManagementEvent>()
        screen(events::add)
        val up = rule.onNodeWithTag("category-drag-3").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "한 칸 위로 이동" }
        val down = rule.onNodeWithTag("category-drag-2").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "한 칸 아래로 이동" }
        rule.runOnIdle { up.action(); down.action() }
        rule.runOnIdle {
            assertEquals(
                listOf(
                    CategoryManagementEvent.ReorderCategories(listOf(3, 2, 4)),
                    CategoryManagementEvent.ReorderCategories(listOf(3, 2, 4)),
                ), events,
            )
        }
    }

    @Test
    fun paletteShowsExistingColorAndSelectionUpdatesSemantics() {
        var state by mutableStateOf(
            CategoryManagementUiState(
                editorMode = CategoryEditorMode.EDIT,
                categoryNameInput = "업무",
                selectedCategoryColor = CategoryColor.RED,
            ),
        )
        val events = mutableListOf<CategoryManagementEvent>()
        rule.setContent {
            ToDOApplicationTheme {
                CategoryManagementScreen(state, {
                    events += it
                    if (it is CategoryManagementEvent.CategoryColorChanged) {
                        state = state.copy(selectedCategoryColor = it.color)
                    }
                }, {})
            }
        }
        rule.onNodeWithTag("category-palette-toggle").performClick()
        rule.onNodeWithTag("category-palette-RED").assertIsSelected()
        rule.onNodeWithTag("category-palette-BLUE").performClick().assertIsSelected()
        rule.onNodeWithTag(SAVE_CATEGORY_TAG).performClick()
        rule.runOnIdle {
            assertEquals(
                listOf(CategoryManagementEvent.CategoryColorChanged(CategoryColor.BLUE),
                    CategoryManagementEvent.SaveCategory), events,
            )
            assertEquals("업무", state.categoryNameInput)
        }
    }

    @Test
    fun createPaletteStartsNeutralAndOffersAllDomainColors() {
        rule.setContent {
            ToDOApplicationTheme {
                CategoryManagementScreen(
                    CategoryManagementUiState(editorMode = CategoryEditorMode.CREATE),
                    {}, {},
                )
            }
        }
        rule.onNodeWithTag("category-palette-toggle").performClick()
        CategoryColor.entries.forEach {
            rule.onNodeWithTag("category-palette-" + it.name).assertExists()
        }
        rule.onNodeWithTag("category-palette-NEUTRAL").assertIsSelected()
    }

    private fun screen(onEvent: (CategoryManagementEvent) -> Unit) {
        rule.setContent {
            ToDOApplicationTheme {
                CategoryManagementScreen(
                    CategoryManagementUiState(categories = categories, isLoading = false),
                    onEvent, {},
                )
            }
        }
    }
}
