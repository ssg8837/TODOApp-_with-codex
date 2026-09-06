package com.example.todoapplication.feature.todo.edit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import com.example.todoapplication.R
import com.example.todoapplication.ui.theme.toComposeColor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

const val EDIT_TITLE_TAG = "edit-title"
const val EDIT_DATE_TAG = "edit-date"
const val EDIT_TIME_TAG = "edit-time"
const val CLEAR_TIME_TAG = "clear-time"
const val SAVE_TODO_TAG = "save-todo"
const val EDIT_LOADING_TAG = "edit-loading"
const val EDIT_ERROR_TAG = "edit-error"
const val CATEGORY_OPTION_TAG_PREFIX = "category-option-"

@Composable
fun TodoEditScreen(
    state: TodoEditUiState,
    onEvent: (TodoEditEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            EditHeader(
                mode = state.mode,
                isSaving = state.isSaving,
                saveEnabled = !state.isLoading && state.error != TodoEditError.TODO_NOT_FOUND,
                onBack = onBack,
                onSave = { onEvent(TodoEditEvent.Save) },
            )
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().testTag(EDIT_LOADING_TAG),
                )
            }
            state.error?.let { error -> EditErrorMessage(error) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onEvent(TodoEditEvent.TitleChanged(it)) },
                    enabled = !state.isLoading && !state.isSaving,
                    label = { Text(stringResource(R.string.todo_title_label)) },
                    isError = TodoEditValidationError.TITLE_REQUIRED in state.validationErrors,
                    supportingText = if (
                        TodoEditValidationError.TITLE_REQUIRED in state.validationErrors
                    ) {
                        { Text(stringResource(R.string.todo_title_required)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(EDIT_TITLE_TAG),
                )
                LabeledSelection(
                    label = stringResource(R.string.todo_date_label),
                    value = state.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                    enabled = !state.isLoading && !state.isSaving,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.testTag(EDIT_DATE_TAG),
                )
                TimeSelection(
                    time = state.time,
                    enabled = !state.isLoading && !state.isSaving,
                    onSelect = { showTimePicker = true },
                    onClear = { onEvent(TodoEditEvent.TimeCleared) },
                )
                CategorySelection(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    enabled = !state.isLoading && !state.isSaving,
                    showError = TodoEditValidationError.CATEGORY_REQUIRED in state.validationErrors,
                    onSelected = { onEvent(TodoEditEvent.CategoryChanged(it)) },
                )
            }
        }
    }

    if (showDatePicker) {
        EditDatePicker(
            initialDate = state.date,
            onDismiss = { showDatePicker = false },
            onSelected = {
                showDatePicker = false
                onEvent(TodoEditEvent.DateChanged(it))
            },
        )
    }
    if (showTimePicker) {
        EditTimePicker(
            initialTime = state.time,
            onDismiss = { showTimePicker = false },
            onSelected = {
                showTimePicker = false
                onEvent(TodoEditEvent.TimeChanged(it))
            },
        )
    }
}

@Composable
private fun EditHeader(
    mode: TodoEditMode,
    isSaving: Boolean,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(
            text = stringResource(
                if (mode == TodoEditMode.CREATE) R.string.create_todo_title
                else R.string.edit_todo_title,
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Button(
            onClick = onSave,
            enabled = saveEnabled && !isSaving,
            modifier = Modifier.testTag(SAVE_TODO_TAG),
        ) {
            Text(stringResource(if (isSaving) R.string.saving else R.string.save))
        }
    }
}

@Composable
private fun LabeledSelection(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = onClick, enabled = enabled, modifier = modifier) { Text(value) }
    }
}

@Composable
private fun TimeSelection(
    time: LocalTime?,
    enabled: Boolean,
    onSelect: () -> Unit,
    onClear: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.todo_time_label), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onSelect,
                enabled = enabled,
                modifier = Modifier.testTag(EDIT_TIME_TAG),
            ) {
                Text(
                    time?.format(DateTimeFormatter.ofPattern("HH:mm"))
                        ?: stringResource(R.string.todo_no_time),
                )
            }
            if (time != null) {
                TextButton(
                    onClick = onClear,
                    enabled = enabled,
                    modifier = Modifier.testTag(CLEAR_TIME_TAG),
                ) {
                    Text(stringResource(R.string.todo_clear_time))
                }
            }
        }
    }
}

@Composable
private fun CategorySelection(
    categories: List<TodoEditCategoryUiModel>,
    selectedCategoryId: Long?,
    enabled: Boolean,
    showError: Boolean,
    onSelected: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.todo_category_label), style = MaterialTheme.typography.labelLarge)
        categories.forEach { category ->
            val categoryDescription = stringResource(
                R.string.category_color_indicator,
                category.name,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { onSelected(category.id) }
                    .testTag(CATEGORY_OPTION_TAG_PREFIX + category.id)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedCategoryId == category.id,
                    onClick = { onSelected(category.id) },
                    enabled = enabled,
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(category.color.toComposeColor())
                        .semantics { contentDescription = categoryDescription },
                )
                Text(text = category.name, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (showError) {
            Text(
                text = stringResource(R.string.todo_category_required),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSelected: (LocalDate) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                    onSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text(stringResource(R.string.date_picker_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.date_picker_cancel)) }
        },
    ) { DatePicker(state = pickerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTimePicker(
    initialTime: LocalTime?,
    onDismiss: () -> Unit,
    onSelected: (LocalTime) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime?.hour ?: 12,
        initialMinute = initialTime?.minute ?: 0,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSelected(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.time_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.time_picker_cancel)) }
        },
        text = { TimePicker(state = pickerState) },
    )
}

@Composable
private fun EditErrorMessage(error: TodoEditError) {
    Text(
        text = stringResource(error.stringResource),
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp)
            .testTag(EDIT_ERROR_TAG),
    )
}

private val TodoEditError.stringResource: Int
    @StringRes get() = when (this) {
        TodoEditError.TODO_NOT_FOUND -> R.string.error_edit_todo_not_found
        TodoEditError.CATEGORY_NOT_FOUND -> R.string.error_edit_category_not_found
        TodoEditError.INVALID_TODO -> R.string.error_edit_invalid_todo
        TodoEditError.PERSISTENCE_FAILURE -> R.string.error_edit_persistence_failure
        TodoEditError.OPERATION_FAILED -> R.string.error_edit_operation_failed
    }
