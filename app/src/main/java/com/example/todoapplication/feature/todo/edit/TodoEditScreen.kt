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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.todoapplication.R
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.reminder.ReminderDurationFormatter
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
const val DELETE_TODO_TAG = "delete-todo"
const val DELETE_DIALOG_TAG = "delete-dialog"
const val CANCEL_DELETE_TAG = "cancel-delete"
const val CONFIRM_DELETE_TAG = "confirm-delete"
const val ONE_DAY_REMINDER_TAG = "reminder-one-day"
const val ONE_HOUR_REMINDER_TAG = "reminder-one-hour"
const val FIFTEEN_MINUTE_REMINDER_TAG = "reminder-fifteen-minutes"
const val ADD_CUSTOM_REMINDER_TAG = "reminder-add-custom"
const val CUSTOM_REMINDER_DIALOG_TAG = "custom-reminder-dialog"
const val CUSTOM_REMINDER_DAYS_INPUT_TAG = "custom-reminder-days-input"
const val CUSTOM_REMINDER_HOURS_INPUT_TAG = "custom-reminder-hours-input"
const val CUSTOM_REMINDER_MINUTES_INPUT_TAG = "custom-reminder-minutes-input"
const val CUSTOM_REMINDER_CONFIRM_TAG = "custom-reminder-confirm"
const val CUSTOM_REMINDER_CANCEL_TAG = "custom-reminder-cancel"
const val CUSTOM_REMINDER_ERROR_TAG = "custom-reminder-error"
const val CUSTOM_REMINDER_ITEM_TAG_PREFIX = "custom-reminder-item-"
const val CUSTOM_REMINDER_EDIT_TAG_PREFIX = "custom-reminder-edit-"
const val CUSTOM_REMINDER_DELETE_TAG_PREFIX = "custom-reminder-delete-"

/**
 * [TodoEditUiState]를 렌더링하고 입력 callback만 방출하는 stateless 편집 Screen.
 * 알림 권한은 사용자가 처음 Reminder를 활성화할 때 Route에 요청하도록 callback을 전달한다.
 */
@Composable
fun TodoEditScreen(
    state: TodoEditUiState,
    onEvent: (TodoEditEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var customReminderDialogEditingOffset by remember { mutableStateOf<Int?>(null) }
    var showCustomReminderDialog by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            EditHeader(
                mode = state.mode,
                isSaving = state.isSaving,
                saveEnabled = !state.isLoading &&
                    !state.isDeleting &&
                    state.error != TodoEditError.TODO_NOT_FOUND,
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
                    enabled = !state.isLoading && !state.isSaving && !state.isDeleting,
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
                    enabled = !state.isLoading && !state.isSaving && !state.isDeleting,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.testTag(EDIT_DATE_TAG),
                )
                TimeSelection(
                    time = state.time,
                    enabled = !state.isLoading && !state.isSaving && !state.isDeleting,
                    onSelect = { showTimePicker = true },
                    onClear = { onEvent(TodoEditEvent.TimeCleared) },
                )
                ReminderSelection(
                    timeAvailable = state.time != null,
                    fifteenMinutes = state.remindFifteenMinutesBefore,
                    oneHour = state.remindOneHourBefore,
                    oneDay = state.remindOneDayBefore,
                    customOffsets = state.customReminderOffsets,
                    enabled = !state.isLoading && !state.isSaving && !state.isDeleting,
                    onTogglePreset = { offset -> onEvent(TodoEditEvent.ToggleReminder(offset)) },
                    onRequestAddCustom = {
                        customReminderDialogEditingOffset = null
                        showCustomReminderDialog = true
                    },
                    onRequestEditCustom = { offset ->
                        customReminderDialogEditingOffset = offset
                        showCustomReminderDialog = true
                    },
                    onRemoveCustom = { offset -> onEvent(TodoEditEvent.RemoveReminder(offset)) },
                )
                if (state.notificationPermissionDenied) {
                    Text(
                        text = stringResource(R.string.notification_permission_denied_message),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                CategorySelection(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    enabled = !state.isLoading && !state.isSaving && !state.isDeleting,
                    showError = TodoEditValidationError.CATEGORY_REQUIRED in state.validationErrors,
                    onSelected = { onEvent(TodoEditEvent.CategoryChanged(it)) },
                )
                if (state.mode == TodoEditMode.EDIT) {
                    Button(
                        onClick = { onEvent(TodoEditEvent.RequestDelete) },
                        enabled = !state.isLoading &&
                            !state.isSaving &&
                            !state.isDeleting &&
                            state.error != TodoEditError.TODO_NOT_FOUND,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(DELETE_TODO_TAG),
                    ) {
                        Text(
                            text = stringResource(
                                if (state.isDeleting) R.string.deleting_todo
                                else R.string.delete_todo,
                            ),
                        )
                    }
                }
            }
        }
    }

    if (state.showDeleteConfirmation) {
        DeleteTodoConfirmationDialog(
            isDeleting = state.isDeleting,
            onCancel = { onEvent(TodoEditEvent.CancelDelete) },
            onConfirm = { onEvent(TodoEditEvent.ConfirmDelete) },
        )
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
    if (showCustomReminderDialog) {
        CustomReminderDialog(
            editingOffsetMinutes = customReminderDialogEditingOffset,
            onDismiss = {
                showCustomReminderDialog = false
                customReminderDialogEditingOffset = null
            },
            onConfirm = { newOffsetMinutes ->
                val oldOffset = customReminderDialogEditingOffset
                showCustomReminderDialog = false
                customReminderDialogEditingOffset = null
                if (oldOffset == null) {
                    onEvent(TodoEditEvent.AddCustomReminder(newOffsetMinutes))
                } else {
                    onEvent(TodoEditEvent.UpdateCustomReminder(oldOffset, newOffsetMinutes))
                }
            },
        )
    }
}

@Composable
private fun ReminderSelection(
    timeAvailable: Boolean,
    fifteenMinutes: Boolean,
    oneHour: Boolean,
    oneDay: Boolean,
    customOffsets: List<Int>,
    enabled: Boolean,
    onTogglePreset: (Int) -> Unit,
    onRequestAddCustom: () -> Unit,
    onRequestEditCustom: (Int) -> Unit,
    onRemoveCustom: (Int) -> Unit,
) {
    val controlsEnabled = enabled && timeAvailable
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.todo_reminder_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReminderPresetChip(
                label = stringResource(R.string.reminder_fifteen_minutes_before),
                selected = fifteenMinutes,
                enabled = controlsEnabled,
                onClick = { onTogglePreset(Reminder.FIFTEEN_MINUTES_BEFORE) },
                testTag = FIFTEEN_MINUTE_REMINDER_TAG,
            )
            ReminderPresetChip(
                label = stringResource(R.string.reminder_one_hour_before),
                selected = oneHour,
                enabled = controlsEnabled,
                onClick = { onTogglePreset(Reminder.ONE_HOUR_BEFORE) },
                testTag = ONE_HOUR_REMINDER_TAG,
            )
            ReminderPresetChip(
                label = stringResource(R.string.reminder_one_day_before),
                selected = oneDay,
                enabled = controlsEnabled,
                onClick = { onTogglePreset(Reminder.ONE_DAY_BEFORE) },
                testTag = ONE_DAY_REMINDER_TAG,
            )
        }
        if (customOffsets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.reminder_selected_custom_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                customOffsets.forEach { offsetMinutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CUSTOM_REMINDER_ITEM_TAG_PREFIX + offsetMinutes),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "• ${ReminderDurationFormatter.formatRelativeText(offsetMinutes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { onRequestEditCustom(offsetMinutes) },
                            enabled = controlsEnabled,
                            modifier = Modifier.testTag(CUSTOM_REMINDER_EDIT_TAG_PREFIX + offsetMinutes),
                        ) {
                            Text(stringResource(R.string.reminder_custom_edit))
                        }
                        TextButton(
                            onClick = { onRemoveCustom(offsetMinutes) },
                            enabled = controlsEnabled,
                            modifier = Modifier.testTag(CUSTOM_REMINDER_DELETE_TAG_PREFIX + offsetMinutes),
                        ) {
                            Text(stringResource(R.string.reminder_custom_delete))
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onRequestAddCustom,
            enabled = controlsEnabled,
            modifier = Modifier.testTag(ADD_CUSTOM_REMINDER_TAG),
        ) {
            Text(stringResource(R.string.reminder_add_custom))
        }
        if (!timeAvailable) {
            Text(
                stringResource(R.string.reminder_requires_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderPresetChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        modifier = Modifier
            .semantics { toggleableState = ToggleableState(selected) }
            .testTag(testTag),
    )
}

@Composable
private fun CustomReminderDialog(
    editingOffsetMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialParts = remember(editingOffsetMinutes) {
        editingOffsetMinutes?.takeIf { it > 0 }?.let(ReminderDurationFormatter::decompose)
    }
    var daysInput by remember(editingOffsetMinutes) {
        mutableStateOf(initialParts?.days?.toString().orEmpty())
    }
    var hoursInput by remember(editingOffsetMinutes) {
        mutableStateOf(initialParts?.hours?.toString().orEmpty())
    }
    var minutesInput by remember(editingOffsetMinutes) {
        mutableStateOf(initialParts?.minutes?.toString().orEmpty())
    }
    var showValidationError by remember(editingOffsetMinutes) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (editingOffsetMinutes == null) {
                        R.string.reminder_custom_dialog_title_add
                    } else {
                        R.string.reminder_custom_dialog_title_edit
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = {
                            daysInput = it
                            showValidationError = false
                        },
                        label = { Text(stringResource(R.string.reminder_custom_days_label)) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(CUSTOM_REMINDER_DAYS_INPUT_TAG),
                    )
                    OutlinedTextField(
                        value = hoursInput,
                        onValueChange = {
                            hoursInput = it
                            showValidationError = false
                        },
                        label = { Text(stringResource(R.string.reminder_custom_hours_label)) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(CUSTOM_REMINDER_HOURS_INPUT_TAG),
                    )
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = {
                            minutesInput = it
                            showValidationError = false
                        },
                        label = { Text(stringResource(R.string.reminder_custom_minutes_label)) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(CUSTOM_REMINDER_MINUTES_INPUT_TAG),
                    )
                }
                if (showValidationError) {
                    Text(
                        text = stringResource(R.string.reminder_custom_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(CUSTOM_REMINDER_ERROR_TAG),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = ReminderDurationFormatter.parseCustomOffsetMinutes(
                        daysInput = daysInput,
                        hoursInput = hoursInput,
                        minutesInput = minutesInput,
                    )
                    if (parsed == null) {
                        showValidationError = true
                    } else {
                        onConfirm(parsed)
                    }
                },
                modifier = Modifier.testTag(CUSTOM_REMINDER_CONFIRM_TAG),
            ) {
                Text(
                    text = stringResource(
                        if (editingOffsetMinutes == null) {
                            R.string.reminder_custom_confirm_add
                        } else {
                            R.string.reminder_custom_confirm_edit
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(CUSTOM_REMINDER_CANCEL_TAG),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.testTag(CUSTOM_REMINDER_DIALOG_TAG),
    )
}

@Composable
private fun DeleteTodoConfirmationDialog(
    isDeleting: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onCancel() },
        title = { Text(stringResource(R.string.delete_todo_dialog_title)) },
        text = { Text(stringResource(R.string.delete_todo_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                modifier = Modifier.testTag(CONFIRM_DELETE_TAG),
            ) {
                Text(
                    text = stringResource(
                        if (isDeleting) R.string.deleting_todo
                        else R.string.confirm_delete_todo,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isDeleting,
                modifier = Modifier.testTag(CANCEL_DELETE_TAG),
            ) {
                Text(stringResource(R.string.cancel_delete_todo))
            }
        },
        modifier = Modifier.testTag(DELETE_DIALOG_TAG),
    )
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
        TodoEditError.ALARM_NOT_SCHEDULED -> R.string.error_alarm_not_scheduled
        TodoEditError.NOTIFICATION_PERMISSION_DENIED -> R.string.notification_permission_denied_message
        TodoEditError.INEXACT_ALARM_SCHEDULED -> R.string.inexact_alarm_scheduled_message
    }
