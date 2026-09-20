package com.example.todoapplication.feature.todo.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.todoapplication.R
import com.example.todoapplication.ui.theme.toComposeColor

const val CATEGORY_FILTER_TAG = "category-filter"
const val CATEGORY_FILTER_OPTION_PREFIX = "category-filter-option-"
const val ALL_COMPLETION_FILTER_TAG = "completion-filter-all"
const val INCOMPLETE_FILTER_TAG = "completion-filter-incomplete"

@Composable
internal fun TodoListFilters(state: TodoListUiState, onEvent: (TodoListEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.testTag(CATEGORY_FILTER_TAG),
            ) {
                Text(stringResource(R.string.todo_category_filter_label))
                CategoryFilterLabel(selectedCategory)
                Text(" ▾")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.filter_all)) },
                    onClick = {
                        expanded = false
                        onEvent(TodoListEvent.SelectCategory(null))
                    },
                    modifier = Modifier.testTag(CATEGORY_FILTER_OPTION_PREFIX + "all")
                        .semantics { selected = state.selectedCategoryId == null },
                )
                state.categories.forEach { category ->
                    DropdownMenuItem(
                        text = { CategoryFilterLabel(category) },
                        onClick = {
                            expanded = false
                            onEvent(TodoListEvent.SelectCategory(category.id))
                        },
                        modifier = Modifier.testTag(CATEGORY_FILTER_OPTION_PREFIX + category.id)
                            .semantics { selected = state.selectedCategoryId == category.id },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !state.incompleteOnly,
                onClick = { onEvent(TodoListEvent.SetIncompleteOnly(false)) },
                label = { Text(stringResource(R.string.filter_all)) },
                modifier = Modifier.testTag(ALL_COMPLETION_FILTER_TAG),
            )
            FilterChip(
                selected = state.incompleteOnly,
                onClick = { onEvent(TodoListEvent.SetIncompleteOnly(true)) },
                label = { Text(stringResource(R.string.filter_incomplete)) },
                modifier = Modifier.testTag(INCOMPLETE_FILTER_TAG),
            )
        }
    }
}

@Composable
private fun CategoryFilterLabel(category: CategoryFilterUiModel?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (category != null) {
            val description = stringResource(R.string.category_color_indicator, category.name)
            Box(
                Modifier.padding(end = 8.dp).size(12.dp)
                    .background(category.color.toComposeColor(), CircleShape)
                    .testTag("filter-color-" + category.id + "-" + category.color.name)
                    .semantics { contentDescription = description },
            )
        }
        Text(category?.name ?: stringResource(R.string.filter_all))
    }
}
