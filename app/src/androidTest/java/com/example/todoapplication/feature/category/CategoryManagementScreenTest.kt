package com.example.todoapplication.feature.category

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.domain.model.CategoryColor
import com.example.todoapplication.ui.theme.ToDOApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryManagementScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsSystemAndUserCategoriesWithColorButNoSystemActions() {
        setScreen(state())
        composeRule.onNodeWithText("일반").assertIsDisplayed()
        composeRule.onNodeWithText("시스템 기본 Category").assertIsDisplayed()
        composeRule.onNodeWithText("업무").assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_COLOR_TAG_PREFIX + 1).assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_COLOR_TAG_PREFIX + 2).assertIsDisplayed()
        composeRule.onAllNodesWithTag(EDIT_CATEGORY_TAG_PREFIX + 1).assertCountEquals(0)
        composeRule.onAllNodesWithTag(DELETE_CATEGORY_TAG_PREFIX + 1).assertCountEquals(0)
        composeRule.onNodeWithTag(EDIT_CATEGORY_TAG_PREFIX + 2).assertIsDisplayed()
        composeRule.onNodeWithTag(DELETE_CATEGORY_TAG_PREFIX + 2).assertIsDisplayed()
    }

    @Test
    fun addButtonEmitsCreateEvent() {
        val events = mutableListOf<CategoryManagementEvent>()
        setScreen(state(), events::add)
        composeRule.onNodeWithTag(ADD_CATEGORY_TAG).performClick()
        composeRule.runOnIdle { assertEquals(listOf(CategoryManagementEvent.RequestCreate), events) }
    }

    @Test
    fun createDialogEmitsSaveEvent() {
        val events = mutableListOf<CategoryManagementEvent>()
        setScreen(
            state().copy(editorMode = CategoryEditorMode.CREATE, categoryNameInput = "업무"),
            events::add,
        )
        composeRule.onNodeWithTag(CATEGORY_EDITOR_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Category 추가").assertIsDisplayed()
        composeRule.onNodeWithTag(SAVE_CATEGORY_TAG).performClick()
        composeRule.runOnIdle { assertEquals(CategoryManagementEvent.SaveCategory, events.last()) }
    }

    @Test
    fun editDialogShowsExistingNameInput() {
        setScreen(
            state().copy(
                editorMode = CategoryEditorMode.EDIT,
                editingCategoryId = 2,
                categoryNameInput = "업무",
            ),
        )
        composeRule.onNodeWithText("Category 수정").assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_NAME_INPUT_TAG).assertIsDisplayed()
    }

    @Test
    fun deleteDialogShowsTodoReassignmentExplanation() {
        setScreen(
            state().copy(showDeleteConfirmation = true, deleteTargetCategoryId = 2),
        )
        composeRule.onNodeWithTag(DELETE_CATEGORY_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(
            "이 Category를 삭제해도 기존 TODO는 삭제되지 않습니다.\n" +
                "해당 TODO의 Category는 ‘일반’으로 변경됩니다.",
        ).assertIsDisplayed()
    }

    @Test
    fun loadingErrorAndSavingButtonsAreRendered() {
        setScreen(
            state().copy(
                isLoading = true,
                error = CategoryManagementError.PERSISTENCE_FAILURE,
                editorMode = CategoryEditorMode.CREATE,
                isSaving = true,
            ),
        )
        composeRule.onNodeWithTag(CATEGORY_LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SAVE_CATEGORY_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(ADD_CATEGORY_TAG).assertIsNotEnabled()
    }

    @Test
    fun deletingDisablesDeleteConfirmation() {
        setScreen(
            state().copy(
                showDeleteConfirmation = true,
                deleteTargetCategoryId = 2,
                isDeleting = true,
            ),
        )
        composeRule.onNodeWithTag(CONFIRM_DELETE_CATEGORY_TAG).assertIsNotEnabled()
    }

    private fun setScreen(
        state: CategoryManagementUiState,
        onEvent: (CategoryManagementEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            ToDOApplicationTheme {
                CategoryManagementScreen(state, onEvent, onBack = {})
            }
        }
    }

    private fun state() = CategoryManagementUiState(
        categories = listOf(
            CategoryManagementItemUiModel(1, "일반", CategoryColor.NEUTRAL, true),
            CategoryManagementItemUiModel(2, "업무", CategoryColor.BLUE, false),
        ),
        isLoading = false,
    )
}
