package com.example.todoapplication.feature.todo.list

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todoapplication.R
import com.example.todoapplication.ui.theme.toComposeColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

const val PREVIOUS_DATE_TAG = "previous-date"
const val NEXT_DATE_TAG = "next-date"
const val SELECT_DATE_TAG = "select-date"
const val LOADING_TAG = "todo-list-loading"
const val ERROR_TAG = "todo-list-error"
const val CATEGORY_COLOR_TAG_PREFIX = "category-color-"
const val ADD_TODO_TAG = "add-todo"
const val TODO_ITEM_TAG_PREFIX = "todo-item-"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    state: TodoListUiState,
    onEvent: (TodoListEvent) -> Unit,
    onAddTodo: () -> Unit = {},
    onEditTodo: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var isDatePickerVisible by remember { mutableStateOf(false) }
    val addTodoDescription = stringResource(R.string.add_todo_description)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTodo,
                modifier = Modifier
                    .testTag(ADD_TODO_TAG)
                    .semantics { contentDescription = addTodoDescription },
            ) {
                Text(text = stringResource(R.string.add_todo))
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            Text(
                text = stringResource(R.string.todo_list_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            DateHeader(
                selectedDate = state.selectedDate,
                onPreviousDate = { onEvent(TodoListEvent.PreviousDate) },
                onNextDate = { onEvent(TodoListEvent.NextDate) },
                onSelectDate = { isDatePickerVisible = true },
            )
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(LOADING_TAG),
                )
            }
            state.error?.let { error ->
                ErrorMessage(error = error)
            }
            TodoListContent(
                todos = state.todos,
                showEmptyState = !state.isLoading && state.error == null,
                onCompletedChange = { id, completed ->
                    onEvent(TodoListEvent.SetCompleted(id, completed))
                },
                onEditTodo = onEditTodo,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (isDatePickerVisible) {
        TodoDatePicker(
            initialDate = state.selectedDate,
            onDismiss = { isDatePickerVisible = false },
            onDateSelected = { date ->
                isDatePickerVisible = false
                onEvent(TodoListEvent.SelectDate(date))
            },
        )
    }
}

@Composable
private fun DateHeader(
    selectedDate: LocalDate,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onSelectDate: () -> Unit,
) {
    val previousDescription = stringResource(R.string.previous_date)
    val nextDescription = stringResource(R.string.next_date)
    val selectDescription = stringResource(R.string.select_date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onPreviousDate,
            modifier = Modifier
                .testTag(PREVIOUS_DATE_TAG)
                .semantics { contentDescription = previousDescription },
        ) {
            Text(text = "‹")
        }
        Button(
            onClick = onSelectDate,
            modifier = Modifier
                .weight(1f)
                .testTag(SELECT_DATE_TAG)
                .semantics { contentDescription = selectDescription },
        ) {
            Text(text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)))
        }
        TextButton(
            onClick = onNextDate,
            modifier = Modifier
                .testTag(NEXT_DATE_TAG)
                .semantics { contentDescription = nextDescription },
        ) {
            Text(text = "›")
        }
    }
}

@Composable
private fun TodoListContent(
    todos: List<TodoListItemUiModel>,
    showEmptyState: Boolean,
    onCompletedChange: (Long, Boolean) -> Unit,
    onEditTodo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (todos.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (showEmptyState) {
                Text(
                    text = stringResource(R.string.empty_todo_list),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items = todos, key = TodoListItemUiModel::id) { todo ->
            TodoListItem(
                todo = todo,
                onCompletedChange = { completed -> onCompletedChange(todo.id, completed) },
                onEdit = { onEditTodo(todo.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TodoListItem(
    todo: TodoListItemUiModel,
    onCompletedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val completionDescription = stringResource(R.string.todo_completion, todo.title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = onCompletedChange,
            modifier = Modifier.semantics {
                contentDescription = completionDescription
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .testTag(TODO_ITEM_TAG_PREFIX + todo.id)
                .padding(top = 4.dp),
        ) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
            )
            todo.time?.let { time ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val categoryDescription = stringResource(
                    R.string.category_color_indicator,
                    todo.categoryName,
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(todo.categoryColor.toComposeColor())
                        .testTag(CATEGORY_COLOR_TAG_PREFIX + todo.categoryColor.name)
                        .semantics { contentDescription = categoryDescription },
                )
                Text(
                    text = todo.categoryName,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) {
                Text(stringResource(R.string.date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.date_picker_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun ErrorMessage(error: TodoListError) {
    Text(
        text = stringResource(error.stringResource),
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(ERROR_TAG),
    )
}

private val TodoListError.stringResource: Int
    @StringRes get() = when (this) {
        TodoListError.TODO_NOT_FOUND -> R.string.error_todo_not_found
        TodoListError.CATEGORY_NOT_FOUND -> R.string.error_category_not_found
        TodoListError.INVALID_TODO -> R.string.error_invalid_todo
        TodoListError.PERSISTENCE_FAILURE -> R.string.error_persistence_failure
        TodoListError.OPERATION_FAILED -> R.string.error_operation_failed
    }
