package com.example.todoapplication.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.todoapplication.R
import kotlinx.coroutines.delay

@Composable
internal fun ReorderableCategoryList(
    categories: List<CategoryManagementItemUiModel>,
    enabled: Boolean,
    onEvent: (CategoryManagementEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // A gesture preview is disposable; persisted order always comes from the Service Flow.
    var preview by remember(categories, enabled) {
        mutableStateOf<List<CategoryManagementItemUiModel>?>(null)
    }
    var draggedId by remember(categories, enabled) { mutableStateOf<Long?>(null) }
    var dragCenter by remember { mutableFloatStateOf(0f) }
    val latestEvent by rememberUpdatedState(onEvent)
    val displayed = preview ?: categories

    fun moveAtCenter() {
        val id = draggedId ?: return
        val current = preview ?: return
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            dragCenter >= it.offset && dragCenter < it.offset + it.size &&
                current.any { category -> category.id == it.key && !category.isSystem }
        } ?: return
        val from = current.indexOfFirst { it.id == id }
        val to = current.indexOfFirst { it.id == target.key }
        if (from >= 0 && to >= 0 && from != to) {
            preview = current.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    LaunchedEffect(draggedId) {
        while (draggedId != null) {
            val layout = listState.layoutInfo
            val edge = 64f
            val scroll = when {
                dragCenter < layout.viewportStartOffset + edge -> -16f
                dragCenter > layout.viewportEndOffset - edge -> 16f
                else -> 0f
            }
            if (scroll != 0f) {
                listState.scrollBy(scroll)
                moveAtCenter()
            }
            delay(16)
        }
    }

    LazyColumn(state = listState, modifier = modifier.testTag("category-reorder-list")) {
        items(displayed, key = CategoryManagementItemUiModel::id) { category ->
            val users = categories.filterNot { it.isSystem }
            val index = users.indexOfFirst { it.id == category.id }
            val up = stringResource(R.string.category_move_up)
            val down = stringResource(R.string.category_move_down)
            val dragDescription = stringResource(R.string.category_drag, category.name)
            CategoryRow(
                category = category,
                enabled = enabled && draggedId == null,
                onEdit = { latestEvent(CategoryManagementEvent.RequestEdit(category.id)) },
                onDelete = { latestEvent(CategoryManagementEvent.RequestDelete(category.id)) },
                modifier = Modifier
                    .testTag("category-row-" + category.id)
                    .background(
                        if (draggedId == category.id) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ),
                dragHandle = {
                    if (!category.isSystem) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp)
                                .testTag("category-drag-" + category.id)
                                .semantics {
                                    contentDescription = dragDescription
                                    customActions = if (!enabled || draggedId != null) emptyList() else {
                                        buildList {
                                            fun move(to: Int): Boolean {
                                                val ids = users.map { it.id }.toMutableList()
                                                ids.add(to, ids.removeAt(index))
                                                latestEvent(CategoryManagementEvent.ReorderCategories(ids))
                                                return true
                                            }
                                            if (index > 0) add(CustomAccessibilityAction(up) { move(index - 1) })
                                            if (index in 0 until users.lastIndex) {
                                                add(CustomAccessibilityAction(down) { move(index + 1) })
                                            }
                                        }
                                    }
                                }
                                .pointerInput(category.id, categories, enabled) {
                                    if (!enabled) return@pointerInput
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            val item = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == category.id }
                                            if (item != null) {
                                                preview = categories
                                                draggedId = category.id
                                                dragCenter = item.offset + item.size / 2f
                                            }
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragCenter += amount.y
                                            moveAtCenter()
                                        },
                                        onDragCancel = {
                                            draggedId = null
                                            preview = null
                                        },
                                        onDragEnd = {
                                            val ids = preview?.filterNot { it.isSystem }?.map { it.id }
                                            draggedId = null
                                            preview = null
                                            if (ids != null && ids != users.map { it.id }) {
                                                latestEvent(CategoryManagementEvent.ReorderCategories(ids))
                                            }
                                        },
                                    )
                                },
                        ) { Text("≡", style = MaterialTheme.typography.headlineSmall) }
                    }
                },
            )
            HorizontalDivider()
        }
    }
}
