package com.example.todoapplication.feature.todo.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.todoapplication.R
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.ui.theme.toComposeColor
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

const val DETAIL_TITLE_TAG = "detail-title"
const val DETAIL_DATE_TAG = "detail-date"
const val DETAIL_TIME_TAG = "detail-time"
const val DETAIL_CATEGORY_TAG = "detail-category"
const val DETAIL_STATUS_TAG = "detail-status"
const val DETAIL_REMINDERS_TAG = "detail-reminders"
const val DETAIL_EDIT_BUTTON_TAG = "detail-edit-button"
const val DETAIL_CONFIRM_BUTTON_TAG = "detail-confirm-button"
const val DETAIL_LOADING_TAG = "detail-loading"
const val DETAIL_ERROR_TAG = "detail-error"

/**
 * TODO 항목의 제목, 날짜, 시간, Category, 상태, Reminder 정보를 표시하는 읽기 전용 화면.
 *
 * 직접적인 데이터 편집을 허용하지 않으며 [onEdit]을 통해 수정 화면으로 이동하거나
 * [onConfirm]으로 화면을 닫는다.
 */
@Composable
fun TodoDetailScreen(
    state: TodoDetailUiState,
    onEvent: (TodoDetailEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            DetailBottomBar(
                canEdit = !state.isLoading && state.error == null && (state.todoId ?: 0L) > 0L,
                onEdit = onEdit,
                onConfirm = onConfirm,
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            DetailHeader(onBack = onBack)

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DETAIL_LOADING_TAG),
                )
            }

            if (state.error != null) {
                val errorMessage = when (state.error) {
                    TodoDetailError.TODO_NOT_FOUND -> stringResource(R.string.error_todo_not_found)
                    TodoDetailError.PERSISTENCE_FAILURE -> stringResource(R.string.error_persistence_failure)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(16.dp)
                        .testTag(DETAIL_ERROR_TAG),
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else if (!state.isLoading) {
                DetailContent(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.back))
        }
        Text(
            text = stringResource(R.string.todo_detail_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun DetailContent(
    state: TodoDetailUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 제목
        DetailField(
            label = stringResource(R.string.todo_title_label),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(DETAIL_TITLE_TAG),
            )
        }

        HorizontalDivider()

        // 날짜
        DetailField(
            label = stringResource(R.string.todo_date_label),
        ) {
            val formattedDate = state.date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(DETAIL_DATE_TAG),
            )
        }

        // 시간
        DetailField(
            label = stringResource(R.string.todo_time_label),
        ) {
            val formattedTime = state.time?.format(DateTimeFormatter.ofPattern("HH:mm"))
                ?: stringResource(R.string.todo_no_time)
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(DETAIL_TIME_TAG),
            )
        }

        HorizontalDivider()

        // Category
        DetailField(
            label = stringResource(R.string.todo_category_label),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val colorDescription = stringResource(
                    R.string.category_color_indicator,
                    state.categoryName,
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(state.categoryColor.toComposeColor())
                        .semantics { contentDescription = colorDescription },
                )
                Text(
                    text = state.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(DETAIL_CATEGORY_TAG),
                )
            }
        }

        // 완료/미완료 상태
        DetailField(
            label = stringResource(R.string.todo_status_label),
        ) {
            val statusText = if (state.isCompleted) {
                stringResource(R.string.status_completed)
            } else {
                stringResource(R.string.status_incomplete)
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(DETAIL_STATUS_TAG),
            )
        }

        HorizontalDivider()

        // Reminder
        DetailField(
            label = stringResource(R.string.todo_reminder_label),
        ) {
            Column(
                modifier = Modifier.testTag(DETAIL_REMINDERS_TAG),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.reminders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.todo_no_reminder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.reminders.forEach { minutes ->
                        val reminderText = when (minutes) {
                            Reminder.ONE_DAY_BEFORE -> stringResource(R.string.reminder_one_day_before)
                            Reminder.FIFTEEN_MINUTES_BEFORE -> stringResource(R.string.reminder_fifteen_minutes_before)
                            else -> "$minutes"
                        }
                        Text(
                            text = "- $reminderText",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun DetailBottomBar(
    canEdit: Boolean,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onEdit,
            enabled = canEdit,
            modifier = Modifier
                .weight(1f)
                .testTag(DETAIL_EDIT_BUTTON_TAG),
        ) {
            Text(text = stringResource(R.string.todo_edit))
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .weight(1f)
                .testTag(DETAIL_CONFIRM_BUTTON_TAG),
        ) {
            Text(text = stringResource(R.string.todo_confirm))
        }
    }
}

