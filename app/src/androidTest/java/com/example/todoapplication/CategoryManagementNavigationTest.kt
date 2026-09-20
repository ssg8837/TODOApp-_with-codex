package com.example.todoapplication

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.application.service.ServiceResult
import com.example.todoapplication.feature.category.ADD_CATEGORY_TAG
import com.example.todoapplication.feature.category.CATEGORY_MANAGEMENT_BACK_TAG
import com.example.todoapplication.feature.category.CATEGORY_NAME_INPUT_TAG
import com.example.todoapplication.feature.category.CONFIRM_DELETE_CATEGORY_TAG
import com.example.todoapplication.feature.category.DELETE_CATEGORY_TAG_PREFIX
import com.example.todoapplication.feature.category.EDIT_CATEGORY_TAG_PREFIX
import com.example.todoapplication.feature.category.SAVE_CATEGORY_TAG
import com.example.todoapplication.feature.todo.edit.EDIT_TITLE_TAG
import com.example.todoapplication.feature.todo.edit.SAVE_TODO_TAG
import com.example.todoapplication.feature.todo.list.ADD_TODO_TAG
import com.example.todoapplication.feature.todo.list.MANAGE_CATEGORIES_TAG
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CategoryManagementNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun categoryCrudUpdatesTodoAndDeletionReassignsItToGeneral() {
        val suffix = System.currentTimeMillis()
        val categoryName = "업무$suffix"
        val updatedName = "회사$suffix"
        val todoTitle = "Category 통합$suffix"

        composeRule.onNodeWithTag(MANAGE_CATEGORIES_TAG).performClick()
        composeRule.onNodeWithText("Category 관리").assertIsDisplayed()
        composeRule.onNodeWithTag(ADD_CATEGORY_TAG).performClick()
        composeRule.onNodeWithTag(CATEGORY_NAME_INPUT_TAG).performTextInput(categoryName)
        composeRule.onNodeWithTag(SAVE_CATEGORY_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(categoryName), timeoutMillis = 5_000)

        val categoryId = categoryIdByName(categoryName)
        assertNotNull(categoryId)

        composeRule.onNodeWithTag(CATEGORY_MANAGEMENT_BACK_TAG).performClick()
        composeRule.onNodeWithTag(ADD_TODO_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag(EDIT_TITLE_TAG), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(EDIT_TITLE_TAG).performTextInput(todoTitle)
        composeRule.onNodeWithText(categoryName).performClick()
        composeRule.onNodeWithTag(SAVE_TODO_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(todoTitle), timeoutMillis = 5_000)

        composeRule.onNodeWithTag(MANAGE_CATEGORIES_TAG).performClick()
        composeRule.onNodeWithTag(EDIT_CATEGORY_TAG_PREFIX + categoryId).performClick()
        composeRule.onNodeWithTag(CATEGORY_NAME_INPUT_TAG).performTextClearance()
        composeRule.onNodeWithTag(CATEGORY_NAME_INPUT_TAG).performTextInput(updatedName)
        composeRule.onNodeWithTag(SAVE_CATEGORY_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(updatedName), timeoutMillis = 5_000)
        composeRule.onNodeWithTag(CATEGORY_MANAGEMENT_BACK_TAG).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(updatedName), timeoutMillis = 5_000)
        composeRule.onNodeWithText(todoTitle).assertIsDisplayed()

        composeRule.onNodeWithTag(MANAGE_CATEGORIES_TAG).performClick()
        composeRule.onNodeWithTag(DELETE_CATEGORY_TAG_PREFIX + categoryId).performClick()
        composeRule.onNodeWithTag(CONFIRM_DELETE_CATEGORY_TAG).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(updatedName)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(CATEGORY_MANAGEMENT_BACK_TAG).performClick()
        composeRule.onNodeWithText(todoTitle).assertIsDisplayed()
        composeRule.onNodeWithText("일반").assertIsDisplayed()
    }

    private fun categoryIdByName(name: String): Long? = runBlocking {
        val application = composeRule.activity.application as TodoApplication
        when (val result = application.container.categoryService.observeAll().first()) {
            is ServiceResult.Success -> result.value.firstOrNull { it.name == name }?.id
            is ServiceResult.Failure -> null
        }
    }
}
