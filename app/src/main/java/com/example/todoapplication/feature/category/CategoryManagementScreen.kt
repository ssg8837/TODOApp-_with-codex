package com.example.todoapplication.feature.category

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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

const val CATEGORY_MANAGEMENT_BACK_TAG = "category-management-back"
const val ADD_CATEGORY_TAG = "add-category"
const val CATEGORY_LOADING_TAG = "category-loading"
const val CATEGORY_ERROR_TAG = "category-error"
const val CATEGORY_COLOR_TAG_PREFIX = "category-management-color-"
const val EDIT_CATEGORY_TAG_PREFIX = "edit-category-"
const val DELETE_CATEGORY_TAG_PREFIX = "delete-category-"
const val CATEGORY_EDITOR_DIALOG_TAG = "category-editor-dialog"
const val CATEGORY_NAME_INPUT_TAG = "category-name-input"
const val SAVE_CATEGORY_TAG = "save-category"
const val DELETE_CATEGORY_DIALOG_TAG = "delete-category-dialog"
const val CONFIRM_DELETE_CATEGORY_TAG = "confirm-delete-category"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    state: CategoryManagementUiState,
    onEvent: (CategoryManagementEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_management_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(CATEGORY_MANAGEMENT_BACK_TAG),
                    ) { Text(stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading || state.isReordering) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().testTag(CATEGORY_LOADING_TAG),
                )
            }
            if (state.editorMode == CategoryEditorMode.NONE) {
                state.error?.let { CategoryErrorMessage(it) }
            }
            ReorderableCategoryList(
                categories = state.categories,
                enabled = !state.isLoading && !state.isSaving && !state.isDeleting &&
                    !state.isReordering && state.editorMode == CategoryEditorMode.NONE &&
                    !state.showDeleteConfirmation,
                onEvent = onEvent,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Button(
                onClick = { onEvent(CategoryManagementEvent.RequestCreate) },
                enabled = !state.isSaving && !state.isDeleting && !state.isReordering,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag(ADD_CATEGORY_TAG),
            ) { Text(stringResource(R.string.add_category)) }
        }
    }

    if (state.editorMode != CategoryEditorMode.NONE) {
        CategoryEditorDialog(state, onEvent)
    }
    if (state.showDeleteConfirmation) {
        DeleteCategoryDialog(state, onEvent)
    }
}

@Composable
internal fun CategoryRow(
    category: CategoryManagementItemUiModel,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit = {},
) {
    val colorDescription = stringResource(R.string.category_color_indicator, category.name)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(category.color.toComposeColor())
                .testTag(CATEGORY_COLOR_TAG_PREFIX + category.id)
                .semantics { contentDescription = colorDescription },
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(category.name, style = MaterialTheme.typography.titleMedium)
            if (category.isSystem) {
                Text(
                    stringResource(R.string.system_category_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!category.isSystem) {
            dragHandle()
            TextButton(
                onClick = onEdit,
                enabled = enabled,
                modifier = Modifier.testTag(EDIT_CATEGORY_TAG_PREFIX + category.id),
            ) { Text(stringResource(R.string.edit_category)) }
            TextButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.testTag(DELETE_CATEGORY_TAG_PREFIX + category.id),
            ) { Text(stringResource(R.string.delete_category)) }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    state: CategoryManagementUiState,
    onEvent: (CategoryManagementEvent) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(CATEGORY_EDITOR_DIALOG_TAG),
        onDismissRequest = { onEvent(CategoryManagementEvent.CancelEdit) },
        title = {
            Text(
                stringResource(
                    if (state.editorMode == CategoryEditorMode.CREATE) {
                        R.string.create_category_title
                    } else {
                        R.string.edit_category_title
                    },
                ),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = state.categoryNameInput,
                    onValueChange = { onEvent(CategoryManagementEvent.CategoryNameChanged(it)) },
                    enabled = !state.isSaving,
                    singleLine = true,
                    label = { Text(stringResource(R.string.category_name_label)) },
                    supportingText = state.validationError?.let { error ->
                        { Text(stringResource(error.stringResource)) }
                    },
                    isError = state.validationError != null,
                    modifier = Modifier.fillMaxWidth().testTag(CATEGORY_NAME_INPUT_TAG),
                )
                CategoryColorPicker(
                    selected = state.selectedCategoryColor,
                    enabled = !state.isSaving,
                    onSelected = { onEvent(CategoryManagementEvent.CategoryColorChanged(it)) },
                )
                state.error?.let { CategoryErrorMessage(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(CategoryManagementEvent.SaveCategory) },
                enabled = !state.isSaving,
                modifier = Modifier.testTag(SAVE_CATEGORY_TAG),
            ) {
                Text(stringResource(if (state.isSaving) R.string.saving else R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(CategoryManagementEvent.CancelEdit) },
                enabled = !state.isSaving,
            ) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun DeleteCategoryDialog(
    state: CategoryManagementUiState,
    onEvent: (CategoryManagementEvent) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(DELETE_CATEGORY_DIALOG_TAG),
        onDismissRequest = { onEvent(CategoryManagementEvent.CancelDelete) },
        title = { Text(stringResource(R.string.delete_category_dialog_title)) },
        text = { Text(stringResource(R.string.delete_category_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = { onEvent(CategoryManagementEvent.ConfirmDelete) },
                enabled = !state.isDeleting,
                modifier = Modifier.testTag(CONFIRM_DELETE_CATEGORY_TAG),
            ) {
                Text(stringResource(if (state.isDeleting) R.string.deleting_category else R.string.delete_category))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(CategoryManagementEvent.CancelDelete) },
                enabled = !state.isDeleting,
            ) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun CategoryErrorMessage(error: CategoryManagementError) {
    Text(
        text = stringResource(error.stringResource),
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp)
            .testTag(CATEGORY_ERROR_TAG),
    )
}

private val CategoryNameValidationError.stringResource: Int
    @StringRes get() = when (this) {
        CategoryNameValidationError.REQUIRED -> R.string.category_name_required
        CategoryNameValidationError.RESERVED -> R.string.category_name_reserved
        CategoryNameValidationError.DUPLICATED -> R.string.category_name_duplicated
        CategoryNameValidationError.INVALID -> R.string.category_name_invalid
    }

private val CategoryManagementError.stringResource: Int
    @StringRes get() = when (this) {
        CategoryManagementError.INVALID_ORDER -> R.string.error_category_order
        CategoryManagementError.CATEGORY_NOT_FOUND -> R.string.error_manage_category_not_found
        CategoryManagementError.SYSTEM_OPERATION_PROHIBITED -> R.string.error_system_category_operation
        CategoryManagementError.PERSISTENCE_FAILURE -> R.string.error_category_persistence_failure
        CategoryManagementError.OPERATION_FAILED -> R.string.error_operation_failed
    }
